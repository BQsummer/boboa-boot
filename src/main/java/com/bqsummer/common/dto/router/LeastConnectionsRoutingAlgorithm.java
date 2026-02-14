package com.bqsummer.common.dto.router;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最少连接路由算法
 * 选择当前活跃请求数最少的模型
 *
 */
@Slf4j
@Component
public class LeastConnectionsRoutingAlgorithm implements RoutingAlgorithm {
    
    // 模拟每个模型的活跃连接数
    private final Map<Long, AtomicInteger> connections = new ConcurrentHashMap<>();
    
    @Override
    public boolean supports(RoutingStrategy strategy) {
        return StrategyType.LEAST_CONNECTIONS.equals(strategy.getStrategyType());
    }
    
    @Override
    public AiModelBo select(RoutingStrategy strategy, List<AiModelBo> models, InferenceRequest request) {
        if (models.isEmpty()) {
            return null;
        }
        
        // 找到连接数最少的模型
        AiModelBo selected = models.get(0);
        int minConnections = getConnectionCount(selected.getId());
        
        for (AiModelBo model : models) {
            int count = getConnectionCount(model.getId());
            if (count < minConnections) {
                minConnections = count;
                selected = model;
            }
        }
        
        log.debug("最少连接路由选择: strategyId={}, selectedModelId={}, connections={}", 
                strategy.getId(), selected.getId(), minConnections);
        
        return selected;
    }
    
    @Override
    public String getName() {
        return "Least Connections";
    }
    
    /**
     * 获取模型的连接数
     */
    public int getConnectionCount(Long modelId) {
        return connections.computeIfAbsent(modelId, k -> new AtomicInteger(0)).get();
    }
    
    /**
     * 增加模型的连接数
     */
    public void incrementConnection(Long modelId) {
        connections.computeIfAbsent(modelId, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * 减少模型的连接数
     */
    public void decrementConnection(Long modelId) {
        AtomicInteger counter = connections.get(modelId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
}
