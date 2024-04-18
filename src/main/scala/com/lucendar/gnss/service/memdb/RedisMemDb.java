package com.lucendar.gnss.service.memdb;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisMemDb implements MemDb {

    public static RedisMemDb create(String redisUri) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                LettuceConnectionFactory.createRedisConfiguration(redisUri)
        );

        return new RedisMemDb(new StringRedisTemplate(factory));
    }

    private final StringRedisTemplate redisTemplate;

    public RedisMemDb(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String keyPrefix, String key, String value, int ttl) {
        redisTemplate.opsForValue().set(keyPrefix + key, value, ttl, TimeUnit.SECONDS);
    }

    @Override
    public void set(String keyPrefix, String key, String value) {
        redisTemplate.opsForValue().set(keyPrefix + key, value);
    }

    @Override
    public String get(String keyPrefix, String key) {
        return redisTemplate.opsForValue().get(keyPrefix + key);
    }

    @Override
    public void del(String keyPrefix, String key) {
        redisTemplate.delete(keyPrefix + key);
    }
}
