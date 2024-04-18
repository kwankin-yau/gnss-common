package com.lucendar.gnss.security;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * 鉴权接口
 */
public interface Authenticator {

    /**
     * 返回会话管理器
     *
     * @return 会话管理器
     */
    @NonNull
    SessionManager sessionManager();

    /**
     * 登录。
     *
     * @param appId    AppId
     * @param userName 用户名
     * @param password 密码
     * @return 会话对象。如果验证失败，实现者必须抛出异常
     */
    @NonNull
    Session login(@NonNull String appId, @NonNull String userName, @NonNull String password);


}
