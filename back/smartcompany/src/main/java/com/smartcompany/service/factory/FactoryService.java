package com.smartcompany.service.factory;

import com.smartcompany.dto.cache.CachedPage;
import com.smartcompany.dto.factory.FactoryDto;
import com.smartcompany.dto.factory.FactoryRequestDto;
import com.smartcompany.dto.location.CityDto;
import com.smartcompany.dto.location.CountryDto;
import com.smartcompany.exception.AppException;
import com.smartcompany.model.factory.Factory;
import com.smartcompany.model.factory.FactoryImage;
import com.smartcompany.model.location.City;
import com.smartcompany.model.location.Country;
import com.smartcompany.model.product.Product;
import com.smartcompany.repository.factory.FactoryRepository;
import com.smartcompany.repository.factory.ProductionRecordRepository;
import com.smartcompany.repository.location.CityRepository;
import com.smartcompany.repository.location.CountryRepository;
import com.smartcompany.repository.product.ProductRepository;
import com.smartcompany.service.influx.InfluxDBService;
import com.smartcompany.specification.FactorySpecification;
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
public class FactoryService {

    @Autowired private FactoryRepository factoryRepository;
    @Autowired private CountryRepository countryRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InfluxDBService influxDBService;
    @Autowired private CacheManager cacheManager;
    @Autowired private ProductionRecordRepository productionRecordRepository;

    // ─── CRUD ────────────────────────────────────────────────────────────────
    public Page<FactoryDto> search(String search, Long countryId, Long cityId,
                               Boolean isOnline, Pageable pageable) {
        Cache cache = cacheManager.getCache("factories");
        String key = search + "-" + countryId + "-" + cityId + "-" + isOnline
                + "-" + pageable.getPageNumber() + "-" + pageable.getPageSize() + "-" + pageable.getSort();

        if (cache != null) {
            CachedPage<FactoryDto> cached = cache.get(key, CachedPage.class);
            if (cached != null) {
                return cached.toPage(pageable);
            }
        }

        Specification<Factory> spec = FactorySpecification.search(search, countryId, cityId, isOnline);
        Page<FactoryDto> result = factoryRepository.findAll(spec, pageable).map(this::toDto);

        if (cache != null) {
            cache.put(key, CachedPage.from(result));
        }

        return result;
    }

    public FactoryDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "factories", allEntries = true)
    public FactoryDto create(FactoryRequestDto dto) {
        Country country = countryRepository.findById(dto.countryId())
                .orElseThrow(() -> new AppException("Country not found", HttpStatus.NOT_FOUND));
        City city = cityRepository.findById(dto.cityId())
                .orElseThrow(() -> new AppException("City not found", HttpStatus.NOT_FOUND));

        Factory factory = new Factory();
        applyDto(factory, dto, country, city);
        return toDto(factoryRepository.save(factory));
    }

    @Transactional
    @CacheEvict(value = "factories", allEntries = true)
    public FactoryDto update(Long id, FactoryRequestDto dto) {
        Factory factory = findOrThrow(id);
        Country country = countryRepository.findById(dto.countryId())
                .orElseThrow(() -> new AppException("Country not found", HttpStatus.NOT_FOUND));
        City city = cityRepository.findById(dto.cityId())
                .orElseThrow(() -> new AppException("City not found", HttpStatus.NOT_FOUND));

        factory.getImages().clear();
        applyDto(factory, dto, country, city);
        return toDto(factoryRepository.save(factory));
    }

    @Transactional
    @CacheEvict(value = "factories", allEntries = true)
    public void delete(Long id) {
        if (!factoryRepository.existsById(id)) {
            throw new AppException("Factory not found", HttpStatus.NOT_FOUND);
        }

        if (productionRecordRepository.existsByFactoryId(id)) {
            throw new AppException("Cannot delete factory with existing production records", HttpStatus.CONFLICT);
        }

        factoryRepository.deleteById(id);
    }

    // ─── Lookup ──────────────────────────────────────────────────────────────

    @Cacheable("countries")
    public List<CountryDto> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(c -> new CountryDto(c.getId(), c.getName(), c.getCode()))
                .collect(java.util.stream.Collectors.toList()); 
    }


    @Cacheable(value = "cities", key = "#countryId")
    public List<CityDto> getCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryId(countryId).stream()
                .map(c -> new CityDto(c.getId(), c.getName(), c.getCountry().getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── Heartbeat checker ───────────────────────────────────────────────────

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkHeartbeats() {
        Instant cutoff = Instant.now().minusSeconds(60);
        List<Factory> stale = factoryRepository.findStaleOnlineFactoriesForUpdate(cutoff);
        for (Factory f : stale) {
            f.setOnline(false);
            influxDBService.writeOfflineEvent(f.getId());
        }
        if (!stale.isEmpty()) {
            factoryRepository.saveAll(stale);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Factory findOrThrow(Long id) {
        return factoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Factory not found", HttpStatus.NOT_FOUND));
    }

    private void applyDto(Factory factory, FactoryRequestDto dto, Country country, City city) {
        factory.setName(dto.name());
        factory.setAddress(dto.address());
        factory.setCountry(country);
        factory.setCity(city);
        factory.setLatitude(dto.latitude());
        factory.setLongitude(dto.longitude());

        if (dto.imageKeys() != null) {
            for (String key : dto.imageKeys()) {
                FactoryImage img = new FactoryImage();
                img.setImageKey(key);
                img.setFactory(factory);
                factory.getImages().add(img);
            }
        }

        factory.getProducts().clear();
        if (dto.productIds() != null && !dto.productIds().isEmpty()) {
            List<Product> products = productRepository.findAllById(dto.productIds());
            factory.setProducts(products);
        }
    }

    public FactoryDto toDto(Factory f) {
        List<String> imageKeys = f.getImages().stream().map(FactoryImage::getImageKey).toList();
        List<Long> productIds = f.getProducts().stream().map(Product::getId).toList();
        return new FactoryDto(
                f.getId(), f.getName(), f.getAddress(),
                new CountryDto(f.getCountry().getId(), f.getCountry().getName(), f.getCountry().getCode()),
                new CityDto(f.getCity().getId(), f.getCity().getName(), f.getCity().getCountry().getId()),
                f.getLatitude(), f.getLongitude(),
                f.isOnline(), f.getLastHeartbeat() != null ? f.getLastHeartbeat().toString() : null,
                imageKeys, productIds
        );
    }
}