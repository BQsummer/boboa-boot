package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.service.ai.adapter.ModelAdapter;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.HealthStatus;
import com.bqsummer.common.dto.ai.ModelHealthStatus;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.ModelHealthStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型健康检查服务实现
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelHealthService {
    
    private final AiModelMapper aiModelMapper;
    private final ModelHealthStatusMapper healthStatusMapper;
    private final List<ModelAdapter> adapters;
    
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final String HEALTH_CHECK_PROMPT = "ping";
    
    public void performHealthCheck(Long modelId) {
        AiModel model = aiModelMapper.selectById(modelId);
        if (model == null) {
            log.warn("模型不存在，跳过健康检查: modelId={}", modelId);
            return;
        }
        
        log.debug("开始健康检查: modelId={}, modelName={}", modelId, model.getName());
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        Integer responseTime = null;
        String errorMessage = null;
        
        try {
            // 选择适配器
            ModelAdapter adapter = selectAdapter(model);
            if (adapter == null) {
                errorMessage = "没有可用的适配器";
                recordHealthCheck(modelId, false, 0, errorMessage);
                return;
            }
            
            // 执行健康检查（发送简单的 ping 请求）
            InferenceRequest request = new InferenceRequest();
            request.setPrompt(HEALTH_CHECK_PROMPT);
            request.setMaxTokens(10);
            request.setTemperature(0.0);
            
            InferenceResponse response = adapter.chat(model, request);
            
            responseTime = (int) (System.currentTimeMillis() - startTime);
            success = response.getSuccess();
            
            if (!success) {
                errorMessage = response.getErrorMessage();
            }
            
        } catch (Exception e) {
            responseTime = (int) (System.currentTimeMillis() - startTime);
            errorMessage = e.getMessage();
            log.error("健康检查异常: modelId={}, error={}", modelId, e.getMessage());
        }
        
        // 记录检查结果
        recordHealthCheck(modelId, success, responseTime, errorMessage);
        
        log.info("健康检查完成: modelId={}, success={}, responseTime={}ms", 
                modelId, success, responseTime);
    }
    
    public void performBatchHealthCheck() {
        log.info("开始批量健康检查");
        
        // 查询所有启用的模型
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModel::getEnabled, true);
        
        List<AiModel> models = aiModelMapper.selectList(queryWrapper);
        
        log.info("批量健康检查: 共 {} 个模型", models.size());
        
        for (AiModel model : models) {
            try {
                performHealthCheck(model.getId());
            } catch (Exception e) {
                log.error("健康检查失败: modelId={}, error={}", model.getId(), e.getMessage(), e);
            }
        }
        
        log.info("批量健康检查完成");
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void recordHealthCheck(Long modelId, boolean success, Integer responseTime, String errorMessage) {
        // 查询现有状态
        LambdaQueryWrapper<ModelHealthStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModelHealthStatus::getModelId, modelId);
        
        ModelHealthStatus status = healthStatusMapper.selectOne(queryWrapper);
        
        if (status == null) {
            // 创建新的健康状态记录
            status = new ModelHealthStatus();
            status.setModelId(modelId);
            status.setTotalChecks(0);
            status.setSuccessfulChecks(0);
            status.setConsecutiveFailures(0);
            status.setUptimePercentage(BigDecimal.ZERO);
        }
        
        // 更新检查计数
        status.setTotalChecks(status.getTotalChecks() + 1);
        
        if (success) {
            // 成功：重置连续失败计数
            status.setSuccessfulChecks(status.getSuccessfulChecks() + 1);
            status.setConsecutiveFailures(0);
            status.setStatus(HealthStatus.ONLINE);
            status.setLastError(null);
        } else {
            // 失败：增加连续失败计数
            status.setConsecutiveFailures(status.getConsecutiveFailures() + 1);
            
            // 根据错误信息判断状态
            if (errorMessage != null && errorMessage.toLowerCase().contains("timeout")) {
                status.setStatus(HealthStatus.TIMEOUT);
            } else if (errorMessage != null && errorMessage.toLowerCase().contains("auth")) {
                status.setStatus(HealthStatus.AUTH_FAILED);
            } else {
                status.setStatus(HealthStatus.OFFLINE);
            }
            
            status.setLastError(errorMessage);
            
            // 连续失败达到阈值，禁用模型
            if (status.getConsecutiveFailures() >= MAX_CONSECUTIVE_FAILURES) {
                disableModel(modelId);
                log.warn("模型连续失败 {} 次，已自动禁用: modelId={}", 
                        status.getConsecutiveFailures(), modelId);
            }
        }
        
        // 计算可用性百分比
        BigDecimal uptimePercentage = BigDecimal.valueOf(status.getSuccessfulChecks())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(status.getTotalChecks()), 2, RoundingMode.HALF_UP);
        status.setUptimePercentage(uptimePercentage);
        
        // 更新响应时间
        status.setLastResponseTime(responseTime);
        status.setLastCheckTime(LocalDateTime.now());
        
        // 保存或更新
        if (status.getId() == null) {
            healthStatusMapper.insert(status);
        } else {
            healthStatusMapper.updateById(status);
        }
    }
    
    public ModelHealthStatus getHealthStatus(Long modelId) {
        LambdaQueryWrapper<ModelHealthStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModelHealthStatus::getModelId, modelId);
        return healthStatusMapper.selectOne(queryWrapper);
    }
    
    public List<ModelHealthStatus> getAllHealthStatus() {
        return healthStatusMapper.selectList(null);
    }
    
    /**
     * 选择适配器
     */
    private ModelAdapter selectAdapter(AiModel model) {
        for (ModelAdapter adapter : adapters) {
            if (adapter.supports(model)) {
                return adapter;
            }
        }
        return null;
    }
    
    /**
     * 禁用模型
     */
    private void disableModel(Long modelId) {
        AiModel model = aiModelMapper.selectById(modelId);
        if (model != null && model.getEnabled()) {
            model.setEnabled(false);
            model.setUpdatedAt(LocalDateTime.now());
            aiModelMapper.updateById(model);
        }
    }
}
