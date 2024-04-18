package com.lucendar.gnss.service.memdb;

/**
 * 内存数据库
 */
public interface MemDb {

    String PUBLIC_PREFIX = "gnss:";

    /**
     * 设置一个键值对及其生存期。生成的key为 `keyPrefix` + `key`。
     *
     * @param keyPrefix
     * @param key
     * @param value
     * @param ttl 生存期，单位：秒
     */
    void set(String keyPrefix, String key, String value, int ttl);

    /**
     * 设置一个永久键值对。生成的key为 `keyPrefix` + `key`。
     *
     * @param keyPrefix
     * @param key
     * @param value
     */
    void set(String keyPrefix, String key, String value);


    String get(String keyPrefix, String key);

    void del(String keyPrefix, String key);
}
