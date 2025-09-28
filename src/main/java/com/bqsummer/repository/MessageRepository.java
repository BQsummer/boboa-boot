package com.bqsummer.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bqsummer.common.dto.Message;
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

    public List<Message> findByRecipientIdAndIdGreaterThanOrderByIdAsc(String userId, long lastSyncId, int limit) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("receiver_id", userId)
                .gt("id", lastSyncId)
                .orderByAsc("id")
                .last("LIMIT " + limit);
        return messageMapper.selectList(queryWrapper);
    }
}
