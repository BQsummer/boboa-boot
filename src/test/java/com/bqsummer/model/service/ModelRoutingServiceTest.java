package com.bqsummer.model.service;

import com.bqsummer.common.dto.ai.ModelType;
import com.bqsummer.common.dto.ai.StrategyModelRelation;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.dto.ai.AiModel;
import com.bqsummer.common.dto.ai.RoutingStrategy;
import com.bqsummer.common.dto.ai.StrategyType;
import com.bqsummer.mapper.AiModelMapper;
import com.bqsummer.mapper.RoutingStrategyMapper;
import com.bqsummer.mapper.StrategyModelRelationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 路由服务测试
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@SpringBootTest
@Transactional
@DisplayName("路由服务测试")
class ModelRoutingServiceTest {
    
    @Autowired
    private ModelRoutingService routingService;
    
    @Autowired
    private AiModelMapper aiModelMapper;
    
    @Autowired
    private RoutingStrategyMapper routingStrategyMapper;
    
    @Autowired
    private StrategyModelRelationMapper relationMapper;
    
    private RoutingStrategy roundRobinStrategy;
    private AiModel model1;
    private AiModel model2;
    private AiModel model3;
    
    @BeforeEach
    void setUp() {
        // 创建测试模型
        model1 = createTestModel("模型1", 10);
        model2 = createTestModel("模型2", 5);
        model3 = createTestModel("模型3", 15);
        
        // 创建轮询策略
        roundRobinStrategy = new RoutingStrategy();
        roundRobinStrategy.setName("测试轮询策略");
        roundRobinStrategy.setStrategyType(StrategyType.ROUND_ROBIN);
        roundRobinStrategy.setEnabled(true);
        roundRobinStrategy.setIsDefault(false);
        roundRobinStrategy.setCreatedBy(1L);
        roundRobinStrategy.setUpdatedBy(1L);
        routingStrategyMapper.insert(roundRobinStrategy);
    }
    
    private AiModel createTestModel(String name, int weight) {
        AiModel model = new AiModel();
        model.setName(name);
        model.setVersion("v1");
        model.setProvider("test");
        model.setApiEndpoint("http://test.com");
        model.setApiKey("key");
        model.setModelType(ModelType.CHAT);
        model.setWeight(weight);
        model.setEnabled(true);
        model.setCreatedBy(1L);
        model.setUpdatedBy(1L);
        aiModelMapper.insert(model);
        return model;
    }
    
    @Test
    @DisplayName("轮询路由 - 依次返回模型")
    void testRoundRobinRouting() {
        // 添加模型到策略
        addModelToStrategy(roundRobinStrategy.getId(), model1.getId(), 1);
        addModelToStrategy(roundRobinStrategy.getId(), model2.getId(), 2);
        addModelToStrategy(roundRobinStrategy.getId(), model3.getId(), 3);
        
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        // 第一次应该返回 model1
        AiModel selected1 = routingService.selectModel(roundRobinStrategy.getId(), request);
        assertEquals(model1.getId(), selected1.getId());
        
        // 第二次应该返回 model2
        AiModel selected2 = routingService.selectModel(roundRobinStrategy.getId(), request);
        assertEquals(model2.getId(), selected2.getId());
        
        // 第三次应该返回 model3
        AiModel selected3 = routingService.selectModel(roundRobinStrategy.getId(), request);
        assertEquals(model3.getId(), selected3.getId());
        
        // 第四次应该回到 model1
        AiModel selected4 = routingService.selectModel(roundRobinStrategy.getId(), request);
        assertEquals(model1.getId(), selected4.getId());
    }
    
    @Test
    @DisplayName("权重路由 - 按权重选择模型")
    void testWeightedRouting() {
        RoutingStrategy weightedStrategy = new RoutingStrategy();
        weightedStrategy.setName("权重路由策略");
        weightedStrategy.setStrategyType(StrategyType.WEIGHTED);
        weightedStrategy.setEnabled(true);
        weightedStrategy.setIsDefault(false);
        weightedStrategy.setCreatedBy(1L);
        weightedStrategy.setUpdatedBy(1L);
        routingStrategyMapper.insert(weightedStrategy);
        
        addModelToStrategy(weightedStrategy.getId(), model1.getId(), 1);
        addModelToStrategy(weightedStrategy.getId(), model2.getId(), 2);
        addModelToStrategy(weightedStrategy.getId(), model3.getId(), 3);
        
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        // 多次调用，验证返回的都是有效模型
        for (int i = 0; i < 10; i++) {
            AiModel selected = routingService.selectModel(weightedStrategy.getId(), request);
            assertNotNull(selected);
            assertTrue(selected.getId().equals(model1.getId()) || 
                      selected.getId().equals(model2.getId()) || 
                      selected.getId().equals(model3.getId()));
        }
    }
    
