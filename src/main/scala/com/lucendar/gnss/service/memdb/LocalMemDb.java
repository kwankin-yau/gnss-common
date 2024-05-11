/*
 * Copyright (c) 2024  lucendar.com.
 * All rights reserved.
 */
package com.lucendar.gnss.service.memdb;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.TimeUnit;

public class LocalMemDb implements MemDb {

    private record ValueEntry(String value, long ttlNano) {
    }

    private final Cache<String, ValueEntry> cache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, ValueEntry>() {
                @Override
                public long expireAfterCreate(String key, ValueEntry value, long currentTime) {
                    return value.ttlNano;
                }

                @Override
                public long expireAfterUpdate(
                        String key,
                        ValueEntry value,
                        long currentTime,
                        @NonNegative long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(
                        String key,
                        ValueEntry value,
                        long currentTime,
                        @NonNegative long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    public void internalSet(String keyPrefix, String key, String value, long ttlNano) {
        var entry = new ValueEntry(value, ttlNano);
        cache.put(keyPrefix + key, entry);
    }

    @Override
    public void set(@NonNull String keyPrefix, @NonNull String key, @NonNull String value, int ttl) {
        internalSet(keyPrefix, key, value, TimeUnit.SECONDS.toNanos(ttl));
    }

    @Override
    public void set(@NonNull String keyPrefix, @NonNull String key, @NonNull String value) {
        internalSet(keyPrefix, key, value, Long.MAX_VALUE);
    }

    @Override
    @Nullable
    public String get(@NonNull String keyPrefix, @NonNull String key) {
        var e = cache.getIfPresent(keyPrefix + key);
        if (e != null)
            return e.value;
        else
            return null;
    }

    @Override
    public void del(@NonNull String keyPrefix, @NonNull String key) {
        cache.invalidate(keyPrefix + key);
    }
}
