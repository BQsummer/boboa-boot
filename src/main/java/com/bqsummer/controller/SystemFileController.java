package com.bqsummer.controller;

import com.bqsummer.common.vo.Response;
import com.bqsummer.common.vo.req.system.UpdateSystemFileReq;
import com.bqsummer.common.vo.resp.system.SystemFileResp;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.service.SystemFileService;
import com.bqsummer.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system/files")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SystemFileController {

    private final SystemFileService systemFileService;
    private final JwtUtil jwtUtil;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<SystemFileResp> upload(@RequestPart("file") MultipartFile file,
                                           @RequestHeader("Authorization") String authHeader,
                                           @RequestParam(required = false) String category) {
        String key = systemFileService.upload(currentUserId(authHeader), file, category);
        return Response.success(toResp(key));
    }

    @GetMapping
    public Response<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String category) {
        Map<String, Object> result = systemFileService.list(page, pageSize, keyword, category);
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) result.getOrDefault("keys", List.of());
        Map<String, Object> data = new HashMap<>(result);
        data.put("list", keys.stream().map(this::toResp).collect(Collectors.toList()));
        data.remove("keys");
        return Response.success(data);
    }

    @GetMapping("/detail")
    public Response<SystemFileResp> detail(@RequestParam String key) {
        return Response.success(toResp(key));
    }

    @PutMapping("/rename")
    public Response<SystemFileResp> update(@RequestHeader("Authorization") String authHeader,
                                           @RequestBody UpdateSystemFileReq req) {
        currentUserId(authHeader);
        String newKey = systemFileService.rename(req.getKey(), req.getFileName(), req.getCategory());
        return Response.success(toResp(newKey));
    }

    @DeleteMapping
    public Response<Void> delete(@RequestParam String key, @RequestHeader("Authorization") String authHeader) {
        currentUserId(authHeader);
        systemFileService.delete(key);
        return Response.success();
    }

    private Long currentUserId(String authHeader) {
        String token = JwtUtil.extractBearerToken(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new SnorlaxClientException(401, "unauthorized");
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new SnorlaxClientException(401, "unauthorized");
        }
        return userId;
    }

    private SystemFileResp toResp(String key) {
        if (key == null || key.isBlank()) {
            throw new SnorlaxClientException(400, "key is required");
        }
        SystemFileResp resp = new SystemFileResp();
        resp.setFileKey(key);
        int idx = key.lastIndexOf('/');
        resp.setFileName(idx >= 0 ? key.substring(idx + 1) : key);
        resp.setCategory(systemFileService.extractCategory(key));
        resp.setStorageType(systemFileService.resolveStorageType());
        resp.setSizeBytes(systemFileService.resolveSize(key));
        URL accessUrl = systemFileService.resolveAccessUrl(key);
        resp.setAccessUrl(accessUrl == null ? null : accessUrl.toString());
        return resp;
    }
}
