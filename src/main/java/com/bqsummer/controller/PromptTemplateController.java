package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.prompt.PromptTemplateCreateRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateQueryRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateRenderRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateUpdateRequest;
import com.bqsummer.common.vo.resp.prompt.PromptTemplateResponse;
import com.bqsummer.service.prompt.PromptTemplateService;
import com.bqsummer.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Prompt 模板控制器
 *
 * 提供 Prompt 模板的增删改查和渲染预览功能。
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;
    private final JwtUtil jwtUtil;

    /**
     * 创建 Prompt 模板
     *
     * @param request 创建请求
     * @return 创建后的模板信息
     */
    @PostMapping
    public ResponseEntity<PromptTemplateResponse> create(@Valid @RequestBody PromptTemplateCreateRequest request, HttpServletRequest http) {
        Long currentUserId = jwtUtil.getUserIdFromRequest(http);
        PromptTemplateResponse response = promptTemplateService.create(request, currentUserId);
        log.info("创建 Prompt 模板成功，id: {}", response.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * 分页查询模板列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<IPage<PromptTemplateResponse>> list(@Valid PromptTemplateQueryRequest request) {
        IPage<PromptTemplateResponse> result = promptTemplateService.list(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID查询模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplateResponse> getById(@PathVariable Long id) {
        PromptTemplateResponse response = promptTemplateService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新模板
     *
     * @param id 模板ID
     * @param request 更新请求
     * @return 更新后的模板信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<PromptTemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PromptTemplateUpdateRequest request,
            HttpServletRequest http) {
        Long currentUserId = jwtUtil.getUserIdFromRequest(http);
        PromptTemplateResponse response = promptTemplateService.update(id, request, currentUserId);
        log.info("更新 Prompt 模板成功，id: {}", id);

        return ResponseEntity.ok(response);
    }

    /**
     * 删除模板（逻辑删除）
     *
     * @param id 模板ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest http) {
        Long currentUserId = jwtUtil.getUserIdFromRequest(http);

        promptTemplateService.delete(id, currentUserId);
        log.info("删除 Prompt 模板成功，id: {}", id);

        return ResponseEntity.ok().build();
    }

    /**
     * 渲染模板预览
     *
     * @param id 模板ID
     * @param request 渲染请求（包含参数）
     * @return 渲染后的内容
     */
    @PostMapping("/{id}/render")
    public ResponseEntity<String> render(
            @PathVariable Long id,
            @Valid @RequestBody PromptTemplateRenderRequest request) {
        String result = promptTemplateService.render(id, request.getParams());
        return ResponseEntity.ok(result);
    }
}
