package com.blog.blogsystem.util;

/**
 * 分页参数归一化工具，统一处理 page/pageSize 钳制与偏移计算
 */
public final class PageUtil {

    /** SQL LIMIT 最大偏移量：防止深分页（LIMIT 100000,10）拖垮数据库 */
    private static final int MAX_OFFSET = 5000;

    /** 页码上限：配合 long 运算，杜绝 (page-1)*pageSize 溢出为负数 */
    private static final int MAX_PAGE = 1_000_000;

    private PageUtil() {}

    /** 页码钳制在 [1, MAX_PAGE] */
    public static int page(int page) {
        if (page < 1) {
            return 1;
        }
        return Math.min(page, MAX_PAGE);
    }

    /** pageSize 钳制在 [1, max] 区间，非法值回退到 10（不超过 max） */
    public static int pageSize(int pageSize, int max) {
        if (pageSize < 1) {
            return Math.min(10, max);
        }
        return Math.min(pageSize, max);
    }

    /**
     * 计算 SQL LIMIT 偏移量（深翻页时钳制在 MAX_OFFSET，避免大偏移扫描）。
     * 用 long 计算再收敛：旧实现 (page-1)*pageSize 会整型溢出成负数，
     * 拼出 LIMIT -xxx 直接 SQL 报错，未登录即可打出 500。
     */
    public static int start(int page, int pageSize) {
        int p = page(page);
        int size = pageSize < 1 ? 1 : pageSize;
        long offset = (long) (p - 1) * size;
        if (offset > MAX_OFFSET) {
            offset = MAX_OFFSET;
        }
        if (offset < 0) {
            offset = 0;
        }
        return (int) offset;
    }

}
