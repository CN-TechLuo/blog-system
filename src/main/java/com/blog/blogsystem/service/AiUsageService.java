package com.blog.blogsystem.service;

import com.blog.blogsystem.entity.AiUsage;
import com.blog.blogsystem.mapper.AiUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 用量统计与每日配额（企业级成本管控）
 * 每个用户每日默认限额 100 次调用，超限拒绝；全部调用落库可审计。
 */
@Service
public class AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);

    private final AiUsageMapper aiUsageMapper;

    @Value("${app.ai.daily-limit:100}")
    private int dailyLimit;

    public AiUsageService(AiUsageMapper aiUsageMapper) {
        this.aiUsageMapper = aiUsageMapper;
    }

    /** 检查配额，超限返回错误消息，未超限返回 null */
    public String checkQuota(Integer userId) {
        if (userId == null) return "未登录";
        try {
            int used = aiUsageMapper.countToday(userId);
            if (used >= dailyLimit) {
                return "今日 AI 使用次数已达上限（" + dailyLimit + " 次/天），请明天再试";
            }
        } catch (Exception e) {
            log.warn("AI 配额查询失败（表可能尚未迁移）", e);
        }
        return null;
    }

    public void record(Integer userId, String apiType, int inputChars, int outputChars) {
        if (userId == null) return;
        try {
            aiUsageMapper.insert(userId, apiType, inputChars, outputChars);
        } catch (Exception e) {
            log.warn("AI 用量记录失败: userId={}, type={}", userId, apiType, e);
        }
    }

    public List<AiUsage> recent(int limit) {
        return aiUsageMapper.findRecent(Math.min(Math.max(limit, 1), 500));
    }
}
