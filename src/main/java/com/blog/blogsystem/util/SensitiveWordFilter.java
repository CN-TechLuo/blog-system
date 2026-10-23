package com.blog.blogsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感词过滤器（内容审核，企业级 P0） 词库文件: classpath:sensitive-words.txt（每行一个词，# 开头为注释） 用于发布文章/评论/反馈/昵称/AI 输入等用户生成内容（UGC）的合规拦截。
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
public final class SensitiveWordFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilter.class);
    private static final String WORD_FILE = "sensitive-words.txt";
    private static final Set<String> WORDS = loadWords();

    private SensitiveWordFilter() {}

    private static Set<String> loadWords() {
        try (InputStream in = SensitiveWordFilter.class.getClassLoader().getResourceAsStream(WORD_FILE)) {
            if (in == null) {
                log.error("敏感词库 {} 未找到，内容审核将 fail-closed 拒绝所有 UGC", WORD_FILE);
                return Set.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Set<String> words = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .map(SensitiveWordFilter::normalize)
                        .filter(word -> !word.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
                if (words.isEmpty()) {
                    log.error("敏感词库为空，内容审核将 fail-closed 拒绝所有 UGC");
                } else {
                    log.info("敏感词库加载完成: {} 个词", words.size());
                }
                return words;
            }
        } catch (IOException e) {
            log.error("加载敏感词库失败", e);
            return Set.of();
        }
    }

    /**
     * 归一化：NFKC 全角转半角、转小写、去除空白与零宽字符，
     * 防止用空格/零宽字符/全角字符绕过匹配。
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u200B-\\u200D\\uFEFF]+", "");
    }

    /** 是否包含敏感词；词库缺失/为空时 fail-closed */
    public static boolean containsSensitive(String text) {
        if (text == null || text.isBlank()) return false;
        if (WORDS.isEmpty()) {
            log.error("敏感词库不可用，按 fail-closed 拒绝内容");
            return true;
        }
        String normalized = normalize(text);
        for (String word : WORDS) {
            if (normalized.contains(word)) return true;
        }
        return false;
    }

    /** 返回命中的敏感词（用于审计日志），未命中返回 null */
    public static String findHit(String text) {
        if (text == null || text.isBlank() || WORDS.isEmpty()) return null;
        String normalized = normalize(text);
        for (String word : WORDS) {
            if (normalized.contains(word)) return word;
        }
        return null;
    }

    /** 将敏感词替换为 ** */
    public static String mask(String text) {
        if (text == null || text.isBlank() || WORDS.isEmpty()) return text;
        String result = text;
        for (String word : WORDS) {
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(word), "**");
        }
        return result;
    }
}
