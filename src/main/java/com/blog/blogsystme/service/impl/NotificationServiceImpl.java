package com.blog.blogsystme.service.impl;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.PageResponse;
import com.blog.blogsystme.entity.Notification;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.NotificationMapper;
import com.blog.blogsystme.mapper.UserMapper;
import com.blog.blogsystme.service.NotificationService;
import com.blog.blogsystme.util.PageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper, UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    @Override
    public ApiResponse<PageResponse> list(Integer userId, int page, int pageSize) {
        int p = PageUtil.page(page);
        int size = PageUtil.pageSize(pageSize, 50);
        int start = PageUtil.start(p, size);
        List<Notification> list = notificationMapper.findByUserId(userId, start, size);
        populateFromUsernames(list);
        int total = notificationMapper.countByUserId(userId);
        return ApiResponse.success("查询成功", new PageResponse(list, total, size, p));
    }

    @Override
    public ApiResponse<Integer> countUnread(Integer userId) {
        int count = notificationMapper.countUnread(userId);
        return ApiResponse.success("查询成功", count);
    }

    @Override
    public ApiResponse<Void> markAllRead(Integer userId) {
        notificationMapper.markAllRead(userId);
        return ApiResponse.success("已全部标为已读");
    }

    @Override
    public void create(Integer userId, String type, Integer fromUserId,
                       Integer articleId, Integer commentId, String content) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setFromUserId(fromUserId);
            notification.setArticleId(articleId);
            notification.setCommentId(commentId);
            notification.setContent(content);
            notificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("创建通知失败: type={}, userId={}", type, userId, e);
        }
    }

    private void populateFromUsernames(List<Notification> list) {
        if (list.isEmpty()) return;
        List<Integer> userIds = list.stream()
                .filter(n -> n.getFromUserId() != null)
                .map(Notification::getFromUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return;
        Map<Integer, String> userMap = userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        list.forEach(n -> {
            if (n.getFromUserId() != null) {
                n.setFromUsername(userMap.get(n.getFromUserId()));
            }
        });
    }
}
