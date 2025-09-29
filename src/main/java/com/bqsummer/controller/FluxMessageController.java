package com.bqsummer.controller;

import com.bqsummer.common.dto.Message;
import com.bqsummer.service.im.MessageService;
import com.bqsummer.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("flux")
public class FluxMessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/api/v1/messages/poll")
    public Mono<ResponseEntity<Map<String, List<Message>>>> pollMessages(
            @RequestParam(defaultValue = "0") long last_sync_id,
            @RequestParam(defaultValue = "50", required = false) int limit,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        // 手动验证JWT
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        String token = authorizationHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        // 可将userId传递给messageService
        return messageService.pollMessages(userId, last_sync_id, limit)
                .map(messages -> {
                    Map<String, List<Message>> response = new HashMap<>();
                    response.put("messages", messages);
                    return ResponseEntity.ok(response);
                });
    }
}
