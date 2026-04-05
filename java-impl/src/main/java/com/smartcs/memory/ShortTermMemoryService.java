package com.smartcs.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 短期记忆服务 — 基于Redis的会话缓存。
 * 存储最近N轮对话，TTL自动过期。
 * 当Redis不可用时自动降级为内存存储。
 */
@Service
public class ShortTermMemoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, List<Map<String, String>>> fallbackStore = new HashMap<>();

    private static final int MAX_TURNS = 20;
    private static final Duration TTL = Duration.ofMinutes(30);

    public ShortTermMemoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void addMessage(String sessionId, String role, String content) {
        Map<String, String> message = Map.of(
                "role", role,
                "content", content,
                "timestamp", LocalDateTime.now().toString()
        );

        try {
            String key = "smartcs:short_term:" + sessionId;
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.opsForList().trim(key, -MAX_TURNS, -1);
            redisTemplate.expire(key, TTL);
        } catch (Exception e) {
            fallbackStore.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
            List<Map<String, String>> list = fallbackStore.get(sessionId);
            if (list.size() > MAX_TURNS) {
                fallbackStore.put(sessionId, new ArrayList<>(list.subList(list.size() - MAX_TURNS, list.size())));
            }
        }
    }

    public List<Map<String, Object>> getHistory(String sessionId) {
        try {
            String key = "smartcs:short_term:" + sessionId;
            List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null) return List.of();

            List<Map<String, Object>> result = new ArrayList<>();
            for (String json : raw) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = objectMapper.readValue(json, Map.class);
                result.add(msg);
            }
            return result;
        } catch (Exception e) {
            List<Map<String, String>> fallback = fallbackStore.getOrDefault(sessionId, List.of());
            return new ArrayList<>(fallback.stream().map(m -> (Map<String, Object>) new HashMap<String, Object>(m)).toList());
        }
    }
}
