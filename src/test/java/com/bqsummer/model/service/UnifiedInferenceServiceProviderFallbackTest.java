package com.bqsummer.model.service;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.mapstruct.ai.AiModelStructMapper;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.service.ai.ModelRequestLogService;
import com.bqsummer.service.ai.ModelRoutingService;
import com.bqsummer.service.ai.UnifiedInferenceService;
import com.bqsummer.service.ai.adapter.ModelAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class UnifiedInferenceServiceProviderFallbackTest {

    @Test
    void fillResponseDefaultsShouldUseModelProviderWhenResponseProviderIsEmpty() throws Exception {
        UnifiedInferenceService service = newService();
        InferenceResponse response = new InferenceResponse();
        AiModelBo model = new AiModelBo();
        model.setApiKind("openrouter");
        model.setProvider("openai");

        invokeFillResponseDefaults(service, response, model);

        assertEquals("openai", response.getProvider());
    }

    @Test
    void fillResponseDefaultsShouldNotOverrideExistingResponseProvider() throws Exception {
        UnifiedInferenceService service = newService();
        InferenceResponse response = new InferenceResponse();
        response.setProvider("azure");
        AiModelBo model = new AiModelBo();
        model.setApiKind("openrouter");
        model.setProvider("openai");

        invokeFillResponseDefaults(service, response, model);

        assertEquals("azure", response.getProvider());
    }

    private UnifiedInferenceService newService() {
        AiModelMapper aiModelMapper = mock(AiModelMapper.class);
        ModelRoutingService routingService = mock(ModelRoutingService.class);
        ModelRequestLogService requestLogService = mock(ModelRequestLogService.class);
        AiModelStructMapper aiModelStructMapper = mock(AiModelStructMapper.class);
        List<ModelAdapter> adapters = List.of();
        return new UnifiedInferenceService(
                aiModelMapper,
                adapters,
                routingService,
                requestLogService,
                aiModelStructMapper
        );
    }

    private void invokeFillResponseDefaults(UnifiedInferenceService service,
                                            InferenceResponse response,
                                            AiModelBo model) throws Exception {
        Method method = UnifiedInferenceService.class.getDeclaredMethod(
                "fillResponseDefaults",
                InferenceResponse.class,
                AiModelBo.class
        );
        method.setAccessible(true);
        method.invoke(service, response, model);
    }
}
