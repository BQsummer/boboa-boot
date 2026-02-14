package com.bqsummer.common.dto.router;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.exception.RoutingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 权重路由算法
 */
@Slf4j
@Component
public class WeightedRoutingAlgorithm implements RoutingAlgorithm {

    private static final int REQUIRED_TOTAL_WEIGHT = 100;
    private final Random random = new Random();

    @Override
    public boolean supports(RoutingStrategy strategy) {
        return StrategyType.WEIGHTED.equals(strategy.getStrategyType());
    }

    @Override
    public AiModelBo select(RoutingStrategy strategy, List<AiModelBo> models, InferenceRequest request) {
        if (models.isEmpty()) {
            return null;
        }

        for (AiModelBo model : models) {
            Integer weight = model.getWeight();
            if (weight == null || weight < 1 || weight > 100) {
                throw new RoutingException("模型权重必须在1到100之间: modelId=" + model.getId() + "，权重：" + weight);
            }
        }

        int totalWeight = models.stream()
                .mapToInt(AiModelBo::getWeight)
                .sum();
        if (totalWeight != REQUIRED_TOTAL_WEIGHT) {
            throw new RoutingException("加权策略模型权重总和必须为100，当前为: " + totalWeight);
        }

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (AiModelBo model : models) {
            currentWeight += model.getWeight();
            if (randomWeight < currentWeight) {
                log.debug("权重路由选择: strategyId={}, selectedModelId={}, weight={}/{}",
                        strategy.getId(), model.getId(), model.getWeight(), totalWeight);
                return model;
            }
        }

        return models.get(models.size() - 1);
    }

    @Override
    public String getName() {
        return "Weighted Random";
    }
}
