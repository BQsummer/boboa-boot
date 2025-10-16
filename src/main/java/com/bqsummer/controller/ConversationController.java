package com.bqsummer.controller;

import com.bqsummer.common.vo.resp.im.ConversationItem;
import com.bqsummer.service.im.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<List<ConversationItem>> list() {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return ResponseEntity.ok(conversationService.list(uid));
    }

    @DeleteMapping("/{peerId}")
    public ResponseEntity<String> delete(@PathVariable Long peerId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        conversationService.deleteConversation(uid, peerId);
        return ResponseEntity.ok("删除会话成功");
    }
}

