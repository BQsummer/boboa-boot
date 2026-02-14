package com.bqsummer.common.dto.router;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 优先级路由算法
 * 从 StrategyModelRelation 的 priority 字段选择最高优先级的模型
 *
 */
@Slf4j
@Component
public class PriorityRoutingAlgorithm implements RoutingAlgorithm {
    
    @Override
    public boolean supports(RoutingStrategy strategy) {
        return StrategyType.PRIORITY.equals(strategy.getStrategyType());
    }
    
    @Override
    public AiModelBo select(RoutingStrategy strategy, List<AiModelBo> models, InferenceRequest request) {
        if (models.isEmpty()) {
            return null;
        }
        
        // 注意：models 列表应该已经按 priority 排序（由 ModelRoutingService 提供）
        // 选择第一个（最高优先级）
        AiModelBo selected = models.get(0);
        
        log.debug("优先级路由选择: strategyId={}, selectedModelId={}", 
                strategy.getId(), selected.getId());
        
        return selected;
    }
    
    @Override
    public String getName() {
        return "Priority Based";
    }
}
