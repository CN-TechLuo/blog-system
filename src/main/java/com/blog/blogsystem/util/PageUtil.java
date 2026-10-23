package com.blog.blogsystem.util;

/**
 * 分页参数归一化工具，统一处理 page/pageSize 钳制与偏移计算
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public final class PageUtil {

    /** SQL LIMIT 最大偏移量：防止深分页（LIMIT 100000,10）拖垮数据库 */
    private static final int MAX_OFFSET = 5000;

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

    /** 计算 SQL LIMIT 偏移量（用 long 计算，双向钳制在 [0, MAX_OFFSET]，避免整型溢出） */
    public static int start(int page, int pageSize) {
        long offset = ((long) page - 1) * pageSize;
        if (offset < 0) {
            return 0;
        }
        return (int) Math.min(offset, MAX_OFFSET);
    }

}
