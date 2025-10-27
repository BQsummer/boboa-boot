package com.bqsummer.common.dto.router;

import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 权重路由算法
 * 根据模型的 weight 字段进行加权随机选择
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Component
public class WeightedRoutingAlgorithm implements RoutingAlgorithm {
    
    private final Random random = new Random();
    
    @Override
    public boolean supports(RoutingStrategy strategy) {
        return StrategyType.WEIGHTED.equals(strategy.getStrategyType());
    }
    
    @Override
    public AiModel select(RoutingStrategy strategy, List<AiModel> models, InferenceRequest request) {
        if (models.isEmpty()) {
            return null;
        }
        
        // 计算总权重
        int totalWeight = models.stream()
                .mapToInt(AiModel::getWeight)
                .sum();
        
        if (totalWeight <= 0) {
            // 权重无效，随机选择
            return models.get(random.nextInt(models.size()));
        }
        
        // 加权随机选择
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (AiModel model : models) {
            currentWeight += model.getWeight();
            if (randomWeight < currentWeight) {
                log.debug("权重路由选择: strategyId={}, selectedModelId={}, weight={}/{}", 
                        strategy.getId(), model.getId(), model.getWeight(), totalWeight);
                return model;
            }
        }
        
        // 兜底：返回最后一个模型
        return models.get(models.size() - 1);
    }
    
    @Override
    public String getName() {
        return "Weighted Random";
    }
}
