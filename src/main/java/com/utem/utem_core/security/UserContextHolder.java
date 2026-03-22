package com.utem.utem_core.security;

public class UserContextHolder {

    private static final ThreadLocal<AuthenticatedUser> HOLDER = new ThreadLocal<>();

    public static void set(AuthenticatedUser user) {
        HOLDER.set(user);
    }

    public static AuthenticatedUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
