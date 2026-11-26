package dev.winestore.wine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WineCacheInvalidatorTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache byId;
    @Mock
    private Cache search;

    private WineCacheInvalidator invalidator;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache(WineCaches.BY_ID)).thenReturn(byId);
        lenient().when(cacheManager.getCache(WineCaches.SEARCH)).thenReturn(search);
        invalidator = new WineCacheInvalidator(cacheManager);
    }

    @Test
    @DisplayName("outside a transaction - evicts immediately: given wines and all search pages")
    void evictsImmediatelyWithoutTransaction() {
        invalidator.evictAfterCommit(List.of(1L, 2L));

        verify(byId).evict(1L);
        verify(byId).evict(2L);
        verify(search).clear();
    }

    @Test
    @DisplayName("inside a transaction - nothing is evicted until the commit")
    void evictsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            invalidator.evictAfterCommit(List.of(1L));

            verifyNoInteractions(byId, search);

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(byId).evict(1L);
            verify(search).clear();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("inside a transaction that rolls back - cache is left untouched")
    void rollbackEvictsNothing() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            invalidator.evictAfterCommit(List.of(1L));

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verifyNoInteractions(byId, search);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Redis failure is swallowed and does not skip the remaining eviction")
    void redisFailureDoesNotPropagate() {
        doThrow(new IllegalStateException("redis down")).when(byId).evict(1L);

        assertThatCode(() -> invalidator.evictAfterCommit(List.of(1L))).doesNotThrowAnyException();

        verify(search).clear();
    }
}
