package com.lucendar.gnss.security;


/**
 * The SessionManager implementation must be threadsafe.
 */
public interface SessionManager {
    void updateLastAccess(Session session, long lastAccess);
    void put(Session session);
    Session get(String token);
    void remove(String token);
}
