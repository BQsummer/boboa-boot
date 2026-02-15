package com.bqsummer.model.service;

import com.bqsummer.common.bo.ai.AiModelBo;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.common.dto.ai.StrategyType;
import com.bqsummer.common.dto.router.LeastConnectionsRoutingAlgorithm;
import com.bqsummer.common.dto.router.PriorityRoutingAlgorithm;
import com.bqsummer.common.dto.router.RoundRobinRoutingAlgorithm;
import com.bqsummer.common.dto.router.RoutingAlgorithm;
import com.bqsummer.common.dto.router.TagBasedRoutingAlgorithm;
import com.bqsummer.common.dto.router.WeightedRoutingAlgorithm;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.exception.RoutingException;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.RoutingStrategyMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import com.bqsummer.mapstruct.ai.AiModelStructMapper;
import com.bqsummer.service.ai.ModelRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModelRoutingService unit tests")
class ModelRoutingServiceTest {

    @Mock
    private RoutingStrategyMapper routingStrategyMapper;

    @Mock
    private StrategyModelRelationMapper relationMapper;

    @Mock
    private AiModelMapper aiModelMapper;

    @Mock
    private AiModelStructMapper aiModelStructMapper;

    private ModelRoutingService routingService;

    @BeforeEach
    void setUp() {
        List<RoutingAlgorithm> algorithms = List.of(
                new RoundRobinRoutingAlgorithm(),
                new LeastConnectionsRoutingAlgorithm(),
                new TagBasedRoutingAlgorithm(),
                new PriorityRoutingAlgorithm(),
                new WeightedRoutingAlgorithm()
        );

        when(aiModelStructMapper.toBo(any(AiModel.class))).thenAnswer(invocation -> toBo(invocation.getArgument(0)));
        routingService = new ModelRoutingService(routingStrategyMapper, relationMapper, aiModelMapper, algorithms, aiModelStructMapper);
    }

