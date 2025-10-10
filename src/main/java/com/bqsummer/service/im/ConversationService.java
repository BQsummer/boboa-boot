package com.bqsummer.service.im;

import com.bqsummer.common.vo.resp.im.ConversationItem;
import com.bqsummer.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;

    public List<ConversationItem> list(Long userId) {
        return conversationMapper.listConversations(userId);
    }

    public void deleteConversation(Long userId, Long peerId) {
        conversationMapper.softDelete(userId, peerId);
    }
}

