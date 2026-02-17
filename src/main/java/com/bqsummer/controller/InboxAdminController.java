package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.vo.Response;
import com.bqsummer.common.vo.req.inbox.AdminCreateInboxMessageReq;
import com.bqsummer.common.vo.req.inbox.AdminUpdateInboxMessageReq;
import com.bqsummer.common.vo.resp.inbox.AdminInboxMessageResp;
import com.bqsummer.common.vo.resp.inbox.InboxRecipientResp;
import com.bqsummer.common.vo.resp.inbox.InboxStatsResp;
import com.bqsummer.service.InboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inbox")
@RequiredArgsConstructor
public class InboxAdminController {

    private final InboxService inboxService;

    @GetMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Page<AdminInboxMessageResp>> list(@RequestParam(required = false) Integer msgType,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String bizType,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size) {
        return Response.success(inboxService.adminPage(msgType, keyword, bizType, page, size));
    }

    @GetMapping("/messages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<AdminInboxMessageResp> detail(@PathVariable("id") Long id) {
        return Response.success(inboxService.detail(id));
    }

    @PostMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<AdminInboxMessageResp> create(@Valid @RequestBody AdminCreateInboxMessageReq req) {
        return Response.success(inboxService.create(req));
    }

    @PutMapping("/messages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<AdminInboxMessageResp> update(@PathVariable("id") Long id,
                                                  @Valid @RequestBody AdminUpdateInboxMessageReq req) {
        return Response.success(inboxService.update(id, req));
    }

    @GetMapping("/messages/{id}/recipients")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<Page<InboxRecipientResp>> recipients(@PathVariable("id") Long id,
                                                         @RequestParam(required = false) Integer readStatus,
                                                         @RequestParam(required = false) Integer deleteStatus,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long size) {
        return Response.success(inboxService.recipients(id, readStatus, deleteStatus, page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<InboxStatsResp> stats() {
        return Response.success(inboxService.stats());
    }
}
