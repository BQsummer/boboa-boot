package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.RoutingStrategyMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import com.bqsummer.common.dto.router.RoutingAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型路由服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRoutingService {
    
    private final RoutingStrategyMapper routingStrategyMapper;
    private final StrategyModelRelationMapper relationMapper;
    private final AiModelMapper aiModelMapper;
    private final List<RoutingAlgorithm> algorithms;
    
    public AiModel selectModel(Long strategyId, InferenceRequest request) {
        // 1. 获取策略
        RoutingStrategy strategy = routingStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RoutingException("路由策略不存在: " + strategyId);
        }
        
        if (!strategy.getEnabled()) {
            throw new RoutingException("路由策略已禁用: " + strategyId);
        }
        
        // 2. 获取策略关联的模型
        List<AiModel> models = getModelsForStrategy(strategyId);
        
        if (models.isEmpty()) {
            throw new RoutingException("策略下没有可用的模型: " + strategyId);
        }
        
        // 3. 选择路由算法
        RoutingAlgorithm algorithm = selectAlgorithm(strategy);
        
        // 4. 执行路由选择
        AiModel selected = algorithm.select(strategy, models, request);
        
        if (selected == null) {
            throw new RoutingException("路由算法未返回有效模型: " + strategy.getName());
        }
        
        log.info("路由选择完成: strategyId={}, strategyType={}, selectedModelId={}", 
                strategyId, strategy.getStrategyType(), selected.getId());
        
        return selected;
    }
    
    public AiModel selectModelByDefault(InferenceRequest request) {
        // 获取默认策略
        LambdaQueryWrapper<RoutingStrategy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoutingStrategy::getIsDefault, true)
                    .eq(RoutingStrategy::getEnabled, true)
                    .last("LIMIT 1");
        
        RoutingStrategy defaultStrategy = routingStrategyMapper.selectOne(queryWrapper);
        
        if (defaultStrategy == null) {
            throw new RoutingException("没有可用的默认路由策略");
        }
        
        return selectModel(defaultStrategy.getId(), request);
    }
    
    public RoutingStrategy getStrategy(Long strategyId) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RoutingException("路由策略不存在: " + strategyId);
        }
        return strategy;
    }
    
    /**
     * 获取策略关联的所有可用模型
     */
    private List<AiModel> getModelsForStrategy(Long strategyId) {
        // 1. 查询策略-模型关联关系
        LambdaQueryWrapper<StrategyModelRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(StrategyModelRelation::getStrategyId, strategyId)
                    .orderByDesc(StrategyModelRelation::getPriority);
        
        List<StrategyModelRelation> relations = relationMapper.selectList(relationQuery);
        
        if (relations.isEmpty()) {
            return List.of();
        }
        
        // 2. 获取模型ID列表
        List<Long> modelIds = relations.stream()
                .map(StrategyModelRelation::getModelId)
                .collect(Collectors.toList());
        
        // 3. 查询模型详情（只查询启用的模型）
        LambdaQueryWrapper<AiModel> modelQuery = new LambdaQueryWrapper<>();
        modelQuery.in(AiModel::getId, modelIds)
                 .eq(AiModel::getEnabled, true);
        
        List<AiModel> models = aiModelMapper.selectList(modelQuery);
        
        // 4. 按照 priority 排序（根据 relation 的顺序）
        return models.stream()
                .sorted(Comparator.comparing(
                        model -> relations.stream()
                                .filter(r -> r.getModelId().equals(model.getId()))
                                .findFirst()
                                .map(StrategyModelRelation::getPriority)
                                .orElse(0),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }
    
    /**
     * 选择路由算法
     */
    private RoutingAlgorithm selectAlgorithm(RoutingStrategy strategy) {
        for (RoutingAlgorithm algorithm : algorithms) {
            if (algorithm.supports(strategy)) {
                log.debug("选择路由算法: algorithm={}, strategyId={}", 
                        algorithm.getName(), strategy.getId());
                return algorithm;
            }
        }
        
        throw new RoutingException("没有可用的路由算法支持策略类型: " + strategy.getStrategyType());
    }
}
