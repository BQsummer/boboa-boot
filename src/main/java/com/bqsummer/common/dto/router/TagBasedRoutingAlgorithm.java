package com.bqsummer.common.dto.router;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签路由算法
 * 根据策略配置的标签要求过滤模型
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
public class TagBasedRoutingAlgorithm implements RoutingAlgorithm {
    
    @Override
    public boolean supports(RoutingStrategy strategy) {
        return StrategyType.TAG_BASED.equals(strategy.getStrategyType());
    }
    
    @Override
    public AiModel select(RoutingStrategy strategy, List<AiModel> models, InferenceRequest request) {
        if (models.isEmpty()) {
            return null;
        }
        
        // 解析配置中的必需标签
        List<String> requiredTags = parseRequiredTags(strategy.getConfig());
        
        if (requiredTags.isEmpty()) {
            // 没有标签要求，返回第一个
            return models.get(0);
        }
        
        // 过滤出包含所有必需标签的模型
        List<AiModel> matchedModels = models.stream()
                .filter(model -> model.getTags() != null && model.getTags().containsAll(requiredTags))
                .collect(Collectors.toList());
        
        if (matchedModels.isEmpty()) {
            log.warn("标签路由未找到匹配模型: strategyId={}, requiredTags={}", 
                    strategy.getId(), requiredTags);
            // 没有匹配的模型，返回第一个
            return models.get(0);
        }
        
        AiModel selected = matchedModels.get(0);
        log.debug("标签路由选择: strategyId={}, selectedModelId={}, tags={}", 
                strategy.getId(), selected.getId(), selected.getTags());
        
        return selected;
    }
    
    @Override
    public String getName() {
        return "Tag Based";
    }
    
    private List<String> parseRequiredTags(String config) {
        try {
            if (config == null || config.isEmpty()) {
                return List.of();
            }
            JSONObject json = JSON.parseObject(config);
            return json.getList("requiredTags", String.class);
        } catch (Exception e) {
            log.error("解析标签配置失败: config={}, error={}", config, e.getMessage());
            return List.of();
        }
    }
}
