package com.bqsummer.service.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.prompt.KbEntry;
import com.bqsummer.common.vo.req.kb.KbEntryCreateRequest;
import com.bqsummer.common.vo.req.kb.KbEntryQueryRequest;
import com.bqsummer.common.vo.req.kb.KbEntryUpdateRequest;
import com.bqsummer.common.vo.resp.kb.KbEntryResponse;
import com.bqsummer.mapper.KbEntryMapper;
import com.bqsummer.util.EmbeddingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KbEntryService {

    private static final Set<String> SUPPORTED_CONTEXT_SCOPES = Set.of("LAST_USER", "LAST_N");
    private static final Set<String> SUPPORTED_KEYWORD_MODES = Set.of("CONTAINS", "EXACT", "REGEX");
    private static final BigDecimal DEFAULT_VECTOR_THRESHOLD = new BigDecimal("0.800000");
    private static final BigDecimal DEFAULT_PROBABILITY = new BigDecimal("1.0000");

    private final KbEntryMapper kbEntryMapper;
    private final EmbeddingUtil embeddingUtil;

    @Transactional(rollbackFor = Exception.class)
    public KbEntryResponse create(KbEntryCreateRequest request) {
        validateRequest(request.getContextScope(), request.getKeywordMode());

        KbEntry entry = new KbEntry();
        entry.setTitle(request.getTitle());
        entry.setEnabled(defaultBoolean(request.getEnabled(), true));
        entry.setPriority(defaultInteger(request.getPriority(), 0));
        entry.setTemplate(request.getTemplate());
        entry.setParams(defaultParams(request.getParams()));
        entry.setContextScope(defaultContextScope(request.getContextScope()));
        entry.setLastN(defaultInteger(request.getLastN(), 1));
        entry.setAlwaysEnabled(defaultBoolean(request.getAlwaysEnabled(), false));
        entry.setKeywords(request.getKeywords());
        entry.setKeywordMode(defaultKeywordMode(request.getKeywordMode()));
        entry.setVectorEnabled(defaultBoolean(request.getVectorEnabled(), false));
        entry.setVectorThreshold(defaultBigDecimal(request.getVectorThreshold(), DEFAULT_VECTOR_THRESHOLD));
        entry.setVectorTopK(defaultInteger(request.getVectorTopK(), 5));
        entry.setProbability(defaultBigDecimal(request.getProbability(), DEFAULT_PROBABILITY));
        entry.setCreatedAt(OffsetDateTime.now());
        entry.setUpdatedAt(OffsetDateTime.now());

        refreshEmbeddingIfNeeded(entry, true);
        kbEntryMapper.insert(entry);

        return convert(entry);
    }

    public IPage<KbEntryResponse> list(KbEntryQueryRequest request) {
        LambdaQueryWrapper<KbEntry> wrapper = new LambdaQueryWrapper<>();
        if (request.getEnabled() != null) {
            wrapper.eq(KbEntry::getEnabled, request.getEnabled());
        }
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.like(KbEntry::getTitle, request.getTitle().trim());
        }
        wrapper.orderByDesc(KbEntry::getPriority)
                .orderByDesc(KbEntry::getUpdatedAt)
                .orderByDesc(KbEntry::getId);

        Page<KbEntry> page = new Page<>(request.getPage(), request.getPageSize());
        IPage<KbEntry> result = kbEntryMapper.selectPage(page, wrapper);
        return result.convert(this::convert);
    }

    public KbEntryResponse getById(Long id) {
        KbEntry entry = kbEntryMapper.selectById(id);
        if (entry == null) {
            throw new RuntimeException("kb entry not found, id: " + id);
        }
        return convert(entry);
    }

    @Transactional(rollbackFor = Exception.class)
    public KbEntryResponse update(Long id, KbEntryUpdateRequest request) {
        KbEntry entry = kbEntryMapper.selectById(id);
        if (entry == null) {
            throw new RuntimeException("kb entry not found, id: " + id);
        }

        String nextContextScope = request.getContextScope() == null ? entry.getContextScope() : request.getContextScope();
        String nextKeywordMode = request.getKeywordMode() == null ? entry.getKeywordMode() : request.getKeywordMode();
        validateRequest(nextContextScope, nextKeywordMode);

        boolean refreshEmbedding = false;

        if (request.getTitle() != null) {
            entry.setTitle(request.getTitle());
        }
        if (request.getEnabled() != null) {
            entry.setEnabled(request.getEnabled());
        }
        if (request.getPriority() != null) {
            entry.setPriority(request.getPriority());
        }
        if (request.getTemplate() != null) {
            entry.setTemplate(request.getTemplate());
            refreshEmbedding = true;
        }
        if (request.getParams() != null) {
            entry.setParams(request.getParams());
        }
        if (request.getContextScope() != null) {
            entry.setContextScope(defaultContextScope(request.getContextScope()));
        }
        if (request.getLastN() != null) {
            entry.setLastN(request.getLastN());
        }
        if (request.getAlwaysEnabled() != null) {
            entry.setAlwaysEnabled(request.getAlwaysEnabled());
        }
        if (request.getKeywords() != null) {
            entry.setKeywords(request.getKeywords());
        }
        if (request.getKeywordMode() != null) {
            entry.setKeywordMode(defaultKeywordMode(request.getKeywordMode()));
        }
        if (request.getVectorEnabled() != null) {
            refreshEmbedding = refreshEmbedding || (!Boolean.TRUE.equals(entry.getVectorEnabled()) && request.getVectorEnabled());
            entry.setVectorEnabled(request.getVectorEnabled());
        }
        if (request.getVectorThreshold() != null) {
            entry.setVectorThreshold(request.getVectorThreshold());
        }
        if (request.getVectorTopK() != null) {
            entry.setVectorTopK(request.getVectorTopK());
        }
        if (request.getProbability() != null) {
            entry.setProbability(request.getProbability());
        }

        entry.setUpdatedAt(OffsetDateTime.now());
        refreshEmbeddingIfNeeded(entry, refreshEmbedding);
        kbEntryMapper.updateById(entry);

        return convert(entry);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        kbEntryMapper.deleteById(id);
    }

    public List<KbEntry> listEnabledByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KbEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KbEntry::getId, ids)
                .eq(KbEntry::getEnabled, true)
                .orderByDesc(KbEntry::getPriority)
                .orderByDesc(KbEntry::getId);
        return kbEntryMapper.selectList(wrapper);
    }

    private void validateRequest(String contextScope, String keywordMode) {
        if (!SUPPORTED_CONTEXT_SCOPES.contains(defaultContextScope(contextScope))) {
            throw new IllegalArgumentException("unsupported contextScope: " + contextScope);
        }
        if (!SUPPORTED_KEYWORD_MODES.contains(defaultKeywordMode(keywordMode))) {
            throw new IllegalArgumentException("unsupported keywordMode: " + keywordMode);
        }
    }

    private void refreshEmbeddingIfNeeded(KbEntry entry, boolean forceRefresh) {
        if (!Boolean.TRUE.equals(entry.getVectorEnabled())) {
            entry.setEmbedding(null);
            return;
        }
        if (!forceRefresh && entry.getEmbedding() != null && entry.getEmbedding().length > 0) {
            return;
        }
        if (!StringUtils.hasText(entry.getTemplate())) {
            entry.setEmbedding(null);
            return;
        }

        long start = System.currentTimeMillis();
        entry.setEmbedding(embeddingUtil.generateEmbedding(entry.getTemplate()));
        log.info("kb entry embedding refreshed: id={}, cost={}ms", entry.getId(), System.currentTimeMillis() - start);
    }

    private String defaultContextScope(String contextScope) {
        return StringUtils.hasText(contextScope) ? contextScope.trim().toUpperCase(Locale.ROOT) : "LAST_USER";
    }

    private String defaultKeywordMode(String keywordMode) {
        return StringUtils.hasText(keywordMode) ? keywordMode.trim().toUpperCase(Locale.ROOT) : "CONTAINS";
    }

    private boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int defaultInteger(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Map<String, Object> defaultParams(Map<String, Object> params) {
        return params == null ? Collections.emptyMap() : params;
    }

    private KbEntryResponse convert(KbEntry entry) {
        KbEntryResponse response = new KbEntryResponse();
        response.setId(entry.getId());
        response.setTitle(entry.getTitle());
        response.setEnabled(entry.getEnabled());
        response.setPriority(entry.getPriority());
        response.setTemplate(entry.getTemplate());
        response.setParams(entry.getParams());
        response.setContextScope(entry.getContextScope());
        response.setLastN(entry.getLastN());
        response.setAlwaysEnabled(entry.getAlwaysEnabled());
        response.setKeywords(entry.getKeywords());
        response.setKeywordMode(entry.getKeywordMode());
        response.setVectorEnabled(entry.getVectorEnabled());
        response.setVectorThreshold(entry.getVectorThreshold());
        response.setVectorTopK(entry.getVectorTopK());
        response.setProbability(entry.getProbability());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());
        return response;
    }
}
