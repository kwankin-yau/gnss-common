package com.lucendar.gnss.service.memdb;

import com.google.gson.Gson;
import com.lucendar.gnss.sdk.GnssConsts;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Type;

/**
 * 内存数据库
 */
public interface MemDb {

    String PUBLIC_PREFIX = "gnss:";

    /**
     * 设置一个键值对及其生存期。生成的key为 `keyPrefix` + `key`。
     *
     * @param keyPrefix key 的前缀
     * @param key       key 的后部
     * @param value     值
     * @param ttl       生存期，单位：秒
     */
    void set(@NonNull String keyPrefix, @NonNull String key, @NonNull String value, int ttl);

    /**
     * 设置一个永久键值对。生成的key为 `keyPrefix` + `key`。
     *
     * @param keyPrefix key 的前缀
     * @param key       key 的后部
     * @param value     值
     */
    void set(@NonNull String keyPrefix, @NonNull String key, @NonNull String value);

    default void setJsonObjectTtl(@NonNull String keyPrefix,
                                  @NonNull String key,
                                  @NonNull Gson gson,
                                  @NonNull Object obj,
                                  int ttl) {
        var s = gson.toJson(obj);
        set(keyPrefix, key, s, ttl);
    }

    default void setJsonObjectTtl(@NonNull String keyPrefix,
                                  @NonNull String key,
                                  @NonNull Object obj,
                                  int ttl) {
        setJsonObjectTtl(keyPrefix, key, GnssConsts.GSON, obj, ttl);
    }

    default void setJsonObject(@NonNull String keyPrefix,
                               @NonNull String key,
                               @NonNull Gson gson,
                               @NonNull Object obj) {
        var s = gson.toJson(obj);
        set(keyPrefix, key, s);
    }

    default void setJsonObject(@NonNull String keyPrefix,
                               @NonNull String key,
                               @NonNull Object obj) {
        setJsonObject(keyPrefix, key, GnssConsts.GSON, obj);
    }

    /**
     * 取给定键的值。生成的key为 `keyPrefix` + `key`。
     *
     * @param keyPrefix key 的前缀
     * @param key       key 的后部
     * @return 给定键的值。如果 key 没找到，返回 null。
     */
    @Nullable
    String get(@NonNull String keyPrefix, @NonNull String key);

    /**
     * 取给定键的值，并转换成对象返回。
     *
     * @param keyPrefix key 的前缀
     * @param key       key 的后部
     * @param type      返回的对象的类型
     * @param gson      用于实施转换的 GSON 对象
     * @param <T>       返回的对象的类型
     * @return 换成后的对象，如果 key 没找到，返回 null。
     */
    @Nullable
    default <T> T getJsonObject(@NonNull String keyPrefix, @NonNull String key, @NonNull Type type, @NonNull Gson gson) {
        return gson.fromJson(get(keyPrefix, key), type);
    }

    default <T> T getJsonObject(@NonNull String keyPrefix, @NonNull String key, @NonNull Type type) {
        return getJsonObject(keyPrefix, key, type, GnssConsts.GSON);
    }

    /**
     * 删除键值对。实际的 key为 `keyPrefix` + `key`。
     *
     * @param keyPrefix key 的前缀
     * @param key       key 的后部
     */
    void del(@NonNull String keyPrefix, @NonNull String key);
}
