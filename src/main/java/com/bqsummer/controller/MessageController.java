package com.bqsummer.controller;

import com.bqsummer.common.dto.Message;
import com.bqsummer.common.vo.req.SendMessageRequest;
import com.bqsummer.service.im.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;
    @PostMapping
    public void sendMessage(@Valid @RequestBody SendMessageRequest request) {

        messageService.sendMessage(request);
    }
}
