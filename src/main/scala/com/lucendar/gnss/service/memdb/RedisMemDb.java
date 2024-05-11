package com.lucendar.gnss.service.memdb;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisMemDb implements MemDb {

    public static RedisMemDb create(String redisUri, boolean localCached) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                LettuceConnectionFactory.createRedisConfiguration(redisUri)
        );

        return new RedisMemDb(new StringRedisTemplate(factory), localCached);
    }

    private final StringRedisTemplate redisTemplate;
    private final LocalMemDb localMemDb;

    public RedisMemDb(StringRedisTemplate redisTemplate, boolean localCached) {
        this.redisTemplate = redisTemplate;
        if (localCached) {
            this.localMemDb = new LocalMemDb();
        } else
            this.localMemDb = null;
    }

    @Override
    public void set(@NonNull String keyPrefix, @NonNull String key, @NonNull String value, int ttl) {
        if (localMemDb != null) {
            localMemDb.set(keyPrefix, key, value, ttl);
        }
        redisTemplate.opsForValue().set(keyPrefix + key, value, ttl, TimeUnit.SECONDS);
    }

    @Override
    public void set(@NonNull String keyPrefix, @NonNull String key, @NonNull String value) {
        if (localMemDb != null) {
            localMemDb.set(keyPrefix, key, value);
        }
        redisTemplate.opsForValue().set(keyPrefix + key, value);
    }

    @Override
    @Nullable
    public String get(@NonNull String keyPrefix, @NonNull String key) {
        if (localMemDb != null) {
            String r = localMemDb.get(keyPrefix, key);
            if (r != null)
                return r;
        }
        return redisTemplate.opsForValue().get(keyPrefix + key);
    }

    @Override
    public void del(@NonNull String keyPrefix, @NonNull String key) {
        if (localMemDb != null) {
            localMemDb.del(keyPrefix, key);
        }

        redisTemplate.delete(keyPrefix + key);
    }
}
