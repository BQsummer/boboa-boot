package com.bqsummer.controller;

import com.bqsummer.common.vo.req.auth.SendMailRequest;
import com.bqsummer.service.notify.EmailTemplate;
import com.bqsummer.service.notify.MessageCenterService;
import com.bqsummer.service.notify.NotifyUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notify/email")
public class MailController {

    @Autowired
    private MessageCenterService messageCenterService;

    @PostMapping("/send")
    public void send(@Valid @RequestBody SendMailRequest request) {
        EmailTemplate template = EmailTemplate.builder()
                .title(request.getSubject())
                .emailBody(request.getBody())
                .build();

        List<NotifyUser> users = request.getTo().stream()
                .map(addr -> NotifyUser.builder().emailAddress(addr).build())
                .toList();

        messageCenterService.sendTemplate(users, List.of(template));
    }
}

