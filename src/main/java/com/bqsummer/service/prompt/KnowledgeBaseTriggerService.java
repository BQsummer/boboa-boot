package com.bqsummer.service.prompt;

import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.dto.prompt.KbEntry;
import com.bqsummer.common.dto.prompt.PromptTemplate;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.util.EmbeddingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseTriggerService {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.800000");
    private static final BigDecimal DEFAULT_PROBABILITY = new BigDecimal("1.0000");

    private final KbEntryService kbEntryService;
    private final MessageRepository messageRepository;
    private final EmbeddingUtil embeddingUtil;
    private final BeetlTemplateService beetlTemplateService;

    public List<TriggeredKnowledge> resolveTriggeredKnowledge(
            PromptTemplate template,
            Long userId,
            Long aiUserId,
            String latestUserMessage,
            Map<String, Object> runtimeParams) {

        if (template == null || template.getKbEntryIds() == null || template.getKbEntryIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<KbEntry> entries = kbEntryService.listEnabledByIds(template.getKbEntryIds());
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> contextCache = new HashMap<>();
        Map<String, float[]> embeddingCache = new HashMap<>();
        Map<Long, VectorMatch> vectorMatches = calculateVectorMatches(entries, userId, aiUserId, latestUserMessage, contextCache, embeddingCache);

        List<TriggeredKnowledge> hits = new ArrayList<>();
        for (KbEntry entry : entries) {
            String context = resolveContext(entry, userId, aiUserId, latestUserMessage, contextCache);

            boolean alwaysHit = Boolean.TRUE.equals(entry.getAlwaysEnabled());
            boolean keywordHit = isKeywordHit(entry, context);
            boolean vectorHit = isVectorHit(entry, vectorMatches.get(entry.getId()));
            if (!alwaysHit && !keywordHit && !vectorHit) {
                continue;
            }
            if (!passProbability(entry.getProbability())) {
                continue;
            }

            String rendered = renderEntry(entry, runtimeParams);
            if (!StringUtils.hasText(rendered)) {
                continue;
            }

            Set<String> triggerTypes = new LinkedHashSet<>();
            if (alwaysHit) {
                triggerTypes.add("ALWAYS");
            }
            if (keywordHit) {
                triggerTypes.add("KEYWORD");
            }
            if (vectorHit) {
                triggerTypes.add("VECTOR");
            }

            Float similarity = vectorMatches.containsKey(entry.getId()) ? vectorMatches.get(entry.getId()).similarity() : null;
            hits.add(new TriggeredKnowledge(
                    entry.getId(),
                    entry.getTitle(),
                    rendered,
                    entry.getPriority() == null ? 0 : entry.getPriority(),
                    similarity,
                    triggerTypes
            ));
        }

        hits.sort(Comparator.comparing(TriggeredKnowledge::priority).reversed()
                .thenComparing(TriggeredKnowledge::id, Comparator.reverseOrder()));
        return hits;
    }

    private Map<Long, VectorMatch> calculateVectorMatches(
            List<KbEntry> entries,
            Long userId,
            Long aiUserId,
            String latestUserMessage,
            Map<String, String> contextCache,
            Map<String, float[]> embeddingCache) {

        List<VectorCandidate> candidates = new ArrayList<>();
        for (KbEntry entry : entries) {
            if (!Boolean.TRUE.equals(entry.getVectorEnabled())) {
                continue;
            }
            if (entry.getEmbedding() == null || entry.getEmbedding().length == 0) {
                continue;
            }

            String context = resolveContext(entry, userId, aiUserId, latestUserMessage, contextCache);
            if (!StringUtils.hasText(context)) {
                continue;
            }

            float[] queryEmbedding = embeddingCache.computeIfAbsent(context, embeddingUtil::generateEmbedding);
            float similarity = calculateCosineSimilarity(entry.getEmbedding(), queryEmbedding);
            candidates.add(new VectorCandidate(entry, similarity));
        }

        candidates.sort(Comparator.comparing(VectorCandidate::similarity).reversed());

        Map<Long, VectorMatch> matches = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            VectorCandidate candidate = candidates.get(i);
            matches.put(
                    candidate.entry().getId(),
                    new VectorMatch(candidate.similarity(), i + 1)
            );
        }
        return matches;
    }

    private boolean isVectorHit(KbEntry entry, VectorMatch match) {
        if (!Boolean.TRUE.equals(entry.getVectorEnabled()) || match == null) {
            return false;
        }
        BigDecimal threshold = entry.getVectorThreshold() == null ? DEFAULT_THRESHOLD : entry.getVectorThreshold();
        int topK = entry.getVectorTopK() == null || entry.getVectorTopK() <= 0 ? 5 : entry.getVectorTopK();
        return BigDecimal.valueOf(match.similarity()).compareTo(threshold) >= 0 && match.rank() <= topK;
    }

    private boolean isKeywordHit(KbEntry entry, String context) {
        if (!StringUtils.hasText(entry.getKeywords()) || !StringUtils.hasText(context)) {
            return false;
        }

        String mode = entry.getKeywordMode();
        if (!StringUtils.hasText(mode)) {
            mode = "CONTAINS";
        }

        String[] parts = entry.getKeywords().split("[|,\\n]");
        String source = context.trim();
        for (String part : parts) {
            String keyword = part == null ? null : part.trim();
            if (!StringUtils.hasText(keyword)) {
                continue;
            }

            if ("EXACT".equalsIgnoreCase(mode)) {
                if (source.equals(keyword)) {
                    return true;
                }
                continue;
            }
            if ("REGEX".equalsIgnoreCase(mode)) {
                try {
                    if (Pattern.compile(keyword).matcher(source).find()) {
                        return true;
                    }
                } catch (PatternSyntaxException e) {
                    log.warn("invalid kb regex keyword, entryId={}, keyword={}, error={}",
                            entry.getId(), keyword, e.getMessage());
                }
                continue;
            }

            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String resolveContext(
            KbEntry entry,
            Long userId,
            Long aiUserId,
            String latestUserMessage,
            Map<String, String> contextCache) {

        String scope = StringUtils.hasText(entry.getContextScope()) ? entry.getContextScope() : "LAST_USER";
        int lastN = entry.getLastN() == null || entry.getLastN() <= 0 ? 1 : entry.getLastN();
        String cacheKey = scope + ":" + lastN;
        if (contextCache.containsKey(cacheKey)) {
            return contextCache.get(cacheKey);
        }

        String context;
        if ("LAST_N".equalsIgnoreCase(scope)) {
            List<Message> messages = messageRepository.findDialogHistoryForPrompt(userId, aiUserId, null, lastN);
            if (messages.isEmpty()) {
                context = defaultLatestMessage(latestUserMessage);
            } else {
                Collections.reverse(messages);
                List<String> lines = new ArrayList<>();
                for (Message message : messages) {
                    if (!StringUtils.hasText(message.getContent())) {
                        continue;
                    }
                    String role = Objects.equals(message.getSenderId(), userId) ? "USER" : "CHAR";
                    lines.add(role + ": " + message.getContent());
                }
                context = String.join("\n", lines);
            }
        } else {
            context = defaultLatestMessage(latestUserMessage);
        }

        contextCache.put(cacheKey, context);
        return context;
    }

    private String defaultLatestMessage(String latestUserMessage) {
        return latestUserMessage == null ? "" : latestUserMessage;
    }

    private boolean passProbability(BigDecimal probability) {
        BigDecimal value = probability == null ? DEFAULT_PROBABILITY : probability;
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (value.compareTo(BigDecimal.ONE) >= 0) {
            return true;
        }
        return ThreadLocalRandom.current().nextDouble() < value.doubleValue();
    }

    private String renderEntry(KbEntry entry, Map<String, Object> runtimeParams) {
        Map<String, Object> params = new HashMap<>();
        if (entry.getParams() != null) {
            params.putAll(entry.getParams());
        }
        if (runtimeParams != null) {
            params.putAll(runtimeParams);
        }

        try {
            return beetlTemplateService.render(entry.getTemplate(), params);
        } catch (Exception e) {
            log.warn("render kb entry failed, fallback raw template: entryId={}, error={}", entry.getId(), e.getMessage());
            return entry.getTemplate();
        }
    }

    private float calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length == 0 || vec2.length == 0 || vec1.length != vec2.length) {
            return 0F;
        }

        float dot = 0F;
        float norm1 = 0F;
        float norm2 = 0F;
        for (int i = 0; i < vec1.length; i++) {
            dot += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        if (norm1 <= 0 || norm2 <= 0) {
            return 0F;
        }
        return (float) (dot / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    private record VectorCandidate(KbEntry entry, float similarity) {
    }

    private record VectorMatch(float similarity, int rank) {
    }

    public record TriggeredKnowledge(
            Long id,
            String title,
            String content,
            Integer priority,
            Float similarity,
            Set<String> triggerTypes) {
    }
}
