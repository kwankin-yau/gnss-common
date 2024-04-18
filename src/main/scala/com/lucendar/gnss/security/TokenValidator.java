package com.lucendar.gnss.security;


/**
 * 令牌验证器
 */
public interface TokenValidator {

    /**
     * 验证token，并返回会话对象。
     *
     * @param token 所要验证的令牌
     * @return 会话对象。 如果令牌无效，则返回 null。
     */
    Session validate(String token);

}
