package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.feedback.Feedback;
import com.bqsummer.common.vo.req.feedback.SubmitFeedbackRequest;
import com.bqsummer.common.vo.resp.feedback.SubmitFeedbackResponse;
import com.bqsummer.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static com.bqsummer.util.IPUtil.extractClientIp;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/submit")
    public ResponseEntity<SubmitFeedbackResponse> submit(@Valid @RequestBody SubmitFeedbackRequest request,
                                                         HttpServletRequest http) {

        request.setUserId((Long) SecurityContextHolder.getContext().getAuthentication().getDetails());
        String clientIp = extractClientIp(http);
        String ua = http.getHeader("User-Agent");
        Long id = feedbackService.submit(request, clientIp, ua);
        return ResponseEntity.ok(SubmitFeedbackResponse.builder().id(id).build());
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Feedback>> myFeedbacks(@RequestParam(required = false) String type,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return ResponseEntity.ok(feedbackService.list(type, status, userId, page, size));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<Feedback> myFeedbackDetail(@PathVariable Long id) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getDetails();
        Feedback fb = feedbackService.getUserFeedback(id, userId);
        if (fb == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(fb);
    }
}
