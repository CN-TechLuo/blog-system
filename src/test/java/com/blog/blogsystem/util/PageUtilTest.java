package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageUtilTest {

    @Test
    void pageShouldBeAtLeastOne() {
        assertEquals(1, PageUtil.page(0));
        assertEquals(1, PageUtil.page(-5));
        assertEquals(3, PageUtil.page(3));
    }

    @Test
    void pageSizeShouldClamp() {
        assertEquals(10, PageUtil.pageSize(0, 100));
        assertEquals(10, PageUtil.pageSize(-1, 100));
        assertEquals(100, PageUtil.pageSize(500, 100));
        assertEquals(50, PageUtil.pageSize(50, 100));
        assertEquals(5, PageUtil.pageSize(-1, 5), "默认值不超过上限");
    }

    @Test
    void startShouldComputeOffset() {
        assertEquals(0, PageUtil.start(1, 10));
        assertEquals(90, PageUtil.start(10, 10));
        assertEquals(40, PageUtil.start(3, 20));
    }

}
