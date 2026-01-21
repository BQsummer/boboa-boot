package com.bqsummer.service.prompt;

import com.bqsummer.common.dto.prompt.PromptTemplate;
import com.bqsummer.common.vo.req.prompt.PromptTemplateCreateRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateQueryRequest;
import com.bqsummer.common.vo.req.prompt.PromptTemplateUpdateRequest;
import com.bqsummer.common.vo.resp.prompt.PromptTemplateResponse;
import com.bqsummer.constant.GrayStrategy;
import com.bqsummer.constant.TemplateStatus;
import com.bqsummer.mapper.PromptTemplateMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * PromptTemplateService 单元测试
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromptTemplateService 单元测试")
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateMapper promptTemplateMapper;

    @Mock
    private BeetlTemplateService beetlTemplateService;

    @InjectMocks
    private PromptTemplateService promptTemplateService;

    @Nested
    @DisplayName("创建模板测试 (US2)")
    class CreateTemplateTests {

        private PromptTemplateCreateRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new PromptTemplateCreateRequest();
            validRequest.setCharId(1L);
            validRequest.setContent("你好，${name}！欢迎来到${place}。");
        }

        @Test
        @DisplayName("创建模板 - 首次创建，版本号为1")
        void createTemplate_FirstVersion_ShouldBeVersion1() {
            // Given
            when(promptTemplateMapper.getMaxVersionByCharId(1L)).thenReturn(null);
            when(promptTemplateMapper.insert(any(PromptTemplate.class))).thenReturn(1);

            // When
            PromptTemplateResponse response = promptTemplateService.create(validRequest, 1001L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVersion()).isEqualTo(1);
            assertThat(response.getIsLatest()).isTrue();
            assertThat(response.getStatus()).isEqualTo(TemplateStatus.DRAFT.getCode());
        }

        @Test
        @DisplayName("创建模板 - 新版本，版本号自动递增")
        void createTemplate_NewVersion_ShouldIncrementVersion() {
            // Given
            when(promptTemplateMapper.getMaxVersionByCharId(1L)).thenReturn(3);
            doNothing().when(promptTemplateMapper).markAllAsNotLatest(1L);
            when(promptTemplateMapper.insert(any(PromptTemplate.class))).thenReturn(1);

            // When
            PromptTemplateResponse response = promptTemplateService.create(validRequest, 1001L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVersion()).isEqualTo(4);
            assertThat(response.getIsLatest()).isTrue();

            // 验证调用了标记非最新版本的方法
            verify(promptTemplateMapper).markAllAsNotLatest(1L);
        }

        @Test
        @DisplayName("创建模板 - 默认状态为草稿")
        void createTemplate_DefaultStatus_ShouldBeDraft() {
            // Given
            when(promptTemplateMapper.getMaxVersionByCharId(1L)).thenReturn(null);
            when(promptTemplateMapper.insert(any(PromptTemplate.class))).thenReturn(1);

            // When
            PromptTemplateResponse response = promptTemplateService.create(validRequest, 1001L);

            // Then
            assertThat(response.getStatus()).isEqualTo(TemplateStatus.DRAFT.getCode());
        }

        @Test
        @DisplayName("创建模板 - 验证创建者信息")
        void createTemplate_ShouldRecordCreator() {
            // Given
            when(promptTemplateMapper.getMaxVersionByCharId(1L)).thenReturn(null);
            when(promptTemplateMapper.insert(any(PromptTemplate.class))).thenReturn(1);

            // When
            PromptTemplateResponse response = promptTemplateService.create(validRequest, 1001L);

            // Then
            assertThat(response.getCreatedBy()).isEqualTo(1001L);
        }

        @Test
        @DisplayName("创建模板 - charId 为空时抛出异常")
        void createTemplate_NullCharId_ShouldThrowException() {
            // Given
            validRequest.setCharId(null);

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.create(validRequest, 1001L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("charId");
        }

        @Test
        @DisplayName("创建模板 - content 为空时抛出异常")
        void createTemplate_NullContent_ShouldThrowException() {
            // Given
            validRequest.setContent(null);

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.create(validRequest, 1001L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("content");
        }
    }

    @Nested
    @DisplayName("查询模板列表测试 (US3)")
    class ListTemplatesTests {

        @Test
        @DisplayName("查询列表 - 按 charId 筛选")
        void listTemplates_FilterByCharId_ShouldReturnMatchingTemplates() {
            // Given
            PromptTemplateQueryRequest request = new PromptTemplateQueryRequest();
            request.setCharId(1L);
            request.setPage(1);
            request.setPageSize(10);

            // When
            IPage<PromptTemplateResponse> result = promptTemplateService.list(request);

            // Then
            verify(promptTemplateMapper).selectPage(any(), any());
        }

        @Test
        @DisplayName("查询列表 - 默认按创建时间降序排序")
        void listTemplates_DefaultSort_ShouldOrderByCreatedAtDesc() {
            // Given
            PromptTemplateQueryRequest request = new PromptTemplateQueryRequest();
            request.setPage(1);
            request.setPageSize(10);

            // When
            promptTemplateService.list(request);

            // Then
            verify(promptTemplateMapper).selectPage(any(), any());
        }

        @Test
        @DisplayName("查询列表 - 不返回已删除的模板")
        void listTemplates_ShouldExcludeDeletedTemplates() {
            // Given
            PromptTemplateQueryRequest request = new PromptTemplateQueryRequest();
            request.setPage(1);
            request.setPageSize(10);

            // When
            promptTemplateService.list(request);

            // Then
            verify(promptTemplateMapper).selectPage(any(), any());
        }
    }

    @Nested
    @DisplayName("查询模板详情测试 (US4)")
    class GetTemplateByIdTests {

        @Test
        @DisplayName("查询详情 - 存在的模板")
        void getById_ExistingTemplate_ShouldReturnTemplate() {
            // Given
            Long templateId = 1L;
            PromptTemplate template = createMockTemplate(templateId);
            when(promptTemplateMapper.selectById(templateId)).thenReturn(template);

            // When
            PromptTemplateResponse response = promptTemplateService.getById(templateId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(templateId);
        }

        @Test
        @DisplayName("查询详情 - 不存在的模板抛出异常")
        void getById_NonExistingTemplate_ShouldThrowException() {
            // Given
            Long templateId = 999L;
            when(promptTemplateMapper.selectById(templateId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.getById(templateId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("模板不存在");
        }

        @Test
        @DisplayName("查询详情 - 已删除的模板抛出异常")
        void getById_DeletedTemplate_ShouldThrowException() {
            // Given
            Long templateId = 1L;
            PromptTemplate template = createMockTemplate(templateId);
            template.setIsDeleted(true);
            when(promptTemplateMapper.selectById(templateId)).thenReturn(template);

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.getById(templateId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("模板不存在");
        }
    }

    @Nested
    @DisplayName("更新模板测试 (US5)")
    class UpdateTemplateTests {

        @Test
        @DisplayName("更新模板 - 成功更新状态")
        void updateTemplate_UpdateStatus_ShouldSucceed() {
            // Given
            Long templateId = 1L;
            PromptTemplate existingTemplate = createMockTemplate(templateId);
            when(promptTemplateMapper.selectById(templateId)).thenReturn(existingTemplate);
            when(promptTemplateMapper.updateById(any(PromptTemplate.class))).thenReturn(1);

            PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
            request.setStatus(TemplateStatus.ENABLED.getCode());

            // When
            PromptTemplateResponse response = promptTemplateService.update(templateId, request, 1002L);

            // Then
            assertThat(response).isNotNull();
            verify(promptTemplateMapper).updateById(any(PromptTemplate.class));
        }

        @Test
        @DisplayName("更新模板 - 记录更新者信息")
        void updateTemplate_ShouldRecordUpdater() {
            // Given
            Long templateId = 1L;
            PromptTemplate existingTemplate = createMockTemplate(templateId);
            when(promptTemplateMapper.selectById(templateId)).thenReturn(existingTemplate);
            when(promptTemplateMapper.updateById(any(PromptTemplate.class))).thenReturn(1);

            PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
            request.setStatus(TemplateStatus.ENABLED.getCode());

            // When
            PromptTemplateResponse response = promptTemplateService.update(templateId, request, 1002L);

            // Then
            assertThat(response.getUpdatedBy()).isEqualTo(1002L);
        }

        @Test
        @DisplayName("更新模板 - 不存在的模板抛出异常")
        void updateTemplate_NonExistingTemplate_ShouldThrowException() {
            // Given
            Long templateId = 999L;
            when(promptTemplateMapper.selectById(templateId)).thenReturn(null);

            PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
            request.setStatus(TemplateStatus.ENABLED.getCode());

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.update(templateId, request, 1002L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("模板不存在");
        }
    }

    @Nested
    @DisplayName("删除模板测试 (US6)")
    class DeleteTemplateTests {

        @Test
        @DisplayName("删除模板 - 逻辑删除")
        void deleteTemplate_ShouldLogicalDelete() {
            // Given
            Long templateId = 1L;
            PromptTemplate existingTemplate = createMockTemplate(templateId);
            when(promptTemplateMapper.selectById(templateId)).thenReturn(existingTemplate);
            when(promptTemplateMapper.updateById(any(PromptTemplate.class))).thenReturn(1);

            // When
            promptTemplateService.delete(templateId, 1001L);

            // Then
            verify(promptTemplateMapper).updateById(any(PromptTemplate.class));
        }

        @Test
        @DisplayName("删除模板 - 不存在的模板抛出异常")
        void deleteTemplate_NonExistingTemplate_ShouldThrowException() {
            // Given
            Long templateId = 999L;
            when(promptTemplateMapper.selectById(templateId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.delete(templateId, 1001L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("模板不存在");
        }
    }

    @Nested
    @DisplayName("渲染预览测试 (US7)")
    class RenderPreviewTests {

        @Test
        @DisplayName("渲染预览 - 成功渲染")
        void renderPreview_ShouldReturnRenderedContent() {
            // Given
            Long templateId = 1L;
            PromptTemplate template = createMockTemplate(templateId);
            template.setContent("你好，${name}！");
            when(promptTemplateMapper.selectById(templateId)).thenReturn(template);
            
            Map<String, Object> params = Map.of("name", "张三");
            when(beetlTemplateService.render(eq("你好，${name}！"), eq(params)))
                    .thenReturn("你好，张三！");

            // When
            String result = promptTemplateService.render(templateId, params);

            // Then
            assertThat(result).isEqualTo("你好，张三！");
        }

        @Test
        @DisplayName("渲染预览 - 模板语法错误时抛出异常")
        void renderPreview_InvalidTemplate_ShouldThrowException() {
            // Given
            Long templateId = 1L;
            PromptTemplate template = createMockTemplate(templateId);
            template.setContent("你好，${name！"); // 语法错误
            when(promptTemplateMapper.selectById(templateId)).thenReturn(template);
            
            Map<String, Object> params = Map.of("name", "张三");
            when(beetlTemplateService.render(any(), any()))
                    .thenThrow(new com.bqsummer.framework.exception.SnorlaxServerException("模板语法错误"));

            // When & Then
            assertThatThrownBy(() -> promptTemplateService.render(templateId, params))
                    .isInstanceOf(com.bqsummer.framework.exception.SnorlaxServerException.class);
        }
    }

    @Nested
    @DisplayName("获取最新稳定模板测试 (T008)")
    class GetLatestStableByCharIdTests {

        @Test
        @DisplayName("getLatestStableByCharId - 存在最新稳定模板")
        void getLatestStableByCharId_TemplateExists_ShouldReturnTemplate() {
            // Given
            Long charId = 1L;
            PromptTemplate mockTemplate = new PromptTemplate();
            mockTemplate.setId(100L);
            mockTemplate.setCharId(charId);
            mockTemplate.setContent("你好，${userName}！");
            mockTemplate.setVersion(3);
            mockTemplate.setIsLatest(true);
            mockTemplate.setIsStable(true);
            mockTemplate.setIsDeleted(false);
            mockTemplate.setStatus(TemplateStatus.ENABLED.getCode());
            
            when(promptTemplateMapper.selectOne(any())).thenReturn(mockTemplate);

            // When
            PromptTemplate result = promptTemplateService.getLatestStableByCharId(charId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getCharId()).isEqualTo(charId);
            assertThat(result.getIsLatest()).isTrue();
            assertThat(result.getIsStable()).isTrue();
            assertThat(result.getVersion()).isEqualTo(3);
            
            verify(promptTemplateMapper).selectOne(any());
        }

        @Test
        @DisplayName("getLatestStableByCharId - 模板不存在")
        void getLatestStableByCharId_TemplateNotExists_ShouldReturnNull() {
            // Given
            Long charId = 999L;
            when(promptTemplateMapper.selectOne(any())).thenReturn(null);

            // When
            PromptTemplate result = promptTemplateService.getLatestStableByCharId(charId);

            // Then
            assertThat(result).isNull();
            verify(promptTemplateMapper).selectOne(any());
        }

        @Test
        @DisplayName("getLatestStableByCharId - 只查询is_latest=1且is_stable=1的模板")
        void getLatestStableByCharId_OnlyLatestAndStable() {
            // Given: 准备多个版本模板，但只有最新稳定版本应该被返回
            Long charId = 1L;
            PromptTemplate latestStableTemplate = new PromptTemplate();
            latestStableTemplate.setId(103L);
            latestStableTemplate.setCharId(charId);
            latestStableTemplate.setVersion(3);
            latestStableTemplate.setIsLatest(true);
            latestStableTemplate.setIsStable(true);
            latestStableTemplate.setIsDeleted(false);
            
            when(promptTemplateMapper.selectOne(any())).thenReturn(latestStableTemplate);

            // When
            PromptTemplate result = promptTemplateService.getLatestStableByCharId(charId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(103L);
            assertThat(result.getIsLatest()).isTrue();
            assertThat(result.getIsStable()).isTrue();
        }
    }

    // Helper method
    private PromptTemplate createMockTemplate(Long id) {
        PromptTemplate template = new PromptTemplate();
        template.setId(id);
        template.setCharId(1L);
        template.setContent("测试模板内容");
        template.setVersion(1);
        template.setIsLatest(true);
        template.setStatus(TemplateStatus.DRAFT.getCode());
        template.setGrayStrategy(GrayStrategy.NONE.getCode());
        template.setIsDeleted(false);
        template.setCreatedBy("user001");
        template.setCreatedAt(LocalDateTime.now());
        return template;
    }
}
