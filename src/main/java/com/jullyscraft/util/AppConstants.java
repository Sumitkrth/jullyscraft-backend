package com.jullyscraft.util;

public final class AppConstants {

    private AppConstants() {}

    // Pagination defaults
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";

    // Cache names
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_USERS = "users";

    // Redis key prefixes
    public static final String REDIS_OTP_PREFIX = "otp:";
    public static final String REDIS_REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String REDIS_CART_PREFIX = "cart:";
    public static final String REDIS_RATE_LIMIT_PREFIX = "rate:";
    public static final String REDIS_SESSION_PREFIX = "session:";

    // JWT
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    // Roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_VENDOR = "ROLE_VENDOR";

    // API paths
    public static final String API_BASE = "/api/v1";
    public static final String AUTH_BASE = API_BASE + "/auth";
    public static final String USER_BASE = API_BASE + "/users";
    public static final String PRODUCT_BASE = API_BASE + "/products";
    public static final String CATEGORY_BASE = API_BASE + "/categories";
    public static final String ORDER_BASE = API_BASE + "/orders";
    public static final String CART_BASE = API_BASE + "/cart";
    public static final String ADMIN_BASE = API_BASE + "/admin";
    public static final String ANALYTICS_BASE = API_BASE + "/analytics";
}