    @Test
    @DisplayName("优先级路由 - 选择最高优先级模型")
    void testPriorityRouting() {
        RoutingStrategy priorityStrategy = new RoutingStrategy();
        priorityStrategy.setName("优先级路由策略");
        priorityStrategy.setStrategyType(StrategyType.PRIORITY);
        priorityStrategy.setEnabled(true);
        priorityStrategy.setIsDefault(false);
        priorityStrategy.setCreatedBy(1L);
        priorityStrategy.setUpdatedBy(1L);
        routingStrategyMapper.insert(priorityStrategy);
        
        addModelToStrategy(priorityStrategy.getId(), model1.getId(), 1);
        addModelToStrategy(priorityStrategy.getId(), model2.getId(), 2);
        addModelToStrategy(priorityStrategy.getId(), model3.getId(), 3);
        
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        // 应该总是返回 priority 最高的模型（priority=3 的 model3）
        AiModel selected = routingService.selectModel(priorityStrategy.getId(), request);
        assertEquals(model3.getId(), selected.getId());
    }
    
    @Test
    @DisplayName("标签路由 - 根据标签选择模型")
    void testTagBasedRouting() {
        // 给模型添加标签
        model1.setTags(List.of("fast", "cheap"));
        model2.setTags(List.of("accurate", "expensive"));
        model3.setTags(List.of("fast", "accurate"));
        aiModelMapper.updateById(model1);
        aiModelMapper.updateById(model2);
        aiModelMapper.updateById(model3);
        
        RoutingStrategy tagStrategy = new RoutingStrategy();
        tagStrategy.setName("标签路由策略");
        tagStrategy.setStrategyType(StrategyType.TAG_BASED);
        tagStrategy.setConfig("{\"requiredTags\": [\"fast\"]}");
        tagStrategy.setEnabled(true);
        tagStrategy.setIsDefault(false);
        tagStrategy.setCreatedBy(1L);
        tagStrategy.setUpdatedBy(1L);
        routingStrategyMapper.insert(tagStrategy);
        
        addModelToStrategy(tagStrategy.getId(), model1.getId(), 1);
        addModelToStrategy(tagStrategy.getId(), model2.getId(), 2);
        addModelToStrategy(tagStrategy.getId(), model3.getId(), 3);
        
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        // 应该返回带有 "fast" 标签的模型（model1 或 model3）
        AiModel selected = routingService.selectModel(tagStrategy.getId(), request);
        assertNotNull(selected);
        assertTrue(selected.getId().equals(model1.getId()) || selected.getId().equals(model3.getId()));
    }
    
    @Test
    @DisplayName("最少连接路由 - 选择负载最低的模型")
    void testLeastConnectionsRouting() {
        RoutingStrategy leastConnStrategy = new RoutingStrategy();
        leastConnStrategy.setName("最少连接路由策略");
        leastConnStrategy.setStrategyType(StrategyType.LEAST_CONNECTIONS);
        leastConnStrategy.setEnabled(true);
        leastConnStrategy.setIsDefault(false);
        leastConnStrategy.setCreatedBy(1L);
        leastConnStrategy.setUpdatedBy(1L);
        routingStrategyMapper.insert(leastConnStrategy);
        
        addModelToStrategy(leastConnStrategy.getId(), model1.getId(), 1);
        addModelToStrategy(leastConnStrategy.getId(), model2.getId(), 2);
        addModelToStrategy(leastConnStrategy.getId(), model3.getId(), 3);
        
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        // 应该返回一个模型（具体逻辑取决于实现）
        AiModel selected = routingService.selectModel(leastConnStrategy.getId(), request);
        assertNotNull(selected);
    }
    
    @Test
    @DisplayName("策略不存在时抛出异常")
    void testStrategyNotFound() {
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        assertThrows(Exception.class, () -> {
            routingService.selectModel(99999L, request);
        });
    }
    
    @Test
    @DisplayName("策略下没有模型时抛出异常")
    void testNoModelsInStrategy() {
        InferenceRequest request = new InferenceRequest();
        request.setPrompt("测试");
        
        assertThrows(Exception.class, () -> {
            routingService.selectModel(roundRobinStrategy.getId(), request);
        });
    }
    
    private void addModelToStrategy(Long strategyId, Long modelId, Integer priority) {
        StrategyModelRelation relation =
                new StrategyModelRelation();
        relation.setStrategyId(strategyId);
        relation.setModelId(modelId);
        relation.setPriority(priority);
        relationMapper.insert(relation);
    }
}
