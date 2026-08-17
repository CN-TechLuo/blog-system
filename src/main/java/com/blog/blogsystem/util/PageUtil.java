package com.blog.blogsystem.util;

/**
 * 分页参数归一化工具，统一处理 page/pageSize 钳制与偏移计算
 */
public final class PageUtil {

    private PageUtil() {}

    /** 页码至少为 1 */
    public static int page(int page) {
        return page < 1 ? 1 : page;
    }

    /** pageSize 钳制在 [1, max] 区间，非法值回退到 10（不超过 max） */
    public static int pageSize(int pageSize, int max) {
        if (pageSize < 1) {
            return Math.min(10, max);
        }
        return Math.min(pageSize, max);
    }

    /** 计算 SQL LIMIT 偏移量 */
    public static int start(int page, int pageSize) {
        return (page - 1) * pageSize;
    }

}
