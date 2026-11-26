package dev.winestore.wine;

import dev.winestore.common.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class WineService {

    private final WineRepository wines;
    private final WineCacheInvalidator cacheInvalidator;

    public WineService(WineRepository wines, WineCacheInvalidator cacheInvalidator) {
        this.wines = wines;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Cacheable(cacheNames = WineCaches.SEARCH,
            key = "T(dev.winestore.wine.WineSearchKey).of(#type, #country, #minPrice, #maxPrice, #pageable)")
    @Transactional(readOnly = true)
    public Page<WineResponse> search(WineType type, String country, BigDecimal minPrice, BigDecimal maxPrice,
                                     Pageable pageable) {
        return wines.findAll(WineSpecs.filter(type, country, minPrice, maxPrice), pageable)
                .map(WineResponse::from);
    }

    @Cacheable(cacheNames = WineCaches.BY_ID, key = "#id")
    @Transactional(readOnly = true)
    public WineResponse get(long id) {
        return WineResponse.from(find(id));
    }

    @Transactional
    public WineResponse create(WineRequest req) {
        WineResponse created = WineResponse.from(wines.save(req.toEntity()));
        cacheInvalidator.evictAfterCommit(List.of());
        return created;
    }

    @Transactional
    public WineResponse update(long id, WineRequest req) {
        Wine wine = find(id);
        wine.setName(req.name().trim());
        wine.setType(Objects.requireNonNull(req.type()));
        wine.setCountry(req.country().trim());
        wine.setRegion(req.region());
        wine.setVintage(req.vintage());
        wine.setPrice(Objects.requireNonNull(req.price()));
        wine.setStock(Objects.requireNonNull(req.stock()));
        wine.setDescription(req.description());
        cacheInvalidator.evictAfterCommit(List.of(id));
        return WineResponse.from(wine);
    }

    @Transactional
    public void delete(long id) {
        if (!wines.existsById(id)) {
            throw new NotFoundException("Wine " + id + " not found");
        }
        wines.deleteById(id);
        cacheInvalidator.evictAfterCommit(List.of(id));
    }

    private Wine find(long id) {
        return wines.findById(id).orElseThrow(() -> new NotFoundException("Wine " + id + " not found"));
    }
}
