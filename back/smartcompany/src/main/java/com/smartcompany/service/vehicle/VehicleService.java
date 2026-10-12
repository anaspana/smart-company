package com.smartcompany.service.vehicle;

import com.smartcompany.dto.cache.CachedPage;
import com.smartcompany.dto.vehicle.*;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.vehicle.*;
import com.smartcompany.repository.vehicle.*;
import com.smartcompany.service.influx.InfluxDBService;
import com.smartcompany.specification.VehicleSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class VehicleService {

    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private VehicleBrandRepository brandRepository;
    @Autowired private VehicleModelRepository modelRepository;
    @Autowired private InfluxDBService influxDBService;
    @Autowired private CacheManager cacheManager;

    // ─── CRUD ────────────────────────────────────────────────────────────────
    public Page<VehicleDto> search(String search, Long brandId, Long modelId,
                                Boolean isOnline, Pageable pageable) {
        Cache cache = cacheManager.getCache("vehicles");
        String key = search + "-" + brandId + "-" + modelId + "-" + isOnline
                + "-" + pageable.getPageNumber() + "-" + pageable.getPageSize() + "-" + pageable.getSort();

        if (cache != null) {
            CachedPage<VehicleDto> cached = cache.get(key, CachedPage.class);
            if (cached != null) {
                return cached.toPage(pageable);
            }
        }

        Specification<Vehicle> spec = VehicleSpecification.search(search, brandId, modelId, isOnline);
        Page<VehicleDto> result = vehicleRepository.findAll(spec, pageable).map(this::toDto);

        if (cache != null) {
            cache.put(key, CachedPage.from(result));
        }

        return result;
    }

    public VehicleDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleDto create(VehicleRequestDto dto) {
        VehicleBrand brand = brandRepository.findById(dto.brandId())
            .orElseThrow(() -> new AppException("Brand not found", HttpStatus.NOT_FOUND));
        VehicleModel model = modelRepository.findById(dto.modelId())
            .orElseThrow(() -> new AppException("Model not found", HttpStatus.NOT_FOUND));

        if (!model.getBrand().getId().equals(brand.getId())) {
            throw new AppException("Model does not belong to selected brand", HttpStatus.BAD_REQUEST);
        }

        Vehicle vehicle = new Vehicle();
        applyDto(vehicle, dto, brand, model);
        return toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleDto update(Long id, VehicleRequestDto dto) {
        Vehicle vehicle = findOrThrow(id);
        VehicleBrand brand = brandRepository.findById(dto.brandId())
            .orElseThrow(() -> new AppException("Brand not found", HttpStatus.NOT_FOUND));
        VehicleModel model = modelRepository.findById(dto.modelId())
            .orElseThrow(() -> new AppException("Model not found", HttpStatus.NOT_FOUND));

        if (!model.getBrand().getId().equals(brand.getId())) {
            throw new AppException("Model does not belong to selected brand", HttpStatus.BAD_REQUEST);
        }

        vehicle.getImages().clear();
        applyDto(vehicle, dto, brand, model);
        return toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public void delete(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new AppException("Vehicle not found", HttpStatus.NOT_FOUND);
        }
        vehicleRepository.deleteById(id);
    }

    // ─── Lookup ──────────────────────────────────────────────────────────────

    @Cacheable("vehicle-brands")
    public List<VehicleBrandDto> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(b -> new VehicleBrandDto(b.getId(), b.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Cacheable(value = "vehicle-models", key = "#brandId")
    public List<VehicleModelDto> getModelsByBrand(Long brandId) {
        return modelRepository.findByBrandId(brandId).stream()
                .map(m -> new VehicleModelDto(m.getId(), m.getName(), m.getBrand().getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── Heartbeat & location ──────────────────────────────────────────────

    @Transactional
    public void handleHeartbeat(Long vehicleId) {
        vehicleRepository.findByIdForUpdate(vehicleId).ifPresent(v -> {
            boolean wasOffline = !v.isOnline();
            v.setOnline(true);
            v.setLastHeartbeat(Instant.now());
            vehicleRepository.save(v);
            if (wasOffline) {
                influxDBService.writeVehicleOnlineEvent(vehicleId);
            }
            influxDBService.writeVehicleHeartbeat(vehicleId);
        });
    }

    @Transactional
    public void handleLocation(VehicleLocationMessage msg) {
        vehicleRepository.findByIdForUpdate(msg.vehicleId()).ifPresent(v -> {
            v.setLatitude(msg.latitude());
            v.setLongitude(msg.longitude());
            vehicleRepository.save(v);

            influxDBService.writeVehicleDistance(
                msg.vehicleId(), msg.distanceTraveled(), msg.timestamp()
            );
        });
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkHeartbeats() {
        Instant cutoff = Instant.now().minusSeconds(60);
        List<Vehicle> stale = vehicleRepository.findStaleOnlineVehiclesForUpdate(cutoff);
        for (Vehicle v : stale) {
            v.setOnline(false);
            influxDBService.writeVehicleOfflineEvent(v.getId());
        }
        if (!stale.isEmpty()) {
            vehicleRepository.saveAll(stale);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id)
            .orElseThrow(() -> new AppException("Vehicle not found", HttpStatus.NOT_FOUND));
    }

    private void applyDto(Vehicle vehicle, VehicleRequestDto dto, VehicleBrand brand, VehicleModel model) {
        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setWeightLimit(dto.weightLimit());
        vehicle.setBrand(brand);
        vehicle.setModel(model);

        if (dto.imageKeys() != null) {
            for (String key : dto.imageKeys()) {
                VehicleImage img = new VehicleImage();
                img.setImageKey(key);
                img.setVehicle(vehicle);
                vehicle.getImages().add(img);
            }
        }
    }

    public VehicleDto toDto(Vehicle v) {
        List<String> imageKeys = v.getImages().stream()
            .map(VehicleImage::getImageKey).toList();
        return new VehicleDto(
            v.getId(), v.getLicensePlate(), v.getWeightLimit(),
            new VehicleBrandDto(v.getBrand().getId(), v.getBrand().getName()),
            new VehicleModelDto(v.getModel().getId(), v.getModel().getName(), v.getModel().getBrand().getId()),
            v.getLatitude(), v.getLongitude(),
            v.isOnline(),
            v.getLastHeartbeat() != null ? v.getLastHeartbeat().toString() : null,
            imageKeys
        );
    }
}