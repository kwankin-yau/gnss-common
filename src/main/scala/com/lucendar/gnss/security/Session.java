package com.lucendar.gnss.security;

import com.lucendar.gnss.sdk.GnssConsts;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class Session extends AbstractAuthenticationToken {

    public final String appId;
    public final String userName;
    public final long loginTime;
    public final String token;

    public volatile long lastAccess;

    public Session(@NonNull String appId, @NonNull String userName, long loginTime, @NonNull String token) {
        super(null);
        this.appId = appId;
        this.userName = userName;
        this.loginTime = loginTime;
        this.token = token;

        lastAccess = loginTime;
    }

    @Override
    public Object getCredentials() {
        return "not-used";
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    public String appIdDef() {
        if (appId != null)
            return appId;
        else
            return GnssConsts.DEFAULT_APP_ID;
    }

    public boolean appIdIs(String appId) {
        return this.appId.equals(appId);
    }

    public boolean userNameIs(String userName) {
        return this.userName.equals(userName);
    }
}
