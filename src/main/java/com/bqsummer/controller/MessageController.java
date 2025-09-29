package com.bqsummer.controller;

import com.bqsummer.common.dto.Message;
import com.bqsummer.common.vo.req.SendMessageRequest;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.service.im.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private MessageRepository messageRepository;

    @PostMapping
    public void sendMessage(@Valid @RequestBody SendMessageRequest request) {

        messageService.sendMessage(request);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public List<Message> history(@RequestParam Long peerId,
                                 @RequestParam(required = false) Long beforeId,
                                 @RequestParam(defaultValue = "20") int limit) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        limit = Math.min(Math.max(limit, 1), 100);
        return messageRepository.findDialogHistory(uid, peerId, beforeId, limit);
    }
}
