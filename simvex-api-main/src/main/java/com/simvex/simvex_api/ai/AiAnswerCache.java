// src/main/java/com/simvex/simvex_api/ai/AiAnswerCache.java
package com.simvex.simvex_api.ai;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiAnswerCache {

    private static final long TTL_SECONDS = 60 * 10; // 10분

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;

        if (Instant.now().getEpochSecond() > entry.expiresAt) {
            cache.remove(key);
            return null;
        }
        return entry.answer;
    }

    public void put(String key, String answer) {
        cache.put(key, new CacheEntry(
                answer,
                Instant.now().getEpochSecond() + TTL_SECONDS
        ));
    }

    private record CacheEntry(String answer, long expiresAt) {}
}
