package dev.winestore.wine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wines")
public class WineController {

    private final WineService service;

    public WineController(WineService service) {
        this.service = service;
    }

    @GetMapping
    public Page<WineResponse> search(
            @RequestParam(required = false) WineType type,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.search(type, country, minPrice, maxPrice, pageable);
    }

    @GetMapping("/{id}")
    public WineResponse get(@PathVariable long id) {
        return service.get(id);
    }
}
