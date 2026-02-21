package com.bqsummer.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class MessageRepository {

    private static final int MESSAGE_CONTENT_MAX_LENGTH = 2048;

    @Autowired
    private MessageMapper messageMapper;

    public List<Message> findByRecipientIdAndIdGreaterThanOrderByIdAsc(Long userId, long lastSyncId, int limit) {
        //log.info("Querying messages for userId: {}, lastSyncId: {}, limit: {}", userId, lastSyncId, limit);
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId)
                .eq("is_deleted", false)
                .gt("id", lastSyncId)
                .orderByAsc("id")
                .last("LIMIT " + limit);
        return messageMapper.selectList(queryWrapper);
    }

    public List<Message> findByUserIdAndIdGreaterThanOrderByIdAsc(Long userId, long lastSyncId, int limit) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .nested(n -> n.eq("sender_id", userId).or().eq("receiver_id", userId))
                .eq("is_deleted", false)
                .gt("id", lastSyncId)
                .orderByAsc("id")
                .last("LIMIT " + limit);
        return messageMapper.selectList(queryWrapper);
    }

    public List<Message> findDialogByUsersAndIdGreaterThanOrderByIdAsc(Long userId, Long peerId, long lastSyncId, int limit) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .nested(n -> n.nested(m -> m.eq("sender_id", userId).eq("receiver_id", peerId))
                        .or()
                        .nested(m -> m.eq("sender_id", peerId).eq("receiver_id", userId)))
                .eq("is_deleted", false)
                .gt("id", lastSyncId)
                .orderByAsc("id")
                .last("LIMIT " + limit);
        return messageMapper.selectList(queryWrapper);
    }

    // save
    public Message save(Message msg) {
        String originalContent = msg.getContent();
        String truncatedContent = truncateByCodePoint(originalContent, MESSAGE_CONTENT_MAX_LENGTH);
        msg.setContent(truncatedContent);
        messageMapper.insert(msg);
        return msg;
    }

    public List<Message> findDialogHistory(Long userId, Long peerId, Long beforeId, int limit) {
        return findDialogHistory(userId, peerId, beforeId, limit, false);
    }

    public List<Message> findDialogHistoryForPrompt(Long userId, Long peerId, Long beforeId, int limit) {
        return findDialogHistory(userId, peerId, beforeId, limit, true);
    }

    private List<Message> findDialogHistory(Long userId, Long peerId, Long beforeId, int limit, boolean onlyInContext) {
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.nested(n -> n.nested(m -> m.eq("sender_id", userId).eq("receiver_id", peerId))
                .or()
                .nested(m -> m.eq("sender_id", peerId).eq("receiver_id", userId)));
        qw.eq("is_deleted", false);
        if (onlyInContext) {
            qw.eq("is_in_context", true);
        }
        if (beforeId != null && beforeId > 0) {
            qw.lt("id", beforeId);
        }
        qw.orderByDesc("id").last("LIMIT " + limit);
        return messageMapper.selectList(qw);
    }

    public List<Message> findRecentDialogMessages(Long userId, Long peerId, int limit) {
        return findDialogHistory(userId, peerId, null, limit);
    }

    public int clearSession(Long userId, Long peerId) {
        UpdateWrapper<Message> uw = new UpdateWrapper<>();
        uw.nested(n -> n.nested(m -> m.eq("sender_id", userId).eq("receiver_id", peerId))
                .or()
                .nested(m -> m.eq("sender_id", peerId).eq("receiver_id", userId)))
                .eq("is_deleted", false)
                .set("is_deleted", true)
                .setSql("updated_at = NOW()");
        return messageMapper.update(null, uw);
    }

    public int clearContext(Long userId, Long peerId) {
        UpdateWrapper<Message> uw = new UpdateWrapper<>();
        uw.nested(n -> n.nested(m -> m.eq("sender_id", userId).eq("receiver_id", peerId))
                .or()
                .nested(m -> m.eq("sender_id", peerId).eq("receiver_id", userId)))
                .eq("is_deleted", false)
                .eq("is_in_context", true)
                .set("is_in_context", false)
                .setSql("updated_at = NOW()");
        return messageMapper.update(null, uw);
    }

    public Message findLatestActiveMessageBySenderAndReceiver(Long senderId, Long receiverId) {
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.eq("sender_id", senderId)
                .eq("receiver_id", receiverId)
                .eq("is_deleted", false)
                .orderByDesc("id")
                .last("LIMIT 1");
        return messageMapper.selectOne(qw);
    }

    public int softDeleteById(Long id) {
        UpdateWrapper<Message> uw = new UpdateWrapper<>();
        uw.eq("id", id)
                .eq("is_deleted", false)
                .set("is_deleted", true)
                .setSql("updated_at = NOW()");
        return messageMapper.update(null, uw);
    }

    private String truncateByCodePoint(String content, int maxCodePoints) {
        if (content == null) {
            return null;
        }
        int codePointLength = content.codePointCount(0, content.length());
        if (codePointLength <= maxCodePoints) {
            return content;
        }
        int endIndex = content.offsetByCodePoints(0, maxCodePoints);
        return content.substring(0, endIndex);
    }
}
