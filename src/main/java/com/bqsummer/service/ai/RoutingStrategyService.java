package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.common.vo.req.ai.StrategyCreateRequest;
import com.bqsummer.common.vo.req.ai.StrategyModelBindRequest;
import com.bqsummer.common.vo.resp.ai.StrategyModelBindingResponse;
import com.bqsummer.common.vo.resp.ai.StrategyResponse;
import com.bqsummer.exception.ModelValidationException;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.mapper.RoutingStrategyMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 璺敱绛栫暐鏈嶅姟瀹炵幇
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingStrategyService {

    private static final int MAX_TOTAL_WEIGHT = 100;

    private final RoutingStrategyMapper routingStrategyMapper;
    private final StrategyModelRelationMapper relationMapper;

    @Transactional(rollbackFor = Exception.class)
    public StrategyResponse createStrategy(StrategyCreateRequest request, Long userId) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetOtherDefaultStrategies();
        }

        RoutingStrategy strategy = new RoutingStrategy();
        strategy.setName(request.getName());
        strategy.setStrategyType(request.getStrategyType());
        strategy.setDescription(request.getDescription());
        strategy.setConfig(request.getConfig());
        strategy.setEnabled(request.getEnabled());
        strategy.setIsDefault(request.getIsDefault());
        strategy.setCreatedBy(userId);
        strategy.setUpdatedBy(userId);
        strategy.setCreatedAt(LocalDateTime.now());
        strategy.setUpdatedAt(LocalDateTime.now());

        routingStrategyMapper.insert(strategy);

        log.info("鍒涘缓璺敱绛栫暐: id={}, name={}, type={}",
                strategy.getId(), strategy.getName(), strategy.getStrategyType());

        return convertToResponse(strategy);
    }

    public List<StrategyResponse> listStrategies() {
        List<RoutingStrategy> strategies = routingStrategyMapper.selectList(null);
        return strategies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public StrategyResponse getStrategyById(Long id) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new RoutingException("绛栫暐涓嶅瓨锟? " + id);
        }
        return convertToResponse(strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public StrategyResponse updateStrategy(Long id, StrategyCreateRequest request, Long userId) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new RoutingException("绛栫暐涓嶅瓨锟? " + id);
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(strategy.getIsDefault())) {
            unsetOtherDefaultStrategies();
        }

        strategy.setName(request.getName());
        strategy.setStrategyType(request.getStrategyType());
        strategy.setDescription(request.getDescription());
        strategy.setConfig(request.getConfig());
        strategy.setEnabled(request.getEnabled());
        strategy.setIsDefault(request.getIsDefault());
        strategy.setUpdatedBy(userId);
        strategy.setUpdatedAt(LocalDateTime.now());

        routingStrategyMapper.updateById(strategy);

        log.info("鏇存柊璺敱绛栫暐: id={}, name={}", id, strategy.getName());

        return convertToResponse(strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteStrategy(Long id) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new RoutingException("绛栫暐涓嶅瓨锟? " + id);
        }

        if (Boolean.TRUE.equals(strategy.getIsDefault())) {
            throw new ModelValidationException("涓嶈兘鍒犻櫎榛樿绛栫暐");
        }

        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, id);
        relationMapper.delete(queryWrapper);

        routingStrategyMapper.deleteById(id);

        log.info("鍒犻櫎璺敱绛栫暐: id={}, name={}", id, strategy.getName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindModel(Long strategyId, StrategyModelBindRequest request) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RoutingException("绛栫暐涓嶅瓨锟? " + strategyId);
        }

        Long modelId = request.getModelId();
        Integer weight = request.getWeight();
        Integer priority = request.getPriority() != null ? request.getPriority() : 0;

        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId)
                .eq(StrategyModelRelation::getModelId, modelId)
                .last("LIMIT 1");

        StrategyModelRelation existing = relationMapper.selectOne(queryWrapper);
        validateTotalWeightNotExceed100(strategyId, existing == null ? null : modelId, weight);

        if (existing == null) {
            StrategyModelRelation relation = new StrategyModelRelation();
            relation.setStrategyId(strategyId);
            relation.setModelId(modelId);
            relation.setPriority(priority);
            relation.setWeight(weight);
            relation.setCreatedAt(LocalDateTime.now());
            relation.setUpdatedAt(LocalDateTime.now());
            relationMapper.insert(relation);
            log.info("缁戝畾妯″瀷鍒扮瓥锟? strategyId={}, modelId={}, weight={}, priority={}",
                    strategyId, modelId, weight, priority);
            return;
        }

        existing.setPriority(priority);
        existing.setWeight(weight);
        existing.setUpdatedAt(LocalDateTime.now());
        relationMapper.updateById(existing);
        log.info("鏇存柊绛栫暐妯″瀷缁戝畾: strategyId={}, modelId={}, weight={}, priority={}",
                strategyId, modelId, weight, priority);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindModel(Long strategyId, Long modelId) {
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId)
                .eq(StrategyModelRelation::getModelId, modelId);

        int deleted = relationMapper.delete(queryWrapper);
        if (deleted == 0) {
            throw new ModelValidationException("模型未绑定到该策略");
        }

        log.info("瑙ｇ粦妯″瀷: strategyId={}, modelId={}", strategyId, modelId);
    }

    public List<StrategyModelBindingResponse> getStrategyModels(Long strategyId) {
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId)
                .orderByDesc(StrategyModelRelation::getPriority)
                .orderByAsc(StrategyModelRelation::getCreatedAt);

        return relationMapper.selectList(queryWrapper).stream()
                .map(relation -> {
                    StrategyModelBindingResponse response = new StrategyModelBindingResponse();
                    response.setModelId(relation.getModelId());
                    response.setWeight(relation.getWeight());
                    response.setPriority(relation.getPriority());
                    response.setCreatedAt(relation.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void validateTotalWeightNotExceed100(Long strategyId, Long excludeModelId, Integer newWeight) {
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId);

        int existingWeightSum = relationMapper.selectList(queryWrapper).stream()
                .filter(relation -> excludeModelId == null || !excludeModelId.equals(relation.getModelId()))
                .map(StrategyModelRelation::getWeight)
                .filter(weight -> weight != null && weight > 0)
                .mapToInt(Integer::intValue)
                .sum();

        int nextTotal = existingWeightSum + newWeight;
        if (nextTotal > MAX_TOTAL_WEIGHT) {
            throw new ModelValidationException("鎵€鏈夋ā鍨嬫潈閲嶆€诲拰涓嶈兘瓒呰繃100锛屽綋鍓嶆彁浜ゅ悗锟? " + nextTotal);
        }
    }

    private void unsetOtherDefaultStrategies() {
        LambdaQueryWrapper<RoutingStrategy> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoutingStrategy::getIsDefault, true);

        List<RoutingStrategy> defaultStrategies = routingStrategyMapper.selectList(queryWrapper);
        for (RoutingStrategy strategy : defaultStrategies) {
            strategy.setIsDefault(false);
            strategy.setUpdatedAt(LocalDateTime.now());
            routingStrategyMapper.updateById(strategy);
        }
    }

    private StrategyResponse convertToResponse(RoutingStrategy strategy) {
        StrategyResponse response = new StrategyResponse();
        BeanUtils.copyProperties(strategy, response);
        return response;
    }
}


