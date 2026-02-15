package com.bqsummer.controller;

import com.bqsummer.common.vo.Response;
import com.bqsummer.common.vo.req.config.SaveIpBlacklistReq;
import com.bqsummer.service.IpBlacklistConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/ip-blacklist")
@RequiredArgsConstructor
public class IpBlacklistController {

    private final IpBlacklistConfigService ipBlacklistConfigService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Response<String> getIpBlacklist() {
        return Response.success(ipBlacklistConfigService.getIpBlacklistValue());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public Response<Void> saveIpBlacklist(@RequestBody SaveIpBlacklistReq req) {
        ipBlacklistConfigService.saveIpBlacklistValue(req == null ? null : req.getValue());
        return Response.success();
    }
}