    @Test
    @DisplayName("ROUND_ROBIN: obeys relation priority order and rotates")
    void roundRobinShouldRotateByPriorityOrder() {
        Long strategyId = 1L;
        when(routingStrategyMapper.selectById(strategyId)).thenReturn(strategy(strategyId, StrategyType.ROUND_ROBIN, null));

        AiModel m1 = model(101L, "m1", 1, List.of("fast"));
        AiModel m2 = model(102L, "m2", 1, List.of("accurate"));
        AiModel m3 = model(103L, "m3", 1, List.of("cheap"));

        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation(strategyId, m1.getId(), 10, 20),
                relation(strategyId, m2.getId(), 30, 30),
                relation(strategyId, m3.getId(), 20, 50)
        ));
        when(aiModelMapper.selectList(any())).thenReturn(List.of(m1, m2, m3));

        InferenceRequest request = request();

        AiModelBo first = routingService.selectModel(strategyId, request);
        AiModelBo second = routingService.selectModel(strategyId, request);
        AiModelBo third = routingService.selectModel(strategyId, request);

        assertEquals(m2.getId(), first.getId());
        assertEquals(m3.getId(), second.getId());
        assertEquals(m1.getId(), third.getId());
    }

    @Test
    @DisplayName("PRIORITY: chooses highest-priority model")
    void priorityShouldSelectHighestPriorityModel() {
        Long strategyId = 2L;
        when(routingStrategyMapper.selectById(strategyId)).thenReturn(strategy(strategyId, StrategyType.PRIORITY, null));

        AiModel low = model(201L, "low", 99, List.of());
        AiModel high = model(202L, "high", 1, List.of());

        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation(strategyId, low.getId(), 1, 50),
                relation(strategyId, high.getId(), 99, 50)
        ));
        when(aiModelMapper.selectList(any())).thenReturn(List.of(low, high));

        AiModelBo selected = routingService.selectModel(strategyId, request());
        assertEquals(high.getId(), selected.getId());
    }

    @Test
    @DisplayName("WEIGHTED: uses relation weights instead of model table weights")
    void weightedShouldUseRelationWeight() {
        Long strategyId = 3L;
        when(routingStrategyMapper.selectById(strategyId)).thenReturn(strategy(strategyId, StrategyType.WEIGHTED, null));

        AiModel m1 = model(301L, "m1", null, List.of());
        AiModel m2 = model(302L, "m2", null, List.of());

        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation(strategyId, m1.getId(), 1, 80),
                relation(strategyId, m2.getId(), 2, 20)
        ));
        when(aiModelMapper.selectList(any())).thenReturn(List.of(m1, m2));

        AiModelBo selected = routingService.selectModel(strategyId, request());

        assertTrue(selected.getId().equals(m1.getId()) || selected.getId().equals(m2.getId()));
        assertTrue(selected.getWeight() == 80 || selected.getWeight() == 20);
    }

    @Test
    @DisplayName("TAG_BASED: picks model matching requiredTags")
    void tagBasedShouldSelectMatchedModel() {
        Long strategyId = 4L;
        when(routingStrategyMapper.selectById(strategyId))
                .thenReturn(strategy(strategyId, StrategyType.TAG_BASED, "{\"requiredTags\":[\"fast\",\"cheap\"]}"));

        AiModel m1 = model(401L, "m1", 50, List.of("fast"));
        AiModel m2 = model(402L, "m2", 50, List.of("fast", "cheap"));

        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation(strategyId, m1.getId(), 10, 50),
                relation(strategyId, m2.getId(), 9, 50)
        ));
        when(aiModelMapper.selectList(any())).thenReturn(List.of(m1, m2));

        AiModelBo selected = routingService.selectModel(strategyId, request());
        assertEquals(m2.getId(), selected.getId());
    }

    @Test
    @DisplayName("LEAST_CONNECTIONS: chooses first model when all connections are equal")
    void leastConnectionsShouldFallbackToFirstSortedModelOnTie() {
        Long strategyId = 5L;
        when(routingStrategyMapper.selectById(strategyId)).thenReturn(strategy(strategyId, StrategyType.LEAST_CONNECTIONS, null));

        AiModel m1 = model(501L, "m1", 50, List.of());
        AiModel m2 = model(502L, "m2", 50, List.of());

        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation(strategyId, m1.getId(), 1, 50),
                relation(strategyId, m2.getId(), 2, 50)
        ));
        when(aiModelMapper.selectList(any())).thenReturn(List.of(m1, m2));

        AiModelBo selected = routingService.selectModel(strategyId, request());
        assertEquals(m2.getId(), selected.getId());
    }

    @Test
    @DisplayName("WEIGHTED: throws when relation weights do not sum to 100")
    void weightedShouldThrowWhenWeightSumNot100() {
        Long strategyId = 6L;
        when(routingStrategyMapper.selectById(strategyId)).thenReturn(strategy(strategyId, StrategyType.WEIGHTED, null));

        AiModel m1 = model(601L, "m1", null, List.of());
        AiModel m2 = model(602L, "m2", null, List.of());

        when(relationMapper.selectList(any())).thenReturn(List.of(
                relation(strategyId, m1.getId(), 1, 60),
                relation(strategyId, m2.getId(), 2, 30)
        ));
        when(aiModelMapper.selectList(any())).thenReturn(List.of(m1, m2));

        assertThrows(RoutingException.class, () -> routingService.selectModel(strategyId, request()));
    }

    private static RoutingStrategy strategy(Long id, StrategyType type, String config) {
        RoutingStrategy strategy = new RoutingStrategy();
        strategy.setId(id);
        strategy.setName("s-" + id);
        strategy.setStrategyType(type);
        strategy.setConfig(config == null ? "{}" : config);
        strategy.setEnabled(true);
        return strategy;
    }

    private static StrategyModelRelation relation(Long strategyId, Long modelId, Integer priority, Integer weight) {
        StrategyModelRelation relation = new StrategyModelRelation();
        relation.setStrategyId(strategyId);
        relation.setModelId(modelId);
        relation.setPriority(priority);
        relation.setWeight(weight);
        return relation;
    }

    private static AiModel model(Long id, String name, Integer weight, List<String> tags) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setName(name);
        model.setVersion("v1");
        model.setProvider("test");
        model.setModelType(ModelType.CHAT);
        model.setApiEndpoint("http://localhost");
        model.setApiKey("test");
        model.setEnabled(true);
        model.setTags(tags);
        return model;
    }

    private static AiModelBo toBo(AiModel model) {
        AiModelBo bo = new AiModelBo();
        bo.setId(model.getId());
        bo.setName(model.getName());
        bo.setVersion(model.getVersion());
        bo.setProvider(model.getProvider());
        bo.setModelType(model.getModelType());
        bo.setApiEndpoint(model.getApiEndpoint());
        bo.setApiKey(model.getApiKey());
        bo.setEnabled(model.getEnabled());
        bo.setTags(model.getTags());
        return bo;
    }

    private static InferenceRequest request() {
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("ping");
        return request;
    }
}
