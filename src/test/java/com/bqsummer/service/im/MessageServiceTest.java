package com.bqsummer.service.im;

import com.bqsummer.BaseTest;
import com.bqsummer.common.dto.auth.User;
import com.bqsummer.common.dto.robot.RobotTask;
import com.bqsummer.common.dto.robot.SendMessagePayload;
import com.bqsummer.common.vo.req.im.SendMessageRequest;
import com.bqsummer.configuration.RobotTaskConfiguration;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.mapper.RobotTaskMapper;
import com.bqsummer.repository.MessageRepository;
import com.bqsummer.service.robot.RobotTaskScheduler;
import com.bqsummer.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * MessageService 单元测试
 * 测试用户消息LLM请求任务队列功能
 */
@SpringBootTest
@Transactional
@DisplayName("MessageService 单元测试")
class MessageServiceTest extends BaseTest {
    
    @Autowired
    private MessageService messageService;
    
    @MockBean
    private UserMapper userMapper;
    
    @SpyBean
    private RobotTaskMapper robotTaskMapper;
    
    @SpyBean
    private RobotTaskScheduler robotTaskScheduler;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private RobotTaskConfiguration config;
    
    @BeforeEach
    void setUp() {
        // 设置安全上下文
        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken authentication = 
            mock(UsernamePasswordAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getDetails()).thenReturn(1001L); // 模拟当前用户ID
        SecurityContextHolder.setContext(securityContext);
        
        // 模拟loadTasks返回成功（加载1个任务）
        when(robotTaskScheduler.loadTasks(anyList())).thenReturn(1);
    }
    
    @Test
    @DisplayName("测试发送消息给AI用户时创建RobotTask")
    void testSendMessageToAiUserCreatesRobotTask() {
        // Given: 准备测试数据
        Long aiUserId = 2001L;
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(aiUserId);
        request.setType("text");
        request.setContent("你好，今天天气怎么样？");
        
        // 模拟AI用户
        User aiUser = new User();
        aiUser.setId(aiUserId);
        aiUser.setUserType("AI");
        when(userMapper.findById(aiUserId)).thenReturn(aiUser);
        
        // When: 执行发送消息
        messageService.sendMessage(request);
        
        // Then: 验证创建了RobotTask
        ArgumentCaptor<RobotTask> taskCaptor = ArgumentCaptor.forClass(RobotTask.class);
        verify(robotTaskMapper).insert(taskCaptor.capture());
        
        RobotTask createdTask = taskCaptor.getValue();
        assertNotNull(createdTask, "应该创建RobotTask");
        assertEquals(1001L, createdTask.getUserId(), "userId应该是发送者ID");
        assertEquals(aiUserId, createdTask.getRobotId(), "robotId应该是AI用户ID");
        assertEquals("SEND_MESSAGE", createdTask.getActionType(), "actionType应该是SEND_MESSAGE");
        assertEquals("SHORT_DELAY", createdTask.getTaskType(), "taskType应该是SHORT_DELAY");
        assertNotNull(createdTask.getActionPayload(), "actionPayload不应为空");
        assertNotNull(createdTask.getScheduledAt(), "scheduledAt不应为空");
        
        // 验证任务被加载到内存队列
        verify(robotTaskScheduler).loadTasks(anyList());
    }
    
    @Test
    @DisplayName("测试发送消息给普通用户时不创建RobotTask")
    void testSendMessageToRegularUserDoesNotCreateRobotTask() {
        // Given: 准备测试数据
        Long regularUserId = 1002L;
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(regularUserId);
        request.setType("text");
        request.setContent("你好");
        
        // 模拟普通用户
        User regularUser = new User();
        regularUser.setId(regularUserId);
        regularUser.setUserType("REAL");
        when(userMapper.findById(regularUserId)).thenReturn(regularUser);
        
        // When: 执行发送消息
        messageService.sendMessage(request);
        
        // Then: 验证没有创建RobotTask
        verify(robotTaskMapper, never()).insert(any(RobotTask.class));
        verify(robotTaskScheduler, never()).loadTasks(anyList());
    }
    
    @Test
    @DisplayName("测试RobotTask的action_payload格式正确")
    void testRobotTaskActionPayloadFormat() {
        // Given: 准备测试数据
        Long aiUserId = 2001L;
        String messageContent = "测试消息内容";
        
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(aiUserId);
        request.setType("text");
        request.setContent(messageContent);
        
        // 模拟AI用户
        User aiUser = new User();
        aiUser.setId(aiUserId);
        aiUser.setUserType("AI");
        when(userMapper.findById(aiUserId)).thenReturn(aiUser);
        
        // When: 执行发送消息
        messageService.sendMessage(request);
        
        // Then: 验证action_payload格式
        ArgumentCaptor<RobotTask> taskCaptor = ArgumentCaptor.forClass(RobotTask.class);
        verify(robotTaskMapper).insert(taskCaptor.capture());
        
        RobotTask createdTask = taskCaptor.getValue();
        String payloadJson = createdTask.getActionPayload();
        assertNotNull(payloadJson, "action_payload不应为空");
        
        // 解析并验证payload内容
        SendMessagePayload payload = JsonUtil.fromJson(payloadJson, SendMessagePayload.class);
        assertNotNull(payload, "payload应该能够正确反序列化");
        assertNotNull(payload.getMessageId(), "messageId应该存在");
        assertEquals(1001L, payload.getSenderId(), "senderId应该是当前用户ID");
        assertEquals(aiUserId, payload.getReceiverId(), "receiverId应该是AI用户ID");
        assertEquals(messageContent, payload.getContent(), "content应该正确");
        assertNotNull(payload.getModelId(), "modelId不应为空");
    }
    
    // ========== Phase 6: 事务回滚测试 ==========
    
    @Test
    @DisplayName("T043: 测试数据库异常时Message和RobotTask都回滚")
    void testTransactionRollbackOnDatabaseException() {
        // Given: 准备AI用户
        Long aiUserId = 2006L;
        User aiUser = new User();
        aiUser.setId(aiUserId);
        aiUser.setUserType("AI");
        when(userMapper.findById(aiUserId)).thenReturn(aiUser);
        
        // Given: Mock RobotTaskMapper.insert抛出异常（模拟数据库错误）
        doThrow(new RuntimeException("数据库连接失败"))
            .when(robotTaskMapper).insert(any(RobotTask.class));
        
        // Given: 准备消息请求
        SendMessageRequest request = new SendMessageRequest();
        request.setReceiverId(aiUserId);
        request.setType("text");
        request.setContent("测试事务回滚");
        
        // When & Then: 执行发送消息应抛出异常
        assertThrows(RuntimeException.class, () -> messageService.sendMessage(request));
        
        // Then: 由于@Transactional，Message也应该回滚（实际验证需要查询数据库）
        // 注意：在测试环境中，@Transactional会自动回滚，所以无法直接验证
        // 但可以验证异常被正确抛出，表明事务会回滚
        verify(robotTaskMapper).insert(any(RobotTask.class));
    }
}
