package com.bqsummer.service.prompt;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.dto.prompt.PostProcessPipeline;
import com.bqsummer.common.dto.prompt.PostProcessStep;
import com.bqsummer.common.dto.prompt.PromptTemplate;
import com.bqsummer.constant.PostProcessStepType;
import com.bqsummer.mapper.PostProcessPipelineMapper;
import com.bqsummer.mapper.PostProcessStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostProcessRuntimeService {

    private final PostProcessPipelineMapper pipelineMapper;
    private final PostProcessStepMapper stepMapper;

    public String process(String content, PromptTemplate template) {
        if (content == null) {
            return null;
        }
        long start = System.currentTimeMillis();
        String result = content;
        boolean applied = false;

        if (template != null && template.getPostProcessPipelineId() != null) {
            result = processByPipelineId(result, template.getPostProcessPipelineId());
            applied = true;
        } else if (template != null && template.getPostProcessConfig() != null) {
            result = processByLegacyConfig(result, template.getPostProcessConfig());
            applied = true;
        }

        if (applied) {
            log.info("post process applied: templateId={}, pipelineId={}, inLen={}, outLen={}, cost={}ms",
                    template.getId(),
                    template.getPostProcessPipelineId(),
                    content.length(),
                    result.length(),
                    System.currentTimeMillis() - start);
        }
        return result;
    }

    public String processByPipelineId(String content, Long pipelineId) {
        PostProcessPipeline pipeline = pipelineMapper.selectById(pipelineId);
        if (pipeline == null || Boolean.TRUE.equals(pipeline.getIsDeleted()) || pipeline.getStatus() != 1) {
            return content;
        }

        LambdaQueryWrapper<PostProcessStep> qw = new LambdaQueryWrapper<>();
        qw.eq(PostProcessStep::getPipelineId, pipelineId)
                .eq(PostProcessStep::getEnabled, true)
                .orderByAsc(PostProcessStep::getStepOrder)
                .orderByDesc(PostProcessStep::getPriority)
                .orderByAsc(PostProcessStep::getId);
        List<PostProcessStep> steps = stepMapper.selectList(qw);
        return applySteps(content, steps);
    }

    public String processByLegacyConfig(String content, Map<String, Object> config) {
        String result = content;
        Object removePatterns = config.get("removeTagPatterns");
        if (removePatterns instanceof List<?> patterns) {
            for (Object p : patterns) {
                if (p != null) {
                    result = result.replaceAll(String.valueOf(p), "");
                }
            }
        }

        Object replaceRules = config.get("replaceRules");
        if (replaceRules instanceof List<?> rules) {
            for (Object ruleObj : rules) {
                if (!(ruleObj instanceof Map<?, ?> rule)) {
                    continue;
                }
                String pattern = stringValue(rule.get("pattern"));
                String replacement = stringValue(rule.get("replacement"));
                if (StringUtils.hasText(pattern)) {
                    result = result.replaceAll(pattern, replacement == null ? "" : replacement);
                }
            }
        }

        if (Boolean.TRUE.equals(config.get("trimWhitespace"))) {
            result = result.trim();
        }

        Integer maxLength = intValue(config.get("maxLength"), null);
        if (maxLength != null && maxLength > 0 && result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;
    }

    private String applySteps(String content, List<PostProcessStep> steps) {
        String result = content;
        if (CollectionUtils.isEmpty(steps)) {
            return result;
        }

        for (PostProcessStep step : steps) {
            String before = result;
            try {
                result = applyStep(result, step);
            } catch (Exception e) {
                int onFail = step.getOnFail() == null ? 0 : step.getOnFail();
                log.warn("post process step failed: stepId={}, stepType={}, onFail={}, error={}",
                        step.getId(), step.getStepType(), onFail, e.getMessage());
                if (onFail == 1) {
                    break;
                }
                if (onFail == 2) {
                    result = content;
                    break;
                }
                result = before;
            }
        }
        return result;
    }

    private String applyStep(String content, PostProcessStep step) {
        if (content == null) {
            return null;
        }
        PostProcessStepType type = PostProcessStepType.fromCode(step.getStepType());
        if (type == null) {
            return content;
        }
        Map<String, Object> config = step.getConfig();
        return switch (type) {
            case REMOVE_TAG_BLOCK -> removeTagBlock(content, config);
            case REMOVE_FENCE_BLOCK -> removeFenceBlock(content, config);
            case REGEX_REPLACE -> regexReplace(content, config);
            case STRIP -> strip(content, config);
            case TRUNCATE -> truncate(content, config);
            case JSON_EXTRACT -> jsonExtract(content, config);
            case SAFETY_FILTER -> safetyFilter(content, config);
        };
    }

    private String removeTagBlock(String content, Map<String, Object> config) {
        String tag = stringValue(config.get("tag"));
        if (!StringUtils.hasText(tag)) {
            return content;
        }
        boolean caseInsensitive = boolValue(config.get("case_insensitive"), true);
        String mode = stringValue(config.get("mode"));
        String body = "greedy".equalsIgnoreCase(mode) ? "[\\s\\S]*" : "[\\s\\S]*?";
        String regex = "<\\s*" + Pattern.quote(tag) + "\\b[^>]*>" + body + "<\\s*/\\s*" + Pattern.quote(tag) + "\\s*>";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | (caseInsensitive ? Pattern.CASE_INSENSITIVE : 0));
        return pattern.matcher(content).replaceAll("");
    }

    private String removeFenceBlock(String content, Map<String, Object> config) {
        List<String> markers = stringList(config.get("lang_markers"));
        if (CollectionUtils.isEmpty(markers)) {
            markers = List.of("think", "analysis");
        }
        String result = content;
        for (String marker : markers) {
            String regex = "```\\s*" + Pattern.quote(marker) + "\\s*[\\r\\n]+[\\s\\S]*?```";
            result = result.replaceAll(regex, "");
        }
        return result;
    }

    private String regexReplace(String content, Map<String, Object> config) {
        String pattern = stringValue(config.get("pattern"));
        String replacement = stringValue(config.get("replacement"));
        if (!StringUtils.hasText(pattern)) {
            return content;
        }
        int flags = Pattern.DOTALL;
        if (boolValue(config.get("case_insensitive"), false)) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        Pattern compiled = Pattern.compile(pattern, flags);
        return compiled.matcher(content).replaceAll(replacement == null ? "" : replacement);
    }

    private String strip(String content, Map<String, Object> config) {
        String result = boolValue(config.get("trim"), true) ? content.trim() : content;
        if (boolValue(config.get("collapse_blank_lines"), true)) {
            int maxBlankLines = intValue(config.get("max_blank_lines"), 1);
            String regex = "[\\r\\n]{"+ (maxBlankLines + 2) +",}";
            String replacement = "\n".repeat(Math.max(2, maxBlankLines + 1));
            result = result.replaceAll(regex, replacement);
        }
        return result;
    }

    private String truncate(String content, Map<String, Object> config) {
        String result = content;
        String marker = stringValue(config.get("marker"));
        if (StringUtils.hasText(marker)) {
            int idx = result.indexOf(marker);
            if (idx >= 0) {
                result = result.substring(0, idx);
            }
        }
        Integer maxLength = intValue(config.get("max_length"), null);
        if (maxLength != null && maxLength > 0 && result.length() > maxLength) {
            result = result.substring(0, maxLength);
        }
        return result;
    }

    private String jsonExtract(String content, Map<String, Object> config) {
        JSONObject json = tryParseJsonObject(content);
        if (json == null) {
            return content;
        }
        List<String> fields = new ArrayList<>();
        String single = stringValue(config.get("field"));
        if (StringUtils.hasText(single)) {
            fields.add(single);
        }
        fields.addAll(stringList(config.get("field_paths")));
        if (fields.isEmpty()) {
            fields = List.of("content", "reply", "text");
        }
        for (String field : fields) {
            Object value = json.get(field);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return content;
    }

    private String safetyFilter(String content, Map<String, Object> config) {
        List<String> words = stringList(config.get("banned_words"));
        if (CollectionUtils.isEmpty(words)) {
            return content;
        }
        String replacement = stringValue(config.get("replace_with"));
        if (replacement == null) {
            replacement = "***";
        }
        String result = content;
        for (String word : words) {
            if (!StringUtils.hasText(word)) {
                continue;
            }
            Pattern p = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(result);
            result = m.replaceAll(replacement);
        }
        return result;
    }

    private JSONObject tryParseJsonObject(String content) {
        try {
            return JSON.parseObject(content);
        } catch (Exception ignored) {
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return JSON.parseObject(content.substring(start, end + 1));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean boolValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }
}
