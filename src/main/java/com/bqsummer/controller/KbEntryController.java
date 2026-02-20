package com.bqsummer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bqsummer.common.vo.req.kb.KbEntryCreateRequest;
import com.bqsummer.common.vo.req.kb.KbEntryQueryRequest;
import com.bqsummer.common.vo.req.kb.KbEntryUpdateRequest;
import com.bqsummer.common.vo.resp.kb.KbEntryResponse;
import com.bqsummer.service.prompt.KbEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kb-entries")
@RequiredArgsConstructor
public class KbEntryController {

    private final KbEntryService kbEntryService;

    @PostMapping
    public ResponseEntity<KbEntryResponse> create(@Valid @RequestBody KbEntryCreateRequest request) {
        return ResponseEntity.ok(kbEntryService.create(request));
    }

    @GetMapping
    public ResponseEntity<IPage<KbEntryResponse>> list(@Valid KbEntryQueryRequest request) {
        return ResponseEntity.ok(kbEntryService.list(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KbEntryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(kbEntryService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KbEntryResponse> update(@PathVariable Long id, @Valid @RequestBody KbEntryUpdateRequest request) {
        return ResponseEntity.ok(kbEntryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        kbEntryService.delete(id);
        return ResponseEntity.ok().build();
    }
}
