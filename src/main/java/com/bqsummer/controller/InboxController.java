package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.vo.Response;
import com.bqsummer.common.vo.req.inbox.InboxBatchOperateReq;
import com.bqsummer.common.vo.resp.inbox.UserInboxMessageResp;
import com.bqsummer.service.InboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    @GetMapping("/messages")
    public Response<Page<UserInboxMessageResp>> list(@RequestParam(required = false) Integer readStatus,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long size) {
        return Response.success(inboxService.userPage(readStatus, page, size));
    }

    @GetMapping("/unread-count")
    public Response<Map<String, Long>> unreadCount() {
        return Response.success(Map.of("unreadCount", inboxService.unreadCount()));
    }

    @PutMapping("/messages/{messageId}/read")
    public Response<Map<String, Integer>> markRead(@PathVariable("messageId") Long messageId) {
        return Response.success(Map.of("updatedCount", inboxService.markRead(messageId)));
    }

    @PutMapping("/messages/read-batch")
    public Response<Map<String, Integer>> markReadBatch(@Valid @RequestBody InboxBatchOperateReq req) {
        return Response.success(Map.of("updatedCount", inboxService.markReadBatch(req.getMessageIds())));
    }

    @DeleteMapping("/messages/{messageId}")
    public Response<Map<String, Integer>> delete(@PathVariable("messageId") Long messageId) {
        return Response.success(Map.of("updatedCount", inboxService.delete(messageId)));
    }

    @DeleteMapping("/messages/delete-batch")
    public Response<Map<String, Integer>> deleteBatch(@Valid @RequestBody InboxBatchOperateReq req) {
        return Response.success(Map.of("updatedCount", inboxService.deleteBatch(req.getMessageIds())));
    }
}
