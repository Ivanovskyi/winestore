package dev.winestore.wine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

@Component
public class WineCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(WineCacheInvalidator.class);

    private final CacheManager cacheManager;

    public WineCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterCommit(Collection<Long> wineIds) {
        List<Long> ids = List.copyOf(wineIds);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow(ids);
                }
            });
        } else {
            evictNow(ids);
        }
    }

    void evictNow(List<Long> ids) {
        try {
            Cache byId = cacheManager.getCache(WineCaches.BY_ID);
            if (byId != null) {
                ids.forEach(byId::evict);
            }
        } catch (RuntimeException e) {
            logFailure(WineCaches.BY_ID, e);
        }
        try {
            Cache search = cacheManager.getCache(WineCaches.SEARCH);
            if (search != null) {
                search.clear();
            }
        } catch (RuntimeException e) {
            logFailure(WineCaches.SEARCH, e);
        }
    }

    private static void logFailure(String cacheName, RuntimeException e) {
        log.warn("Could not evict cache '{}'; stale data is possible until the TTL expires: {}", cacheName, e.toString());
    }
}
