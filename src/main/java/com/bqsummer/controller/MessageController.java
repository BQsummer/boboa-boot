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

import java.util.List;


@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Message", description = "消息发送与历史记录API，支持对话记忆功能")
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private MessageRepository messageRepository;

    @PostMapping
    @Operation(
        summary = "发送消息",
        description = "发送消息给AI角色，系统会自动存储对话记忆、生成总结（达30条时）、提取长期记忆"
    )
    public void sendMessage(@Valid @RequestBody SendMessageRequest request) {
        messageService.sendMessage(request);
    }

    @GetMapping("/history")
    @Operation(
        summary = "获取对话历史",
        description = "分页获取与指定用户/角色的对话历史记录"
    )
    public List<Message> history(
            @Parameter(description = "对话对方ID（用户或AI角色）") @RequestParam Long peerId,
            @Parameter(description = "查询此消息ID之前的记录（用于分页）") @RequestParam(required = false) Long beforeId,
            @Parameter(description = "返回数量限制（1-100）") @RequestParam(defaultValue = "20") int limit) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        limit = Math.min(Math.max(limit, 1), 100);
        return messageRepository.findDialogHistory(uid, peerId, beforeId, limit);
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近消息", description = "获取当前用户与指定用户/角色的最近消息（按消息ID升序返回）")
    public List<Message> recent(
            @Parameter(description = "对话对方ID（用户或AI角色）") @RequestParam Long peerId,
            @Parameter(description = "返回数量限制（1-100）") @RequestParam(defaultValue = "50") int limit) {
        Long uid = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        limit = Math.min(Math.max(limit, 1), 100);
        return messageService.getRecentMessages(uid, peerId, limit);
    }
}
