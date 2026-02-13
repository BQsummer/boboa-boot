package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.common.vo.req.ai.ModelQueryRequest;
import com.bqsummer.common.vo.req.ai.ModelRegisterRequest;
import com.bqsummer.common.vo.resp.ai.ModelResponse;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.exception.ModelNotFoundException;
import com.bqsummer.exception.ModelValidationException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import com.bqsummer.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型服务实现类
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService {
    
    private final AiModelMapper aiModelMapper;
    private final StrategyModelRelationMapper strategyModelRelationMapper;
    private final EncryptionUtil encryptionUtil;
    
    @Transactional(rollbackFor = Exception.class)
    public ModelResponse registerModel(ModelRegisterRequest request, Long userId) {
        log.info("开始注册模型: name={}, version={}, provider={}", 
                request.getName(), request.getVersion(), request.getProvider());
        
        // 1. 验证必填字段
        validateRequiredFields(request);
        
        // 2. 检查唯一性约束（name + version）
        checkUniqueness(request.getName(), request.getVersion());
        
        // 3. 验证 API 端点连通性（可选，根据需求）
        // validateApiEndpoint(request.getApiEndpoint(), request.getApiKey());
        
        // 4. 创建实体并加密 API Key
        AiModel model = new AiModel();
        BeanUtils.copyProperties(request, model);
        
        // 加密 API Key
        String encryptedApiKey = encryptionUtil.encrypt(request.getApiKey());
        model.setApiKey(encryptedApiKey);
        
        // 设置默认值
        if (model.getWeight() == null) {
            model.setWeight(1);
        }
        if (model.getEnabled() == null) {
            model.setEnabled(true);
        }
        
        // 设置创建信息
        model.setCreatedBy(userId);
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedBy(userId);
        model.setUpdatedAt(LocalDateTime.now());
        
        // 5. 保存到数据库
        aiModelMapper.insert(model);
        
        log.info("模型注册成功: id={}, name={}", model.getId(), model.getName());
        
        // 6. 转换为响应对象（不包含 API Key）
        return convertToResponse(model, false);
    }
    
    public List<ModelResponse> listModels(ModelQueryRequest request) {
        log.info("查询模型列表: page={}, pageSize={}, provider={}, modelType={}, enabled={}", 
                request.getPage(), request.getPageSize(), request.getProvider(), 
                request.getModelType(), request.getEnabled());
        
        // 构建查询条件
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        
        if (request.getProvider() != null && !request.getProvider().isEmpty()) {
            queryWrapper.eq(AiModel::getProvider, request.getProvider());
        }
        
        if (request.getModelType() != null) {
            queryWrapper.eq(AiModel::getModelType, request.getModelType());
        }
        
        if (request.getEnabled() != null) {
            queryWrapper.eq(AiModel::getEnabled, request.getEnabled());
        }
        
        // 分页查询
        Page<AiModel> page = new Page<>(request.getPage(), request.getPageSize());
        Page<AiModel> resultPage = aiModelMapper.selectPage(page, queryWrapper);
        
        // 转换为响应对象
        List<ModelResponse> responses = resultPage.getRecords().stream()
                .map(model -> convertToResponse(model, false))
                .collect(Collectors.toList());
        
        log.info("查询到 {} 个模型", responses.size());
        return responses;
    }

    public List<String> listModelCodes() {
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(AiModel::getName);

        return aiModelMapper.selectList(queryWrapper).stream()
                .map(AiModel::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
    
    public ModelResponse getModelById(Long id) {
        log.info("查询模型详情: id={}", id);
        
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new ModelNotFoundException(id);
        }
        
        return convertToResponse(model, false);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public ModelResponse updateModel(Long id, ModelRegisterRequest request, Long userId) {
        log.info("更新模型: id={}", id);
        
        // 1. 检查模型是否存在
        AiModel existingModel = aiModelMapper.selectById(id);
        if (existingModel == null) {
            throw new ModelNotFoundException(id);
        }
        
        // 2. 验证必填字段
        validateRequiredFields(request);
        
        // 3. 如果修改了 name 或 version，检查唯一性
        if (!existingModel.getName().equals(request.getName()) 
                || !existingModel.getVersion().equals(request.getVersion())) {
            checkUniqueness(request.getName(), request.getVersion());
        }
        
        // 4. 更新字段
        BeanUtils.copyProperties(request, existingModel, "id", "createdBy", "createdAt");
        
        // 如果 API Key 发生变化，重新加密
        if (request.getApiKey() != null && !request.getApiKey().isEmpty()) {
            String encryptedApiKey = encryptionUtil.encrypt(request.getApiKey());
            existingModel.setApiKey(encryptedApiKey);
        }
        
        existingModel.setUpdatedBy(userId);
        existingModel.setUpdatedAt(LocalDateTime.now());
        
        // 5. 保存更新
        aiModelMapper.updateById(existingModel);
        
        log.info("模型更新成功: id={}", id);
        return convertToResponse(existingModel, false);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        log.info("删除模型: id={}", id);
        
        // 1. 检查模型是否存在
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new ModelNotFoundException(id);
        }
        
        // 2. 检查是否被路由策略引用
        Long count = strategyModelRelationMapper.selectCount(
                new LambdaQueryWrapper<StrategyModelRelation>()
                        .eq(StrategyModelRelation::getModelId, id)
        );
        
        if (count > 0) {
            throw new ModelValidationException("该模型正在被 " + count + " 个路由策略使用，无法删除");
        }
        
        // 3. 删除模型
        aiModelMapper.deleteById(id);
        
        log.info("模型删除成功: id={}", id);
    }
    
    /**
     * 验证必填字段
     */
    private void validateRequiredFields(ModelRegisterRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ModelValidationException("模型名称不能为空");
        }
        if (request.getVersion() == null || request.getVersion().trim().isEmpty()) {
            throw new ModelValidationException("模型版本不能为空");
        }
        if (request.getProvider() == null || request.getProvider().trim().isEmpty()) {
            throw new ModelValidationException("提供商不能为空");
        }
        if (request.getModelType() == null) {
            throw new ModelValidationException("模型类型不能为空");
        }
        if (request.getApiEndpoint() == null || request.getApiEndpoint().trim().isEmpty()) {
            throw new ModelValidationException("API端点不能为空");
        }
        if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
            throw new ModelValidationException("API密钥不能为空");
        }
    }
    
    /**
     * 检查唯一性约束
     */
    private void checkUniqueness(String name, String version) {
        Long count = aiModelMapper.selectCount(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getName, name)
                        .eq(AiModel::getVersion, version)
        );
        
        if (count > 0) {
            throw new ModelValidationException(
                    String.format("模型已存在: name=%s, version=%s", name, version));
        }
    }
    
    /**
     * 转换为响应对象
     * 
     * @param model 实体对象
     * @param includeApiKey 是否包含 API Key（默认不包含）
     * @return 响应对象
     */
    private ModelResponse convertToResponse(AiModel model, boolean includeApiKey) {
        ModelResponse response = new ModelResponse();
        BeanUtils.copyProperties(model, response);
        
        // 默认不返回 API Key
        if (!includeApiKey) {
            response.setApiKey(null);
        }
        
        return response;
    }
}
