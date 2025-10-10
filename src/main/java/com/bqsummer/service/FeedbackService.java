package com.bqsummer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.feedback.Feedback;
import com.bqsummer.common.vo.req.feedback.SubmitFeedbackRequest;
import com.bqsummer.mapper.FeedbackMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;
    private final ObjectMapper objectMapper;

    public Long submit(SubmitFeedbackRequest req, String clientIp, String userAgent) {
        Feedback fb = new Feedback();
        fb.setType(req.getType());
        fb.setContent(req.getContent());
        fb.setContact(req.getContact());
        fb.setImages(toJson(req.getImages()));
        fb.setAppVersion(req.getAppVersion());
        fb.setOsVersion(req.getOsVersion());
        fb.setDeviceModel(req.getDeviceModel());
        fb.setNetworkType(req.getNetworkType());
        fb.setPageRoute(req.getPageRoute());
        fb.setUserId(req.getUserId());
        // store extraData and a few auto extras
        fb.setExtraData(toJson(mergeExtra(req.getExtraData(), clientIp, userAgent)));
        fb.setStatus("NEW");
        feedbackMapper.insert(fb);
        return fb.getId();
    }

    public Page<Feedback> list(String type, String status, Long userId, long page, long size) {
        return feedbackMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Feedback>()
                        .eq(type != null && !type.isBlank(), Feedback::getType, type)
                        .eq(status != null && !status.isBlank(), Feedback::getStatus, status)
                        .eq(userId != null, Feedback::getUserId, userId)
                        .orderByDesc(Feedback::getCreatedTime));
    }

    public Page<Feedback> searchList(String type, String status, Long userId, String keyword, long page, long size) {
        return feedbackMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Feedback>()
                        .eq(type != null && !type.isBlank(), Feedback::getType, type)
                        .eq(status != null && !status.isBlank(), Feedback::getStatus, status)
                        .eq(userId != null, Feedback::getUserId, userId)
                        .and(wrapper -> wrapper
                                .like(Feedback::getContent, keyword)
                                .or()
                                .like(Feedback::getContact, keyword)
                                .or()
                                .like(Feedback::getHandlerRemark, keyword))
                        .orderByDesc(Feedback::getCreatedTime));
    }

    public Feedback detail(Long id) {
        return feedbackMapper.selectById(id);
    }

    public int updateStatus(Long id, String status, String remark, Long handlerUserId) {
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) return 0;

        fb.setStatus(status);
        fb.setHandlerRemark(remark);
        fb.setHandlerUserId(handlerUserId);
        return feedbackMapper.updateById(fb);
    }

    public int delete(Long id) {
        return feedbackMapper.deleteById(id);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总数统计
        Long totalCount = feedbackMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        // 按状态统计
        Map<String, Long> statusStats = new HashMap<>();
        statusStats.put("NEW", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "NEW")));
        statusStats.put("IN_PROGRESS", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "IN_PROGRESS")));
        statusStats.put("RESOLVED", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "RESOLVED")));
        statusStats.put("REJECTED", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "REJECTED")));
        stats.put("statusStats", statusStats);

        // 按类型统计
        Map<String, Long> typeStats = new HashMap<>();
        typeStats.put("bug", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getType, "bug")));
        typeStats.put("suggestion", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getType, "suggestion")));
        typeStats.put("content", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getType, "content")));
        typeStats.put("ux", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getType, "ux")));
        typeStats.put("other", feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getType, "other")));
        stats.put("typeStats", typeStats);

        return stats;
    }

    public int batchUpdateStatus(List<Long> ids, String status, String remark, Long handlerUserId) {
        int updatedCount = 0;
        for (Long id : ids) {
            int result = updateStatus(id, status, remark, handlerUserId);
            updatedCount += result;
        }
        return updatedCount;
    }

    public int batchDelete(List<Long> ids) {
        return feedbackMapper.deleteBatchIds(ids);
    }

    public Feedback getUserFeedback(Long id, Long userId) {
        return feedbackMapper.selectOne(
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getId, id)
                        .eq(Feedback::getUserId, userId));
    }

    private Map<String, Object> mergeExtra(Map<String, Object> extra, String clientIp, String ua) {
        if (extra == null) extra = new HashMap<>();
        if (clientIp != null) extra.put("clientIp", clientIp);
        if (ua != null) extra.put("userAgent", ua);
        return extra;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }
}
