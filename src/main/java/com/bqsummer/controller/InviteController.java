package com.bqsummer.controller;

import com.bqsummer.plugin.invite.domain.InviteCode;
import com.bqsummer.plugin.invite.service.InviteService;
import com.bqsummer.plugin.invite.service.dto.*;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invite")
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;
    private final JwtUtil jwtUtil;

    @PostMapping("/codes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CreateInviteCodeResponse> create(@Valid @RequestBody CreateInviteCodeRequest request,
                                                           HttpServletRequest http) {
        Long userId = currentUserId(http);
        InviteCode ic = inviteService.createCode(userId, request);
        CreateInviteCodeResponse resp = CreateInviteCodeResponse.builder()
                .id(ic.getId())
                .code(ic.getCode())
                .maxUses(ic.getMaxUses())
                .usedCount(ic.getUsedCount())
                .status(ic.getStatus())
                .expireAt(ic.getExpireAt())
                .build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/codes/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CreateInviteCodeResponse>> myCodes(HttpServletRequest http) {
        Long userId = currentUserId(http);
        List<CreateInviteCodeResponse> list = inviteService.listMyCodes(userId).stream().map(ic ->
                CreateInviteCodeResponse.builder()
                        .id(ic.getId())
                        .code(ic.getCode())
                        .maxUses(ic.getMaxUses())
                        .usedCount(ic.getUsedCount())
                        .status(ic.getStatus())
                        .expireAt(ic.getExpireAt())
                        .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateInviteResponse> validate(@RequestParam("code") String code) {
        var vr = inviteService.validateCode(code);
        ValidateInviteResponse resp = ValidateInviteResponse.builder()
                .valid(vr.valid())
                .status(vr.status())
                .remainingUses(vr.remainingUses())
                .expireAt(vr.expireAt())
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RedeemInviteResponse> redeem(@Valid @RequestBody RedeemInviteRequest request,
                                                       HttpServletRequest http) {
        Long userId = currentUserId(http);
        String clientIp = extractClientIp(http);
        String ua = http.getHeader("User-Agent");
        var rr = inviteService.redeem(userId, request, clientIp, ua);
        RedeemInviteResponse resp = RedeemInviteResponse.builder()
                .success(rr.success())
                .remainingUses(rr.remainingUses())
                .build();
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/codes/{id}/revoke")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> revoke(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = currentUserId(http);
        inviteService.revoke(userId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MyInviteStatsResponse> myStats(HttpServletRequest http) {
        Long userId = currentUserId(http);
        List<InviteCode> codes = inviteService.listMyCodes(userId);
        int totalCodes = codes.size();
        int totalUses = codes.stream().mapToInt(c -> c.getUsedCount() == null ? 0 : c.getUsedCount()).sum();
        int totalRemaining = codes.stream().mapToInt(c -> Math.max(0, (c.getMaxUses() == null ? 0 : c.getMaxUses()) - (c.getUsedCount() == null ? 0 : c.getUsedCount()))).sum();
        MyInviteStatsResponse resp = MyInviteStatsResponse.builder()
                .totalCodes(totalCodes)
                .totalUses(totalUses)
                .totalRemaining(totalRemaining)
                .build();
        return ResponseEntity.ok(resp);
    }

    private Long currentUserId(HttpServletRequest request) {
        String token = JwtUtil.extractBearerToken(request.getHeader("Authorization"));
        return token != null ? jwtUtil.getUserIdFromToken(token) : null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int i = xff.indexOf(',');
            return (i > 0 ? xff.substring(0, i) : xff).trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}

