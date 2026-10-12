package com.smartcompany.service.warehouse;

import com.smartcompany.dto.cache.CachedPage;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;
import com.smartcompany.dto.warehouse.TemperatureMessage;
import com.smartcompany.dto.warehouse.WarehouseDto;
import com.smartcompany.dto.warehouse.WarehouseRequestDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.location.City;
import com.smartcompany.model.location.Country;
import com.smartcompany.model.warehouse.*;
import com.smartcompany.repository.location.CityRepository;
import com.smartcompany.repository.location.CountryRepository;
import com.smartcompany.repository.warehouse.WarehouseRepository;
import com.smartcompany.repository.warehouse.WarehouseSectorRepository;
import com.smartcompany.service.influx.InfluxDBService;
import com.smartcompany.specification.WarehouseSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class WarehouseService {

    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private WarehouseSectorRepository sectorRepository;
    @Autowired private CountryRepository countryRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private InfluxDBService influxDBService;
    @Autowired private CacheManager cacheManager;

    // ─── CRUD ────────────────────────────────────────────────────────────────
    public Page<WarehouseDto> search(String search, Long countryId, Long cityId,
                                 Boolean isOnline, Pageable pageable) {
        Cache cache = cacheManager.getCache("warehouses");
        String key = search + "-" + countryId + "-" + cityId + "-" + isOnline
                + "-" + pageable.getPageNumber() + "-" + pageable.getPageSize() + "-" + pageable.getSort();

        if (cache != null) {
            CachedPage<WarehouseDto> cached = cache.get(key, CachedPage.class);
            if (cached != null) {
                return cached.toPage(pageable);
            }
        }

        Specification<Warehouse> spec = WarehouseSpecification.search(search, countryId, cityId, isOnline);
        Page<WarehouseDto> result = warehouseRepository.findAll(spec, pageable).map(this::toDto);

        if (cache != null) {
            cache.put(key, CachedPage.from(result));
        }

        return result;
    }

    public WarehouseDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public WarehouseDto create(WarehouseRequestDto dto) {
        Country country = countryRepository.findById(dto.countryId())
            .orElseThrow(() -> new AppException("Country not found", HttpStatus.NOT_FOUND));
        City city = cityRepository.findById(dto.cityId())
            .orElseThrow(() -> new AppException("City not found", HttpStatus.NOT_FOUND));

        Warehouse warehouse = new Warehouse();
        applyDto(warehouse, dto, country, city);
        return toDto(warehouseRepository.save(warehouse));
    }

    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public WarehouseDto update(Long id, WarehouseRequestDto dto) {
        Warehouse warehouse;
        try {
            warehouse = warehouseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            throw new AppException(
                "Magacin se trenutno menja od strane drugog menadžera, pokušajte ponovo",
                HttpStatus.CONFLICT
            );
        }

        Country country = countryRepository.findById(dto.countryId())
            .orElseThrow(() -> new AppException("Country not found", HttpStatus.NOT_FOUND));
        City city = cityRepository.findById(dto.cityId())
            .orElseThrow(() -> new AppException("City not found", HttpStatus.NOT_FOUND));

        warehouse.getImages().clear();
        warehouse.getSectors().clear();
        applyDto(warehouse, dto, country, city);
        return toDto(warehouseRepository.save(warehouse));
}

    @Transactional
    @CacheEvict(value = "warehouses", allEntries = true)
    public void delete(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new AppException("Warehouse not found", HttpStatus.NOT_FOUND);
        }
        warehouseRepository.deleteById(id);
    }

    // ─── Heartbeat ───────────────────────────────────────────────────────────

    @Transactional
    public void handleHeartbeat(Long warehouseId) {
        // Zaključava red magacina (SELECT ... FOR UPDATE) da se ne bi
        // konkurentni heartbeat i checkHeartbeats() sudarili nad isOnline/lastHeartbeat
        warehouseRepository.findByIdForUpdate(warehouseId).ifPresent(w -> {
            boolean wasOffline = !w.isOnline();
            w.setOnline(true);
            w.setLastHeartbeat(Instant.now());
            warehouseRepository.save(w);
            if (wasOffline) {
                influxDBService.writeWarehouseOnlineEvent(warehouseId);
            }
            influxDBService.writeWarehouseHeartbeat(warehouseId);
        });
    }

    @Transactional
    public void handleTemperature(TemperatureMessage msg) {
        sectorRepository.findByIdForUpdate(msg.sectorId()).ifPresent(sector -> {
            sector.setLastTemperature(msg.temperature());
            sector.setLastMeasuredAt(msg.timestamp());
            sectorRepository.save(sector);
            influxDBService.writeTemperature(
                msg.warehouseId(), msg.sectorId(), msg.temperature(), msg.timestamp()
            );
        });
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkHeartbeats() {
        Instant cutoff = Instant.now().minusSeconds(60);
        // Zaključana varijanta postojećeg upita — sprečava da se ovaj scheduled
        // posao i konkurentan handleHeartbeat() za isti magacin preseku
        List<Warehouse> stale = warehouseRepository.findStaleOnlineWarehousesForUpdate(cutoff);
        for (Warehouse w : stale) {
            w.setOnline(false);
            influxDBService.writeWarehouseOfflineEvent(w.getId());
        }
        if (!stale.isEmpty()) {
            warehouseRepository.saveAll(stale);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Warehouse findOrThrow(Long id) {
        return warehouseRepository.findById(id)
            .orElseThrow(() -> new AppException("Warehouse not found", HttpStatus.NOT_FOUND));
    }

    private void applyDto(Warehouse warehouse, WarehouseRequestDto dto,
                           Country country, City city) {
        warehouse.setName(dto.name());
        warehouse.setAddress(dto.address());
        warehouse.setCountry(country);
        warehouse.setCity(city);
        warehouse.setLatitude(dto.latitude());
        warehouse.setLongitude(dto.longitude());

        if (dto.imageKeys() != null) {
            for (String key : dto.imageKeys()) {
                WarehouseImage img = new WarehouseImage();
                img.setImageKey(key);
                img.setWarehouse(warehouse);
                warehouse.getImages().add(img);
            }
        }

        if (dto.sectors() != null) {
            for (WarehouseRequestDto.SectorRequestDto s : dto.sectors()) {
                WarehouseSector sector = new WarehouseSector();
                sector.setName(s.name());
                sector.setWarehouse(warehouse);
                warehouse.getSectors().add(sector);
            }
        }
    }

    public WarehouseDto toDto(Warehouse w) {
        List<String> imageKeys = w.getImages().stream()
            .map(WarehouseImage::getImageKey).toList();
        List<WarehouseDto.SectorDto> sectors = w.getSectors().stream()
            .map(s -> new WarehouseDto.SectorDto(
                s.getId(), s.getName(), s.getLastTemperature(),
                s.getLastMeasuredAt() != null ? s.getLastMeasuredAt().toString() : null
            )).toList();
        return new WarehouseDto(
            w.getId(), w.getName(), w.getAddress(),
            new CountryDto(w.getCountry().getId(), w.getCountry().getName(), w.getCountry().getCode()),
            new CityDto(w.getCity().getId(), w.getCity().getName(), w.getCity().getCountry().getId()),
            w.getLatitude(), w.getLongitude(),
            w.isOnline(),
            w.getLastHeartbeat() != null ? w.getLastHeartbeat().toString() : null,
            imageKeys, sectors
        );
    }
}