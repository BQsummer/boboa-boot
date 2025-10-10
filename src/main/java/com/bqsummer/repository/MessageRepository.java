package com.bqsummer.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.mapper.MessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class MessageRepository {

    @Autowired
    private MessageMapper messageMapper;

    public List<Message> findByRecipientIdAndIdGreaterThanOrderByIdAsc(Long userId, long lastSyncId, int limit) {
        //log.info("Querying messages for userId: {}, lastSyncId: {}, limit: {}", userId, lastSyncId, limit);
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId)
                .gt("id", lastSyncId)
                .orderByAsc("id")
                .last("LIMIT " + limit);
        return messageMapper.selectList(queryWrapper);
    }

    // save
    public Message save(Message msg) {
        messageMapper.insert(msg);
        return msg;
    }

    public List<Message> findDialogHistory(Long userId, Long peerId, Long beforeId, int limit) {
        QueryWrapper<Message> qw = new QueryWrapper<>();
        qw.nested(n -> n.eq("sender_id", userId).eq("receiver_id", peerId))
          .or(n -> n.eq("sender_id", peerId).eq("receiver_id", userId));
        if (beforeId != null && beforeId > 0) {
            qw.lt("id", beforeId);
        }
        qw.orderByDesc("id").last("LIMIT " + limit);
        return messageMapper.selectList(qw);
    }
}
