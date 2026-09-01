package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ReportRequest;
import com.blog.blogsystem.entity.Article;
import com.blog.blogsystem.entity.Report;
import com.blog.blogsystem.mapper.ArticleMapper;
import com.blog.blogsystem.mapper.CommentMapper;
import com.blog.blogsystem.mapper.ReportMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final ArticleMapper articleMapper = mock(ArticleMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final ReportController controller = new ReportController(reportMapper, articleMapper, commentMapper);

    @Test
    void invalidTargetTypeRejected() {
        ReportRequest req = new ReportRequest();
        req.setTargetType("user");
        req.setTargetId(1);
        req.setReason("垃圾广告");

        ResponseEntity<?> r = controller.create(1, req);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void invalidReasonRejected() {
        ReportRequest req = new ReportRequest();
        req.setTargetType("article");
        req.setTargetId(1);
        req.setReason("随便写的");

        ResponseEntity<?> r = controller.create(1, req);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void missingTargetRejected() {
        when(articleMapper.findById(anyInt())).thenReturn(null);
        ReportRequest req = new ReportRequest();
        req.setTargetType("article");
        req.setTargetId(999);
        req.setReason("垃圾广告");

        ResponseEntity<?> r = controller.create(1, req);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void validReportInserted() {
        when(articleMapper.findById(10)).thenReturn(new Article());
        when(reportMapper.countPendingByReporterAndTarget(anyInt(), anyString(), anyInt())).thenReturn(0);
        when(reportMapper.insert(any(Report.class))).thenReturn(1);

        ReportRequest req = new ReportRequest();
        req.setTargetType("article");
        req.setTargetId(10);
        req.setReason("侵权");
        req.setDetail("抄袭我的文章");

        ResponseEntity<?> r = controller.create(1, req);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        verify(reportMapper).insert(any(Report.class));
    }

    @Test
    void duplicateReportRejected() {
        when(articleMapper.findById(10)).thenReturn(new Article());
        when(reportMapper.countPendingByReporterAndTarget(anyInt(), anyString(), anyInt())).thenReturn(1);

        ReportRequest req = new ReportRequest();
        req.setTargetType("article");
        req.setTargetId(10);
        req.setReason("侵权");

        ResponseEntity<?> r = controller.create(1, req);
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

}
