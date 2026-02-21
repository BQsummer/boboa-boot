package com.bqsummer.controller;

import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.vo.req.im.SendMessageRequest;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.service.im.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Message", description = "Message send/history API")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @PostMapping
    @Operation(summary = "Send message", description = "Send message to peer")
    public void sendMessage(@Valid @RequestBody SendMessageRequest request) {
        messageService.sendMessage(request);
    }

    @GetMapping("/history")
    @Operation(summary = "Get history", description = "Get paged dialog history with a peer")
    public List<Message> history(
            @Parameter(description = "Peer ID") @RequestParam Long peerId,
            @Parameter(description = "Query records before this message ID") @RequestParam(required = false) Long beforeId,
            @Parameter(description = "Limit 1-100") @RequestParam(defaultValue = "20") int limit) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        limit = Math.min(Math.max(limit, 1), 100);
        return messageRepository.findDialogHistory(uid, peerId, beforeId, limit);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent messages", description = "Get recent dialog messages with a peer")
    public List<Message> recent(
            @Parameter(description = "Peer ID") @RequestParam Long peerId,
            @Parameter(description = "Limit 1-100") @RequestParam(defaultValue = "50") int limit) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        limit = Math.min(Math.max(limit, 1), 100);
        return messageService.getRecentMessages(uid, peerId, limit);
    }

    @PostMapping("/clear-session")
    @Operation(summary = "Clear session", description = "Mark current dialog messages as deleted")
    public Map<String, Integer> clearSession(@RequestParam Long peerId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        int updated = messageService.clearSession(uid, peerId);
        return Map.of("updatedCount", updated);
    }

    @PostMapping("/clear-context")
    @Operation(summary = "Clear context", description = "Mark current dialog messages as out of prompt context")
    public Map<String, Integer> clearContext(@RequestParam Long peerId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        int updated = messageService.clearContext(uid, peerId);
        return Map.of("updatedCount", updated);
    }

    @PostMapping("/regenerate-last")
    @Operation(summary = "Regenerate last AI reply", description = "Soft-delete latest AI reply in current dialog and regenerate")
    public Map<String, Object> regenerateLast(@RequestParam Long peerId) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        MessageService.RegenerateResult result = messageService.regenerateLastAiReply(uid, peerId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("regenerated", result.regenerated());
        resp.put("deletedMessageId", result.deletedMessageId());
        resp.put("taskId", result.taskId());
        resp.put("message", result.message());
        return resp;
    }
}
