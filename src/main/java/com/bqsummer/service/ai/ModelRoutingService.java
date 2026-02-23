package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.common.dto.router.RoutingAlgorithm;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.RoutingStrategyMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import com.bqsummer.mapstruct.ai.AiModelStructMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final AiModelStructMapper aiModelStructMapper;

    public AiModelBo selectModel(Long strategyId, InferenceRequest request) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RoutingException("路由策略不存在: " + strategyId);
        }
        if (!strategy.getEnabled()) {
            throw new RoutingException("路由策略已禁用: " + strategyId);
        }

        List<AiModelBo> models = getModelsForStrategy(strategyId);
        if (models.isEmpty()) {
            throw new RoutingException("策略下没有可用的模型: " + strategyId);
        }

        RoutingAlgorithm algorithm = selectAlgorithm(strategy);
        AiModelBo selected = algorithm.select(strategy, models, request);
        if (selected == null) {
            throw new RoutingException("路由算法未返回有效模型: " + strategy.getName());
        }

        log.info("路由选择完成: strategyId={}, strategyType={}, selectedModelId={}",
                strategyId, strategy.getStrategyType(), selected.getId());
        return selected;
    }

    public AiModelBo selectModelByDefault(InferenceRequest request) {
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

    private List<AiModelBo> getModelsForStrategy(Long strategyId) {
        LambdaQueryWrapper<StrategyModelRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(StrategyModelRelation::getStrategyId, strategyId)
                .orderByDesc(StrategyModelRelation::getPriority);

        List<StrategyModelRelation> relations = relationMapper.selectList(relationQuery);
        if (relations.isEmpty()) {
            return List.of();
        }

        Map<Long, StrategyModelRelation> relationMap = relations.stream()
                .collect(Collectors.toMap(
                        StrategyModelRelation::getModelId,
                        relation -> relation,
                        (left, right) -> left));

        List<Long> modelIds = relations.stream()
                .map(StrategyModelRelation::getModelId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<AiModel> modelQuery = new LambdaQueryWrapper<>();
        modelQuery.in(AiModel::getId, modelIds)
                .eq(AiModel::getEnabled, true);

        List<AiModel> models = aiModelMapper.selectList(modelQuery);

        return models.stream()
                .map(model -> {
                    StrategyModelRelation relation = relationMap.get(model.getId());
                    AiModelBo enriched = aiModelStructMapper.toBo(model);
                    if (relation != null) {
                        enriched.setWeight(relation.getWeight());
                        enriched.setRoutingParams(relation.getModelParams());
                    }
                    return enriched;
                })
                .sorted(Comparator.comparing(
                        model -> relationMap.getOrDefault(model.getId(), new StrategyModelRelation()).getPriority(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

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
