package com.bqsummer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.dto.feedback.Feedback;
import com.bqsummer.common.vo.req.feedback.SubmitFeedbackRequest;
import com.bqsummer.mapper.FeedbackMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService Unit Tests")
class FeedbackServiceTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private FeedbackService feedbackService;

    private SubmitFeedbackRequest mockRequest;
    private Feedback mockFeedback;

    @BeforeEach
    void setUp() {
        mockRequest = createMockSubmitRequest();
        mockFeedback = createMockFeedback();
    }

    @Test
    @DisplayName("提交反馈 - 成功情况")
    void submit_Success() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(123L);
            return 1;
        });

        // When
        Long result = feedbackService.submit(mockRequest, "192.168.1.1", "TestAgent/1.0");

        // Then
        assertEquals(123L, result);

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).insert(feedbackCaptor.capture());

        Feedback capturedFeedback = feedbackCaptor.getValue();
        assertEquals("bug", capturedFeedback.getType());
        assertEquals("Test content", capturedFeedback.getContent());
        assertEquals("test@example.com", capturedFeedback.getContact());
        assertEquals("NEW", capturedFeedback.getStatus());
        assertEquals(100L, capturedFeedback.getUserId());
        assertNotNull(capturedFeedback.getExtraData());

        // Verify JSON serialization calls
        verify(objectMapper, times(2)).writeValueAsString(any());
    }

    @Test
    @DisplayName("提交反馈 - JSON序列化失败")
    void submit_JsonSerializationFails() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(123L);
            return 1;
        });

        // When
        Long result = feedbackService.submit(mockRequest, "192.168.1.1", "TestAgent/1.0");

        // Then
        assertEquals(123L, result);

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).insert(feedbackCaptor.capture());

        Feedback capturedFeedback = feedbackCaptor.getValue();
        assertNull(capturedFeedback.getImages()); // Should be null due to serialization failure
        assertNull(capturedFeedback.getExtraData()); // Should be null due to serialization failure
    }

    @Test
    @DisplayName("提交反馈 - 空的extraData和clientInfo")
    void submit_NullExtraDataAndClientInfo() throws JsonProcessingException {
        // Given
        mockRequest.setExtraData(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"clientIp\":\"192.168.1.1\",\"userAgent\":\"TestAgent/1.0\"}");
        when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(123L);
            return 1;
        });

        // When
        Long result = feedbackService.submit(mockRequest, null, null);

        // Then
        assertEquals(123L, result);

        // Verify that mergeExtra was called with null values
        verify(objectMapper).writeValueAsString(argThat(map -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> actualMap = (Map<String, Object>) map;
            return actualMap.isEmpty(); // Should be empty when clientIp and userAgent are null
        }));
    }

    @Test
    @DisplayName("获取反馈列表 - 带所有过滤条件")
    void list_WithAllFilters() {
        // Given
        Page<Feedback> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Collections.singletonList(mockFeedback));
        when(feedbackMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        Page<Feedback> result = feedbackService.list("bug", "NEW", 100L, 1, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(mockFeedback, result.getRecords().get(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Feedback>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(feedbackMapper).selectPage(any(Page.class), queryCaptor.capture());
    }

    @Test
    @DisplayName("获取反馈列表 - 无过滤条件")
    void list_NoFilters() {
        // Given
        Page<Feedback> mockPage = new Page<>(1, 20);
        when(feedbackMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        Page<Feedback> result = feedbackService.list(null, null, null, 1, 20);

        // Then
        assertNotNull(result);
        verify(feedbackMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("搜索反馈列表 - 带关键词")
    void searchList_WithKeyword() {
        // Given
        Page<Feedback> mockPage = new Page<>(1, 20);
        mockPage.setRecords(Collections.singletonList(mockFeedback));
        when(feedbackMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        // When
        Page<Feedback> result = feedbackService.searchList("bug", "NEW", 100L, "crash", 1, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(feedbackMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取反馈详情 - 存在")
    void detail_Exists() {
        // Given
        when(feedbackMapper.selectById(1L)).thenReturn(mockFeedback);

        // When
        Feedback result = feedbackService.detail(1L);

        // Then
        assertEquals(mockFeedback, result);
        verify(feedbackMapper).selectById(1L);
    }

    @Test
    @DisplayName("获取反馈详情 - 不存在")
    void detail_NotExists() {
        // Given
        when(feedbackMapper.selectById(999L)).thenReturn(null);

        // When
        Feedback result = feedbackService.detail(999L);

        // Then
        assertNull(result);
        verify(feedbackMapper).selectById(999L);
    }

    @Test
    @DisplayName("更新反馈状态 - 成功")
    void updateStatus_Success() {
        // Given
        when(feedbackMapper.selectById(1L)).thenReturn(mockFeedback);
        when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

        // When
        int result = feedbackService.updateStatus(1L, "RESOLVED", "Fixed", 200L);

        // Then
        assertEquals(1, result);

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).updateById(feedbackCaptor.capture());

        Feedback updatedFeedback = feedbackCaptor.getValue();
        assertEquals("RESOLVED", updatedFeedback.getStatus());
        assertEquals("Fixed", updatedFeedback.getHandlerRemark());
        assertEquals(200L, updatedFeedback.getHandlerUserId());
    }

    @Test
    @DisplayName("更新反馈状态 - 反馈不存在")
    void updateStatus_FeedbackNotExists() {
        // Given
        when(feedbackMapper.selectById(999L)).thenReturn(null);

        // When
        int result = feedbackService.updateStatus(999L, "RESOLVED", "Fixed", 200L);

        // Then
        assertEquals(0, result);
        verify(feedbackMapper).selectById(999L);
        verify(feedbackMapper, never()).updateById(any(Feedback.class));
    }

    @Test
    @DisplayName("删除反馈 - 成功")
    void delete_Success() {
        // Given
        when(feedbackMapper.deleteById(1L)).thenReturn(1);

        // When
        int result = feedbackService.delete(1L);

        // Then
        assertEquals(1, result);
        verify(feedbackMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除反馈 - 不存在")
    void delete_NotExists() {
        // Given
        when(feedbackMapper.deleteById(999L)).thenReturn(0);

        // When
        int result = feedbackService.delete(999L);

        // Then
        assertEquals(0, result);
        verify(feedbackMapper).deleteById(999L);
    }

    @Test
    @DisplayName("获取统计信息")
    void getStats() {
        // Given
        when(feedbackMapper.selectCount(isNull())).thenReturn(100L);
        when(feedbackMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(20L)  // NEW
                .thenReturn(30L)  // IN_PROGRESS
                .thenReturn(40L)  // RESOLVED
                .thenReturn(10L)  // REJECTED
                .thenReturn(25L)  // bug
                .thenReturn(20L)  // suggestion
                .thenReturn(20L)  // content
                .thenReturn(15L)  // ux
                .thenReturn(20L); // other

        // When
        Map<String, Object> result = feedbackService.getStats();

        // Then
        assertNotNull(result);
        assertEquals(100L, result.get("totalCount"));

        @SuppressWarnings("unchecked")
        Map<String, Long> statusStats = (Map<String, Long>) result.get("statusStats");
        assertNotNull(statusStats);
        assertEquals(20L, statusStats.get("NEW"));
        assertEquals(30L, statusStats.get("IN_PROGRESS"));
        assertEquals(40L, statusStats.get("RESOLVED"));
        assertEquals(10L, statusStats.get("REJECTED"));

        @SuppressWarnings("unchecked")
        Map<String, Long> typeStats = (Map<String, Long>) result.get("typeStats");
        assertNotNull(typeStats);
        assertEquals(25L, typeStats.get("bug"));
        assertEquals(20L, typeStats.get("suggestion"));

        // Verify all selectCount calls
        verify(feedbackMapper, times(10)).selectCount(any());
    }

    @Test
    @DisplayName("批量更新状态 - 成功")
    void batchUpdateStatus_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(feedbackMapper.selectById(anyLong())).thenReturn(mockFeedback);
        when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

        // When
        int result = feedbackService.batchUpdateStatus(ids, "RESOLVED", "Batch fixed", 200L);

        // Then
        assertEquals(3, result);
        verify(feedbackMapper, times(3)).selectById(anyLong());
        verify(feedbackMapper, times(3)).updateById(any(Feedback.class));
    }

    @Test
    @DisplayName("批量更新状态 - 部分成功")
    void batchUpdateStatus_PartialSuccess() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 999L); // 999L不存在
        when(feedbackMapper.selectById(1L)).thenReturn(mockFeedback);
        when(feedbackMapper.selectById(2L)).thenReturn(mockFeedback);
        when(feedbackMapper.selectById(999L)).thenReturn(null);
        when(feedbackMapper.updateById(any(Feedback.class))).thenReturn(1);

        // When
        int result = feedbackService.batchUpdateStatus(ids, "RESOLVED", "Batch fixed", 200L);

        // Then
        assertEquals(2, result); // 只有2个成功更新
        verify(feedbackMapper, times(3)).selectById(anyLong());
        verify(feedbackMapper, times(2)).updateById(any(Feedback.class)); // 只调用2次更新
    }

    @Test
    @DisplayName("批量删除 - 成功")
    @SuppressWarnings("deprecation")
    void batchDelete_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(feedbackMapper.deleteBatchIds(ids)).thenReturn(3);

        // When
        int result = feedbackService.batchDelete(ids);

        // Then
        assertEquals(3, result);
        verify(feedbackMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除 - 部分成功")
    @SuppressWarnings("deprecation")
    void batchDelete_PartialSuccess() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 999L);
        when(feedbackMapper.deleteBatchIds(ids)).thenReturn(2); // 只删除了2个

        // When
        int result = feedbackService.batchDelete(ids);

        // Then
        assertEquals(2, result);
        verify(feedbackMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("获取用户反馈详情 - 成功")
    void getUserFeedback_Success() {
        // Given
        Feedback userFeedback = createMockFeedback();
        userFeedback.setUserId(100L);
        when(feedbackMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userFeedback);

        // When
        Feedback result = feedbackService.getUserFeedback(1L, 100L);

        // Then
        assertEquals(userFeedback, result);
        verify(feedbackMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取用户反馈详情 - 不存在或非本人")
    void getUserFeedback_NotFoundOrNotOwner() {
        // Given
        when(feedbackMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        Feedback result = feedbackService.getUserFeedback(1L, 100L);

        // Then
        assertNull(result);
        verify(feedbackMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    // Helper methods
    private SubmitFeedbackRequest createMockSubmitRequest() {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setType("bug");
        request.setContent("Test content");
        request.setContact("test@example.com");
        request.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        request.setAppVersion("1.0.0");
        request.setOsVersion("iOS 15");
        request.setDeviceModel("iPhone 13");
        request.setNetworkType("wifi");
        request.setPageRoute("/test");
        request.setUserId(100L);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("custom", "value");
        request.setExtraData(extraData);

        return request;
    }

    private Feedback createMockFeedback() {
        Feedback feedback = new Feedback();
        feedback.setId(1L);
        feedback.setType("bug");
        feedback.setContent("Test content");
        feedback.setStatus("NEW");
        feedback.setUserId(100L);
        return feedback;
    }
}
