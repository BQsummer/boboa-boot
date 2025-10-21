package com.bqsummer.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.model.dto.StrategyCreateRequest;
import com.bqsummer.model.dto.StrategyModelBindRequest;
import com.bqsummer.model.dto.StrategyResponse;
import com.bqsummer.model.entity.RoutingStrategy;
import com.bqsummer.model.entity.StrategyModelRelation;
import com.bqsummer.model.exception.ModelValidationException;
import com.bqsummer.model.exception.RoutingException;
import com.bqsummer.model.mapper.RoutingStrategyMapper;
import com.bqsummer.model.mapper.StrategyModelRelationMapper;
import com.bqsummer.model.service.RoutingStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 路由策略服务实现
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingStrategyServiceImpl implements RoutingStrategyService {
    
    private final RoutingStrategyMapper routingStrategyMapper;
    private final StrategyModelRelationMapper relationMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyResponse createStrategy(StrategyCreateRequest request, Long userId) {
        // 如果设置为默认策略，先取消其他默认策略
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
        
        log.info("创建路由策略: id={}, name={}, type={}", 
                strategy.getId(), strategy.getName(), strategy.getStrategyType());
        
        return convertToResponse(strategy);
    }
    
    @Override
    public List<StrategyResponse> listStrategies() {
        List<RoutingStrategy> strategies = routingStrategyMapper.selectList(null);
        return strategies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public StrategyResponse getStrategyById(Long id) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new RoutingException("策略不存在: " + id);
        }
        return convertToResponse(strategy);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyResponse updateStrategy(Long id, StrategyCreateRequest request, Long userId) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new RoutingException("策略不存在: " + id);
        }
        
        // 如果设置为默认策略，先取消其他默认策略
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
        
        log.info("更新路由策略: id={}, name={}", id, strategy.getName());
        
        return convertToResponse(strategy);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStrategy(Long id) {
        RoutingStrategy strategy = routingStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new RoutingException("策略不存在: " + id);
        }
        
        if (Boolean.TRUE.equals(strategy.getIsDefault())) {
            throw new ModelValidationException("不能删除默认策略");
        }
        
        // 删除策略-模型关联
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, id);
        relationMapper.delete(queryWrapper);
        
        // 删除策略
        routingStrategyMapper.deleteById(id);
        
        log.info("删除路由策略: id={}, name={}", id, strategy.getName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindModel(Long strategyId, StrategyModelBindRequest request) {
        // 检查策略是否存在
        RoutingStrategy strategy = routingStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new RoutingException("策略不存在: " + strategyId);
        }
        
        // 检查是否已经绑定
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId)
                   .eq(StrategyModelRelation::getModelId, request.getModelId());
        
        if (relationMapper.selectCount(queryWrapper) > 0) {
            throw new ModelValidationException("模型已经绑定到该策略");
        }
        
        // 创建关联
        StrategyModelRelation relation = new StrategyModelRelation();
        relation.setStrategyId(strategyId);
        relation.setModelId(request.getModelId());
        relation.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        relation.setCreatedAt(LocalDateTime.now());
        
        relationMapper.insert(relation);
        
        log.info("绑定模型到策略: strategyId={}, modelId={}, priority={}", 
                strategyId, request.getModelId(), relation.getPriority());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindModel(Long strategyId, Long modelId) {
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId)
                   .eq(StrategyModelRelation::getModelId, modelId);
        
        int deleted = relationMapper.delete(queryWrapper);
        
        if (deleted == 0) {
            throw new ModelValidationException("模型未绑定到该策略");
        }
        
        log.info("解绑模型: strategyId={}, modelId={}", strategyId, modelId);
    }
    
    @Override
    public List<Long> getStrategyModels(Long strategyId) {
        LambdaQueryWrapper<StrategyModelRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StrategyModelRelation::getStrategyId, strategyId)
                   .orderByDesc(StrategyModelRelation::getPriority);
        
        return relationMapper.selectList(queryWrapper).stream()
                .map(StrategyModelRelation::getModelId)
                .collect(Collectors.toList());
    }
    
    /**
     * 取消其他策略的默认状态
     */
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
    
    /**
     * 转换为响应对象
     */
    private StrategyResponse convertToResponse(RoutingStrategy strategy) {
        StrategyResponse response = new StrategyResponse();
        BeanUtils.copyProperties(strategy, response);
        return response;
    }
}
