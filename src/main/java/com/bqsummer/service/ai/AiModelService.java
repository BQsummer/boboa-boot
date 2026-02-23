package com.bqsummer.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.common.vo.req.ai.ModelQueryRequest;
import com.bqsummer.common.vo.req.ai.ModelRegisterRequest;
import com.bqsummer.common.vo.resp.ai.ModelResponse;
import com.bqsummer.exception.ModelNotFoundException;
import com.bqsummer.exception.ModelValidationException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import com.bqsummer.service.ai.adapter.ModelAdapter;
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
import java.util.stream.Stream;

/**
 * AI model service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelMapper aiModelMapper;
    private final StrategyModelRelationMapper strategyModelRelationMapper;
    private final EncryptionUtil encryptionUtil;
    private final List<ModelAdapter> adapters;

    @Transactional(rollbackFor = Exception.class)
    public ModelResponse registerModel(ModelRegisterRequest request, Long userId) {
        log.info("register model: name={}, version={}, apiKind={}",
                request.getName(), request.getVersion(), request.getApiKind());

        validateRequiredFields(request, true);
        checkUniqueness(request.getName(), request.getVersion());

        AiModel model = new AiModel();
        BeanUtils.copyProperties(request, model);

        String encryptedApiKey = encryptionUtil.encrypt(request.getApiKey());
        model.setApiKey(encryptedApiKey);

        if (model.getEnabled() == null) {
            model.setEnabled(true);
        }

        model.setCreatedBy(userId);
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedBy(userId);
        model.setUpdatedAt(LocalDateTime.now());

        aiModelMapper.insert(model);
        log.info("model registered: id={}, name={}", model.getId(), model.getName());

        return convertToResponse(model, false);
    }

    public List<ModelResponse> listModels(ModelQueryRequest request) {
        log.info("list models: page={}, pageSize={}, apiKind={}, modelType={}, enabled={}",
                request.getPage(), request.getPageSize(), request.getApiKind(),
                request.getModelType(), request.getEnabled());

        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();

        if (hasText(request.getApiKind())) {
            queryWrapper.eq(AiModel::getApiKind, request.getApiKind());
        }

        if (request.getModelType() != null) {
            queryWrapper.eq(AiModel::getModelType, request.getModelType());
        }

        if (request.getEnabled() != null) {
            queryWrapper.eq(AiModel::getEnabled, request.getEnabled());
        }

        Page<AiModel> page = new Page<>(request.getPage(), request.getPageSize());
        Page<AiModel> resultPage = aiModelMapper.selectPage(page, queryWrapper);

        List<ModelResponse> responses = resultPage.getRecords().stream()
                .map(model -> convertToResponse(model, false))
                .collect(Collectors.toList());

        log.info("models listed: count={}", responses.size());
        return responses;
    }

    public List<String> listModelCodes() {
        LambdaQueryWrapper<AiModel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(AiModel::getName);

        return aiModelMapper.selectList(queryWrapper).stream()
                .map(AiModel::getName)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public List<String> listModelApiKinds() {
        return adapters.stream()
                .flatMap(adapter -> {
                    if (adapter.supportedApiKinds() == null) {
                        return Stream.empty();
                    }
                    return adapter.supportedApiKinds().stream();
                })
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public ModelResponse getModelById(Long id) {
        log.info("get model by id: id={}", id);

        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new ModelNotFoundException(id);
        }

        return convertToResponse(model, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelResponse updateModel(Long id, ModelRegisterRequest request, Long userId) {
        log.info("update model: id={}", id);

        AiModel existingModel = aiModelMapper.selectById(id);
        if (existingModel == null) {
            throw new ModelNotFoundException(id);
        }

        validateRequiredFields(request, false);

        if (!existingModel.getName().equals(request.getName())
                || !existingModel.getVersion().equals(request.getVersion())) {
            checkUniqueness(request.getName(), request.getVersion());
        }

        BeanUtils.copyProperties(request, existingModel, "id", "createdBy", "createdAt", "apiKey");

        if (hasText(request.getApiKey())) {
            String encryptedApiKey = encryptionUtil.encrypt(request.getApiKey());
            existingModel.setApiKey(encryptedApiKey);
        }

        existingModel.setUpdatedBy(userId);
        existingModel.setUpdatedAt(LocalDateTime.now());

        aiModelMapper.updateById(existingModel);
        log.info("model updated: id={}", id);

        return convertToResponse(existingModel, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        log.info("delete model: id={}", id);

        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new ModelNotFoundException(id);
        }

        Long count = strategyModelRelationMapper.selectCount(
                new LambdaQueryWrapper<StrategyModelRelation>()
                        .eq(StrategyModelRelation::getModelId, id)
        );

        if (count > 0) {
            throw new ModelValidationException(
                    "Model is still bound by " + count + " routing strategies and cannot be deleted");
        }

        aiModelMapper.deleteById(id);
        log.info("model deleted: id={}", id);
    }

    private void validateRequiredFields(ModelRegisterRequest request, boolean requireApiKey) {
        if (!hasText(request.getName())) {
            throw new ModelValidationException("Model name cannot be blank");
        }
        if (!hasText(request.getVersion())) {
            throw new ModelValidationException("Model version cannot be blank");
        }
        if (!hasText(request.getApiKind())) {
            throw new ModelValidationException("API kind cannot be blank");
        }
        if (request.getModelType() == null) {
            throw new ModelValidationException("Model type cannot be null");
        }
        if (!hasText(request.getApiEndpoint())) {
            throw new ModelValidationException("API endpoint cannot be blank");
        }
        if (requireApiKey && !hasText(request.getApiKey())) {
            throw new ModelValidationException("API key cannot be blank");
        }
    }

    private void checkUniqueness(String name, String version) {
        Long count = aiModelMapper.selectCount(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getName, name)
                        .eq(AiModel::getVersion, version)
        );

        if (count > 0) {
            throw new ModelValidationException(
                    String.format("Model already exists: name=%s, version=%s", name, version));
        }
    }

    private ModelResponse convertToResponse(AiModel model, boolean includeApiKey) {
        ModelResponse response = new ModelResponse();
        BeanUtils.copyProperties(model, response);
        if (!includeApiKey) {
            response.setApiKey(null);
        }
        return response;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
