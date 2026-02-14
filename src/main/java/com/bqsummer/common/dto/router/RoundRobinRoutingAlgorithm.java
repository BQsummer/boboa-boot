package com.bqsummer.common.dto.router;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询路由算法
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
public class RoundRobinRoutingAlgorithm implements RoutingAlgorithm {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public boolean supports(RoutingStrategy strategy) {
        return StrategyType.ROUND_ROBIN.equals(strategy.getStrategyType());
    }
    
    @Override
    public AiModelBo select(RoutingStrategy strategy, List<AiModelBo> models, InferenceRequest request) {
        if (models.isEmpty()) {
            return null;
        }
        
        int index = Math.abs(counter.getAndIncrement() % models.size());
        AiModelBo selected = models.get(index);
        
        log.debug("轮询路由选择: strategyId={}, selectedModelId={}, index={}/{}", 
                strategy.getId(), selected.getId(), index, models.size());
        
        return selected;
    }
    
    @Override
    public String getName() {
        return "Round Robin";
    }
}
