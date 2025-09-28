package com.bqsummer.controller;

import com.bqsummer.common.dto.Message;
import com.bqsummer.service.im.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FluxMessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/api/v1/messages/poll")
    public Mono<ResponseEntity<Map<String, List<Message>>>> pollMessages(
            Principal principal,
            @RequestParam(defaultValue = "0") long last_sync_id,
            @RequestParam(defaultValue = "50", required = false) int limit) {

        // It's good practice to check if the principal is null, though Spring Security's
        // configuration should prevent unauthenticated access to this endpoint anyway.
        if (principal == null) {
            // This line is optional if your security config already protects the endpoint.
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        // Get the user's name directly from the Principal object.
        String userId = principal.getName();

        // The rest of your reactive chain works perfectly.
        // The service call is still reactive, and we build the response from its result.
        return messageService.pollMessages(userId, last_sync_id, limit)
                .map(messages -> {
                    Map<String, List<Message>> response = new HashMap<>();
                    response.put("messages", messages);
                    return ResponseEntity.ok(response);
                });
    }
}
