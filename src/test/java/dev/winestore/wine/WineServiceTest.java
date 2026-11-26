package dev.winestore.wine;

import dev.winestore.common.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static dev.winestore.TestEntities.wine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WineServiceTest {

    @Mock
    private WineRepository wines;
    @Mock
    private WineCacheInvalidator cacheInvalidator;

    private WineService service;

    @BeforeEach
    void setUp() {
        service = new WineService(wines, cacheInvalidator);
    }

    private static WineRequest request(String name, WineType type, String country, String price, int stock) {
        return new WineRequest(name, type, country, null, null, new BigDecimal(price), stock, null);
    }

    @Test
    @DisplayName("search - returns paged results")
    void searchReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Wine> page = new PageImpl<>(List.of(wine(1L, 5)), pageable, 1);
        when(wines.findAll(ArgumentMatchers.<Specification<Wine>>any(), eq(pageable))).thenReturn(page);

        Page<WineResponse> result = service.search(null, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Test Wine");
    }

    @Test
    @DisplayName("get - returns wine by id")
    void getById() {
        when(wines.findById(1L)).thenReturn(Optional.of(wine(1L, 5)));

        WineResponse result = service.get(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Test Wine");
    }

    @Test
    @DisplayName("get - wine not found - throws NotFoundException")
    void getNotFound() {
        when(wines.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create - saves, returns wine and evicts search cache")
    void createSaves() {
        when(wines.save(any(Wine.class))).thenReturn(wine(1L, 5));

        WineResponse result = service.create(request("Test Wine", WineType.RED, "France", "10.00", 5));

        assertThat(result.id()).isEqualTo(1L);
        verify(cacheInvalidator).evictAfterCommit(List.of());
    }

    @Test
    @DisplayName("create - trims name and country")
    void createTrims() {
        when(wines.save(any(Wine.class))).thenAnswer(inv -> {
            Wine saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        WineResponse result = service.create(request("  Test Wine  ", WineType.RED, "  France  ", "10.00", 5));

        assertThat(result.name()).isEqualTo("Test Wine");
        assertThat(result.country()).isEqualTo("France");
    }

    @Test
    @DisplayName("update - updates existing wine and evicts its cache entries")
    void updateUpdates() {
        Wine wine = wine(1L, 5);
        when(wines.findById(1L)).thenReturn(Optional.of(wine));

        service.update(1L, request("Updated Wine", WineType.WHITE, "Italy", "15.00", 10));

        assertThat(wine.getName()).isEqualTo("Updated Wine");
        assertThat(wine.getType()).isEqualTo(WineType.WHITE);
        assertThat(wine.getCountry()).isEqualTo("Italy");
        assertThat(wine.getStock()).isEqualTo(10);
        verify(cacheInvalidator).evictAfterCommit(List.of(1L));
    }

    @Test
    @DisplayName("update - trims name and country")
    void updateTrims() {
        Wine wine = wine(1L, 5);
        when(wines.findById(1L)).thenReturn(Optional.of(wine));

        service.update(1L, request("  Updated Wine  ", WineType.WHITE, "  Italy  ", "15.00", 10));

        assertThat(wine.getName()).isEqualTo("Updated Wine");
        assertThat(wine.getCountry()).isEqualTo("Italy");
    }

    @Test
    @DisplayName("update - wine not found - throws NotFoundException, cache untouched")
    void updateNotFound() {
        when(wines.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, request("Updated Wine", WineType.WHITE, "Italy", "15.00", 10)))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(cacheInvalidator);
    }

    @Test
    @DisplayName("delete - deletes existing wine and evicts its cache entries")
    void deleteDeletes() {
        when(wines.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(wines).deleteById(1L);
        verify(cacheInvalidator).evictAfterCommit(List.of(1L));
    }

    @Test
    @DisplayName("delete - wine not found - throws NotFoundException")
    void deleteNotFound() {
        when(wines.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);

        verify(wines, never()).deleteById(any());
        verifyNoInteractions(cacheInvalidator);
    }
}
