package com.bqsummer.controller;

import com.bqsummer.common.vo.resp.ConversationItem;
import com.bqsummer.service.im.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ConversationItem>> list() {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return ResponseEntity.ok(conversationService.list(uid));
    }

    @DeleteMapping("/{peerId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> delete(@PathVariable Long peerId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        conversationService.deleteConversation(uid, peerId);
        return ResponseEntity.ok("删除会话成功");
    }
}

