package com.bqsummer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.inbox.InboxMessage;
import com.bqsummer.common.dto.inbox.InboxMessageBatchSend;
import com.bqsummer.common.dto.inbox.InboxUserMessage;
import com.bqsummer.common.vo.req.inbox.AdminCreateInboxMessageReq;
import com.bqsummer.common.vo.req.inbox.AdminUpdateInboxMessageReq;
import com.bqsummer.common.vo.resp.inbox.AdminInboxMessageResp;
import com.bqsummer.common.vo.resp.inbox.InboxRecipientResp;
import com.bqsummer.common.vo.resp.inbox.InboxStatsResp;
import com.bqsummer.common.vo.resp.inbox.UserInboxMessageResp;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.InboxMessageBatchSendMapper;
import com.bqsummer.mapper.InboxMessageMapper;
import com.bqsummer.mapper.InboxUserMessageMapper;
import com.bqsummer.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxMessageMapper inboxMessageMapper;
    private final InboxUserMessageMapper inboxUserMessageMapper;
    private final InboxMessageBatchSendMapper inboxMessageBatchSendMapper;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public AdminInboxMessageResp create(AdminCreateInboxMessageReq req) {
        Long operatorId = currentUserId();
        List<Long> targetUserIds = resolveTargetUserIds(req.getSendType(), req.getTargetUserIds());
        LocalDateTime now = LocalDateTime.now();

        InboxMessage message = new InboxMessage();
        message.setMsgType(req.getMsgType());
        message.setTitle(req.getTitle());
        message.setContent(req.getContent());
        message.setSenderId(req.getSenderId() == null ? 0L : req.getSenderId());
        message.setBizType(req.getBizType());
        message.setBizId(req.getBizId());
        message.setExtra(req.getExtra());
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        message.setCreatedBy(operatorId);
        message.setUpdatedBy(operatorId);
        inboxMessageMapper.insert(message);

        int successCount = 0;
        for (Long userId : targetUserIds) {
            InboxUserMessage relation = new InboxUserMessage();
            relation.setUserId(userId);
            relation.setMessageId(message.getId());
            relation.setReadStatus(0);
            relation.setDeleteStatus(0);
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);
            relation.setCreatedBy(operatorId);
            relation.setUpdatedBy(operatorId);
            successCount += inboxUserMessageMapper.insert(relation);
        }

        InboxMessageBatchSend batchSend = new InboxMessageBatchSend();
        batchSend.setMessageId(message.getId());
        batchSend.setSendType(req.getSendType());
        batchSend.setTargetCount(targetUserIds.size());
        batchSend.setSuccessCount(successCount);
        batchSend.setCreatedAt(now);
        batchSend.setUpdatedAt(now);
        batchSend.setCreatedBy(operatorId);
        batchSend.setUpdatedBy(operatorId);
        inboxMessageBatchSendMapper.insert(batchSend);

        return buildAdminMessageResp(message, batchSend.getSendType());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminInboxMessageResp update(Long messageId, AdminUpdateInboxMessageReq req) {
        Long operatorId = currentUserId();
        InboxMessage message = inboxMessageMapper.selectById(messageId);
        if (message == null) {
            throw new SnorlaxClientException("站内信不存在");
        }

        message.setMsgType(req.getMsgType() == null ? message.getMsgType() : req.getMsgType());
        message.setTitle(req.getTitle());
        message.setContent(req.getContent());
        message.setSenderId(req.getSenderId() == null ? message.getSenderId() : req.getSenderId());
        message.setBizType(req.getBizType());
        message.setBizId(req.getBizId());
        message.setExtra(req.getExtra());
        message.setUpdatedAt(LocalDateTime.now());
        message.setUpdatedBy(operatorId);
        inboxMessageMapper.updateById(message);

        return detail(messageId);
    }

    public Page<AdminInboxMessageResp> adminPage(Integer msgType, String keyword, String bizType, long page, long size) {
        LambdaQueryWrapper<InboxMessage> wrapper = new LambdaQueryWrapper<InboxMessage>()
                .eq(msgType != null, InboxMessage::getMsgType, msgType)
                .eq(bizType != null && !bizType.isBlank(), InboxMessage::getBizType, bizType)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(InboxMessage::getTitle, keyword)
                        .or()
                        .like(InboxMessage::getContent, keyword))
                .orderByDesc(InboxMessage::getCreatedAt);

        Page<InboxMessage> rawPage = inboxMessageMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminInboxMessageResp> records = rawPage.getRecords().stream()
                .map(item -> buildAdminMessageResp(item, findSendType(item.getId())))
                .toList();

        Page<AdminInboxMessageResp> result = new Page<>(page, size, rawPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public AdminInboxMessageResp detail(Long messageId) {
        InboxMessage message = inboxMessageMapper.selectById(messageId);
        if (message == null) {
            throw new SnorlaxClientException("站内信不存在");
        }
        return buildAdminMessageResp(message, findSendType(messageId));
    }

    public Page<InboxRecipientResp> recipients(Long messageId, Integer readStatus, Integer deleteStatus, long page, long size) {
        InboxMessage exists = inboxMessageMapper.selectById(messageId);
        if (exists == null) {
            throw new SnorlaxClientException("站内信不存在");
        }

        LambdaQueryWrapper<InboxUserMessage> wrapper = new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getMessageId, messageId)
                .eq(readStatus != null, InboxUserMessage::getReadStatus, readStatus)
                .eq(deleteStatus != null, InboxUserMessage::getDeleteStatus, deleteStatus)
                .orderByDesc(InboxUserMessage::getCreatedAt);
        Page<InboxUserMessage> rawPage = inboxUserMessageMapper.selectPage(new Page<>(page, size), wrapper);

        List<InboxRecipientResp> records = rawPage.getRecords().stream()
                .map(item -> InboxRecipientResp.builder()
                        .id(item.getId())
                        .userId(item.getUserId())
                        .messageId(item.getMessageId())
                        .readStatus(item.getReadStatus())
                        .readAt(item.getReadAt())
                        .deleteStatus(item.getDeleteStatus())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .toList();

        Page<InboxRecipientResp> result = new Page<>(page, size, rawPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public InboxStatsResp stats() {
        long totalMessages = inboxMessageMapper.selectCount(null);
        long totalUserMessages = inboxUserMessageMapper.selectCount(null);
        long unreadUserMessages = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getReadStatus, 0)
                .eq(InboxUserMessage::getDeleteStatus, 0));
        long readUserMessages = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getReadStatus, 1)
                .eq(InboxUserMessage::getDeleteStatus, 0));
        long deletedUserMessages = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getDeleteStatus, 1));
        long todayBatchSendCount = inboxMessageBatchSendMapper.selectCount(new LambdaQueryWrapper<InboxMessageBatchSend>()
                .ge(InboxMessageBatchSend::getCreatedAt, LocalDate.now().atStartOfDay()));

        QueryWrapper<InboxMessage> groupedQuery = new QueryWrapper<>();
        groupedQuery.select("msg_type", "COUNT(*) AS cnt").groupBy("msg_type");
        List<Map<String, Object>> grouped = inboxMessageMapper.selectMaps(groupedQuery);
        Map<Integer, Long> msgTypeStats = new LinkedHashMap<>();
        for (Map<String, Object> row : grouped) {
            Integer type = row.get("msg_type") == null ? null : Integer.valueOf(String.valueOf(row.get("msg_type")));
            Long cnt = row.get("cnt") == null ? 0L : Long.valueOf(String.valueOf(row.get("cnt")));
            if (type != null) {
                msgTypeStats.put(type, cnt);
            }
        }

        return InboxStatsResp.builder()
                .totalMessages(totalMessages)
                .totalUserMessages(totalUserMessages)
                .readUserMessages(readUserMessages)
                .unreadUserMessages(unreadUserMessages)
                .deletedUserMessages(deletedUserMessages)
                .todayBatchSendCount(todayBatchSendCount)
                .msgTypeStats(msgTypeStats)
                .build();
    }

    public Page<UserInboxMessageResp> userPage(Integer readStatus, long page, long size) {
        Long currentUserId = currentUserId();

        LambdaQueryWrapper<InboxUserMessage> wrapper = new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getUserId, currentUserId)
                .eq(InboxUserMessage::getDeleteStatus, 0)
                .eq(readStatus != null, InboxUserMessage::getReadStatus, readStatus)
                .orderByDesc(InboxUserMessage::getCreatedAt);
        Page<InboxUserMessage> rawPage = inboxUserMessageMapper.selectPage(new Page<>(page, size), wrapper);

        List<Long> messageIds = rawPage.getRecords().stream().map(InboxUserMessage::getMessageId).distinct().toList();
        Map<Long, InboxMessage> messageMap = messageIds.isEmpty()
                ? Collections.emptyMap()
                : inboxMessageMapper.selectBatchIds(messageIds).stream()
                .collect(Collectors.toMap(InboxMessage::getId, item -> item, (a, b) -> a));

        List<UserInboxMessageResp> records = new ArrayList<>();
        for (InboxUserMessage rel : rawPage.getRecords()) {
            InboxMessage message = messageMap.get(rel.getMessageId());
            if (message == null) {
                continue;
            }
            records.add(UserInboxMessageResp.builder()
                    .messageId(message.getId())
                    .msgType(message.getMsgType())
                    .title(message.getTitle())
                    .content(message.getContent())
                    .senderId(message.getSenderId())
                    .bizType(message.getBizType())
                    .bizId(message.getBizId())
                    .extra(message.getExtra())
                    .readStatus(rel.getReadStatus())
                    .readAt(rel.getReadAt())
                    .createdAt(rel.getCreatedAt())
                    .build());
        }

        Page<UserInboxMessageResp> result = new Page<>(page, size, rawPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public long unreadCount() {
        Long currentUserId = currentUserId();
        return inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getUserId, currentUserId)
                .eq(InboxUserMessage::getDeleteStatus, 0)
                .eq(InboxUserMessage::getReadStatus, 0));
    }

    @Transactional(rollbackFor = Exception.class)
    public int markRead(Long messageId) {
        return markReadBatch(List.of(messageId));
    }

    @Transactional(rollbackFor = Exception.class)
    public int markReadBatch(List<Long> messageIds) {
        List<Long> ids = normalizeIds(messageIds);
        if (ids.isEmpty()) {
            return 0;
        }
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        return inboxUserMessageMapper.update(null, new LambdaUpdateWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getUserId, userId)
                .in(InboxUserMessage::getMessageId, ids)
                .eq(InboxUserMessage::getDeleteStatus, 0)
                .eq(InboxUserMessage::getReadStatus, 0)
                .set(InboxUserMessage::getReadStatus, 1)
                .set(InboxUserMessage::getReadAt, now)
                .set(InboxUserMessage::getUpdatedAt, now)
                .set(InboxUserMessage::getUpdatedBy, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(Long messageId) {
        return deleteBatch(List.of(messageId));
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<Long> messageIds) {
        List<Long> ids = normalizeIds(messageIds);
        if (ids.isEmpty()) {
            return 0;
        }
        Long userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        return inboxUserMessageMapper.update(null, new LambdaUpdateWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getUserId, userId)
                .in(InboxUserMessage::getMessageId, ids)
                .eq(InboxUserMessage::getDeleteStatus, 0)
                .set(InboxUserMessage::getDeleteStatus, 1)
                .set(InboxUserMessage::getUpdatedAt, now)
                .set(InboxUserMessage::getUpdatedBy, userId));
    }

    private List<Long> resolveTargetUserIds(Integer sendType, List<Long> requestedUserIds) {
        if (sendType == null) {
            throw new SnorlaxClientException("发送类型不能为空");
        }
        if (sendType == 1) {
            return userMapper.findAllActiveRealUserIds();
        }
        if (sendType == 2 || sendType == 3) {
            List<Long> userIds = normalizeIds(requestedUserIds);
            if (userIds.isEmpty()) {
                throw new SnorlaxClientException("指定发送时目标用户不能为空");
            }
            Set<Long> validUserIds = userMapper.findAllActiveRealUserIds().stream().collect(Collectors.toSet());
            List<Long> target = userIds.stream().filter(validUserIds::contains).toList();
            if (target.isEmpty()) {
                throw new SnorlaxClientException("未找到有效目标用户");
            }
            return target;
        }
        throw new SnorlaxClientException("不支持的发送类型");
    }

    private AdminInboxMessageResp buildAdminMessageResp(InboxMessage message, Integer sendType) {
        Long totalCount = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getMessageId, message.getId()));
        Long readCount = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getMessageId, message.getId())
                .eq(InboxUserMessage::getReadStatus, 1)
                .eq(InboxUserMessage::getDeleteStatus, 0));
        Long unreadCount = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getMessageId, message.getId())
                .eq(InboxUserMessage::getReadStatus, 0)
                .eq(InboxUserMessage::getDeleteStatus, 0));
        Long deletedCount = inboxUserMessageMapper.selectCount(new LambdaQueryWrapper<InboxUserMessage>()
                .eq(InboxUserMessage::getMessageId, message.getId())
                .eq(InboxUserMessage::getDeleteStatus, 1));

        return AdminInboxMessageResp.builder()
                .id(message.getId())
                .msgType(message.getMsgType())
                .title(message.getTitle())
                .content(message.getContent())
                .senderId(message.getSenderId())
                .bizType(message.getBizType())
                .bizId(message.getBizId())
                .extra(message.getExtra())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .createdBy(message.getCreatedBy())
                .updatedBy(message.getUpdatedBy())
                .sendType(sendType)
                .targetCount(totalCount)
                .readCount(readCount)
                .unreadCount(unreadCount)
                .deletedCount(deletedCount)
                .build();
    }

    private Integer findSendType(Long messageId) {
        InboxMessageBatchSend batch = inboxMessageBatchSendMapper.selectOne(new LambdaQueryWrapper<InboxMessageBatchSend>()
                .eq(InboxMessageBatchSend::getMessageId, messageId)
                .orderByDesc(InboxMessageBatchSend::getCreatedAt)
                .last("LIMIT 1"));
        return batch == null ? null : batch.getSendType();
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(item -> item != null && item > 0)
                .distinct()
                .toList();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new SnorlaxClientException("未登录");
        }
        Object details = authentication.getDetails();
        if (details instanceof Long value) {
            return value;
        }
        try {
            return Long.parseLong(String.valueOf(details));
        } catch (NumberFormatException ex) {
            throw new SnorlaxClientException("登录态异常");
        }
    }
}
