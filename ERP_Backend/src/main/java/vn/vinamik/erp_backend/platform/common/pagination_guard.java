package vn.vinamik.erp_backend.platform.common;

public final class pagination_guard {
    private static final int max_page_number = 1_000_000;

    private pagination_guard() {
    }

    public static int normalize_page(int page) {
        if (page < 0) {
            return 0;
        }
        if (page > max_page_number) {
            throw new IllegalArgumentException("Page is too large.");
        }
        return page;
    }

    public static int offset(int page, int page_size) {
        if (page_size < 1) {
            throw new IllegalArgumentException("Page size must be positive.");
        }
        return Math.toIntExact((long) normalize_page(page) * page_size);
    }
}