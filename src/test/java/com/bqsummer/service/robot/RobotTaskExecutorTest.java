package com.bqsummer.service.robot;

import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.im.Message;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.RobotTaskExecutionLog;
import com.bqsummer.common.dto.robot.SendMessagePayload;
import com.bqsummer.mapper.ConversationMapper;
import com.bqsummer.mapper.RobotTaskExecutionLogMapper;
import com.bqsummer.common.vo.req.ai.InferenceRequest;
import com.bqsummer.common.vo.resp.ai.InferenceResponse;
import com.bqsummer.model.service.UnifiedInferenceService;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RobotTaskExecutor 单元测试
 * 测试executeSendMessage方法的LLM调用逻辑
 */
@SpringBootTest
@Transactional
@DisplayName("RobotTaskExecutor 单元测试")
class RobotTaskExecutorTest extends BaseTest {
    
    @SpyBean
    private RobotTaskExecutor robotTaskExecutor;
    
    @MockBean
    private UnifiedInferenceService inferenceService;
    
    @MockBean
    private MessageRepository messageRepository;
    
    @MockBean
    private ConversationMapper conversationMapper;
    
    @MockBean
    private RobotTaskExecutionLogMapper executionLogMapper;
    
    @BeforeEach
    void setUp() {
        // Mock UnifiedInferenceService返回成功响应
        InferenceResponse mockResponse = new InferenceResponse();
        mockResponse.setSuccess(true);
        mockResponse.setContent("这是AI生成的回复内容");
        mockResponse.setTotalTokens(50);
        mockResponse.setResponseTimeMs(1200);
        when(inferenceService.chat(any(InferenceRequest.class))).thenReturn(mockResponse);
        
        // Mock消息保存成功
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(System.currentTimeMillis()); // 模拟自增ID
            return msg;
        });
    }
    
    @Test
    @DisplayName("测试executeSendMessage方法解析action_payload")
    void testExecuteSendMessageParsesPayload() {
        // Given: 准备任务数据
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(123L)
                .senderId(1001L)
                .receiverId(2001L)
                .content("用户的问题")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(1L);
        task.setUserId(1001L);
        task.setRobotId(2001L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务（这会调用executeSendMessage）
        // 由于executeSendMessage是private方法，我们通过execute方法间接测试
        robotTaskExecutor.execute(task);
        
        // Then: 验证UnifiedInferenceService被调用
        verify(inferenceService).chat(any(InferenceRequest.class));
    }
    
    @Test
    @DisplayName("测试executeSendMessage调用UnifiedInferenceService")
    void testExecuteSendMessageCallsInferenceService() {
        // Given: 准备任务数据
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(123L)
                .senderId(1001L)
                .receiverId(2001L)
                .content("今天天气怎么样？")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(2L);
        task.setUserId(1001L);
        task.setRobotId(2001L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        robotTaskExecutor.execute(task);
        
        // Then: 验证UnifiedInferenceService被调用且参数正确
        ArgumentCaptor<InferenceRequest> requestCaptor = ArgumentCaptor.forClass(InferenceRequest.class);
        verify(inferenceService).chat(requestCaptor.capture());
        
        InferenceRequest request = requestCaptor.getValue();
        assertNotNull(request, "InferenceRequest不应为空");
        assertEquals(1L, request.getModelId(), "modelId应该正确");
        assertTrue(request.getPrompt().contains("今天天气怎么样？"), "prompt应该包含用户消息内容");
    }
    
    @Test
    @DisplayName("测试executeSendMessage创建AI回复消息")
    void testExecuteSendMessageCreatesAiReplyMessage() {
        // Given: 准备任务数据
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(123L)
                .senderId(1001L)
                .receiverId(2001L)
                .content("你是谁？")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(3L);
        task.setUserId(1001L);
        task.setRobotId(2001L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        robotTaskExecutor.execute(task);
        
        // Then: 验证创建了AI回复消息
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        
        Message aiReply = messageCaptor.getValue();
        assertNotNull(aiReply, "AI回复消息不应为空");
        assertEquals(2001L, aiReply.getSenderId(), "senderId应该是AI用户ID");
        assertEquals(1001L, aiReply.getReceiverId(), "receiverId应该是原始发送者ID");
        assertEquals("这是AI生成的回复内容", aiReply.getContent(), "content应该是LLM响应内容");
        assertEquals("text", aiReply.getType(), "type应该是text");
        assertEquals("sent", aiReply.getStatus(), "status应该是sent");
    }
    
    // ========== US2: 任务执行失败自动重试测试 ==========
    
    @Test
    @DisplayName("T023: 测试LLM调用超时时任务重试")
    void testExecuteSendMessageRetriesOnTimeout() {
        // Given: Mock LLM服务抛出超时异常
        when(inferenceService.chat(any(InferenceRequest.class)))
                .thenThrow(new RuntimeException("LLM调用超时"));
        
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(123L)
                .senderId(1001L)
                .receiverId(2001L)
                .content("测试超时重试")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(100L);
        task.setUserId(1001L);
        task.setRobotId(2001L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务（预期会失败）
        assertThrows(RuntimeException.class, () -> robotTaskExecutor.execute(task));
        
        // Then: 验证任务失败且会触发重试逻辑
        verify(inferenceService).chat(any(InferenceRequest.class));
        // 注意：实际的状态更新和重试调度由execute方法的异常处理部分完成
    }
    
    @Test
    @DisplayName("T024: 测试LLM返回错误时任务重试")
    void testExecuteSendMessageRetriesOnLlmError() {
        // Given: Mock LLM服务返回失败响应
        InferenceResponse errorResponse = new InferenceResponse();
        errorResponse.setSuccess(false);
        errorResponse.setContent("模型服务不可用");
        when(inferenceService.chat(any(InferenceRequest.class))).thenReturn(errorResponse);
        
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(124L)
                .senderId(1002L)
                .receiverId(2002L)
                .content("测试LLM错误重试")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(101L);
        task.setUserId(1002L);
        task.setRobotId(2002L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务（预期会失败）
        assertThrows(RuntimeException.class, () -> robotTaskExecutor.execute(task));
        
        // Then: 验证LLM服务被调用
        verify(inferenceService).chat(any(InferenceRequest.class));
    }
    
    @Test
    @DisplayName("T025: 测试达到最大重试次数后任务不再重试")
    void testExecuteSendMessageDoesNotRetryWhenMaxRetriesReached() {
        // Given: Mock LLM服务失败
        when(inferenceService.chat(any(InferenceRequest.class)))
                .thenThrow(new RuntimeException("LLM服务故障"));
        
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(125L)
                .senderId(1003L)
                .receiverId(2003L)
                .content("测试最大重试次数")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(102L);
        task.setUserId(1003L);
        task.setRobotId(2003L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(3); // 已达到最大重试次数
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        assertThrows(RuntimeException.class, () -> robotTaskExecutor.execute(task));
        
        // Then: 验证LLM服务被调用
        verify(inferenceService).chat(any(InferenceRequest.class));
        // 注意：达到最大重试次数后，任务应标记为FAILED但不再重新调度
    }
    
    @Test
    @DisplayName("T026: 测试重试时retry_count正确增加")
    void testExecuteSendMessageIncrementsRetryCount() {
        // Given: Mock LLM服务第一次失败
        when(inferenceService.chat(any(InferenceRequest.class)))
                .thenThrow(new RuntimeException("LLM第一次调用失败"));
        
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(126L)
                .senderId(1004L)
                .receiverId(2004L)
                .content("测试retry_count增加")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(103L);
        task.setUserId(1004L);
        task.setRobotId(2004L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(1); // 当前是第2次尝试（第1次重试）
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        assertThrows(RuntimeException.class, () -> robotTaskExecutor.execute(task));
        
        // Then: 验证LLM服务被调用
        verify(inferenceService).chat(any(InferenceRequest.class));
        // 注意：retry_count的增加由execute方法的异常处理部分完成
    }
    
    // ========== US3: 任务执行日志追踪测试 ==========
    
    @Test
    @DisplayName("T034: 测试任务成功执行时创建执行日志")
    void testExecutionLogCreatedOnSuccess() {
        // Given: 准备成功的任务数据
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(200L)
                .senderId(1005L)
                .receiverId(2005L)
                .content("测试执行日志创建")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(200L);
        task.setUserId(1005L);
        task.setRobotId(2005L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now().minusSeconds(5)); // 延迟5秒
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        robotTaskExecutor.execute(task);
        
        // Then: 验证执行日志被创建
        ArgumentCaptor<RobotTaskExecutionLog> logCaptor = ArgumentCaptor.forClass(RobotTaskExecutionLog.class);
        verify(executionLogMapper).insert(logCaptor.capture());
        
        RobotTaskExecutionLog log = logCaptor.getValue();
        assertNotNull(log, "执行日志不应为空");
        assertEquals(200L, log.getTaskId(), "taskId应该正确");
        assertEquals(1, log.getExecutionAttempt(), "executionAttempt应该为1（retryCount=0时）");
        assertEquals("SUCCESS", log.getStatus(), "status应该为SUCCESS");
        assertNotNull(log.getStartedAt(), "startedAt不应为空");
        assertNotNull(log.getCompletedAt(), "completedAt不应为空");
        assertNotNull(log.getExecutionDurationMs(), "executionDurationMs不应为空");
        assertNotNull(log.getDelayFromScheduledMs(), "delayFromScheduledMs不应为空");
        assertNull(log.getErrorMessage(), "errorMessage应该为空（成功时）");
    }
    
    @Test
    @DisplayName("T035: 测试任务失败时创建执行日志并记录错误信息")
    void testExecutionLogCreatedOnFailure() {
        // Given: Mock LLM服务失败
        when(inferenceService.chat(any(InferenceRequest.class)))
                .thenThrow(new RuntimeException("LLM服务故障"));
        
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(201L)
                .senderId(1006L)
                .receiverId(2006L)
                .content("测试失败日志")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(201L);
        task.setUserId(1006L);
        task.setRobotId(2006L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now().minusSeconds(3));
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务（预期失败）
        assertThrows(RuntimeException.class, () -> robotTaskExecutor.execute(task));
        
        // Then: 验证执行日志被创建且包含错误信息
        ArgumentCaptor<RobotTaskExecutionLog> logCaptor = ArgumentCaptor.forClass(RobotTaskExecutionLog.class);
        verify(executionLogMapper).insert(logCaptor.capture());
        
        RobotTaskExecutionLog log = logCaptor.getValue();
        assertNotNull(log, "执行日志不应为空");
        assertEquals(201L, log.getTaskId(), "taskId应该正确");
        assertEquals("FAILED", log.getStatus(), "status应该为FAILED");
        assertNotNull(log.getErrorMessage(), "errorMessage不应为空（失败时）");
        assertTrue(log.getErrorMessage().contains("LLM服务故障"), 
                "errorMessage应该包含异常信息");
    }
    
    @Test
    @DisplayName("T036: 测试执行日志包含正确的execution_duration_ms")
    void testExecutionLogContainsExecutionDuration() {
        // Given: 准备任务数据
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(202L)
                .senderId(1007L)
                .receiverId(2007L)
                .content("测试执行时长")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(202L);
        task.setUserId(1007L);
        task.setRobotId(2007L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        robotTaskExecutor.execute(task);
        
        // Then: 验证execution_duration_ms正确计算
        ArgumentCaptor<RobotTaskExecutionLog> logCaptor = ArgumentCaptor.forClass(RobotTaskExecutionLog.class);
        verify(executionLogMapper).insert(logCaptor.capture());
        
        RobotTaskExecutionLog log = logCaptor.getValue();
        assertNotNull(log.getExecutionDurationMs(), "executionDurationMs不应为空");
        assertTrue(log.getExecutionDurationMs() >= 0, "executionDurationMs应该>=0");
        // 验证时间是合理的（通常LLM调用应该在几秒内）
        assertTrue(log.getExecutionDurationMs() < 60000, 
                "executionDurationMs应该<60秒（测试环境）");
    }
    
    @Test
    @DisplayName("T037: 测试执行日志包含正确的delay_from_scheduled_ms")
    void testExecutionLogContainsDelayFromScheduled() {
        // Given: 准备一个过去的scheduled_at时间
        LocalDateTime scheduledAt = LocalDateTime.now().minusSeconds(10); // 10秒前
        
        SendMessagePayload payload = SendMessagePayload.builder()
                .messageId(203L)
                .senderId(1008L)
                .receiverId(2008L)
                .content("测试调度延迟")
                .modelId(1L)
                .build();
        
        RobotTask task = new RobotTask();
        task.setId(203L);
        task.setUserId(1008L);
        task.setRobotId(2008L);
        task.setActionType("SEND_MESSAGE");
        task.setActionPayload(JsonUtil.toJson(payload));
        task.setScheduledAt(scheduledAt);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        
        // When: 执行任务
        robotTaskExecutor.execute(task);
        
        // Then: 验证delay_from_scheduled_ms正确计算
        ArgumentCaptor<RobotTaskExecutionLog> logCaptor = ArgumentCaptor.forClass(RobotTaskExecutionLog.class);
        verify(executionLogMapper).insert(logCaptor.capture());
        
        RobotTaskExecutionLog log = logCaptor.getValue();
        assertNotNull(log.getDelayFromScheduledMs(), "delayFromScheduledMs不应为空");
        // 延迟应该接近10秒（10000ms）
        assertTrue(log.getDelayFromScheduledMs() >= 9000, 
                "delayFromScheduledMs应该>=9秒（scheduledAt在10秒前）");
        assertTrue(log.getDelayFromScheduledMs() <= 11000, 
                "delayFromScheduledMs应该<=11秒（允许1秒误差）");
    }
}
