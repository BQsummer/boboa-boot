package com.bqsummer.service.prompt;

import com.bqsummer.common.dto.prompt.PromptTemplate;
import com.bqsummer.common.vo.req.prompt.PromptTemplateUpdateRequest;
import com.bqsummer.constant.TemplateStatus;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.PromptTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateDisableValidationTest {

    @Mock
    private PromptTemplateMapper promptTemplateMapper;

    @Mock
    private BeetlTemplateService beetlTemplateService;

    @InjectMocks
    private PromptTemplateService promptTemplateService;

    @Test
    void update_shouldRejectDisablingLastEnabledTemplate() {
        Long templateId = 1L;
        PromptTemplate existingTemplate = enabledTemplate(templateId, 10L);
        when(promptTemplateMapper.selectById(templateId)).thenReturn(existingTemplate);
        when(promptTemplateMapper.countEnabledByCharIdExcludingId(10L, TemplateStatus.ENABLED.getCode(), templateId))
                .thenReturn(0L);

        PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
        request.setStatus(TemplateStatus.DISABLED.getCode());

        assertThatThrownBy(() -> promptTemplateService.update(templateId, request, 1002L))
                .isInstanceOf(SnorlaxClientException.class);
        verify(promptTemplateMapper, never()).updateById(any(PromptTemplate.class));
    }

    @Test
    void update_shouldAllowDisablingWhenOtherEnabledTemplateExists() {
        Long templateId = 1L;
        PromptTemplate existingTemplate = enabledTemplate(templateId, 10L);
        when(promptTemplateMapper.selectById(templateId)).thenReturn(existingTemplate);
        when(promptTemplateMapper.countEnabledByCharIdExcludingId(10L, TemplateStatus.ENABLED.getCode(), templateId))
                .thenReturn(1L);
        when(promptTemplateMapper.updateById(any(PromptTemplate.class))).thenReturn(1);

        PromptTemplateUpdateRequest request = new PromptTemplateUpdateRequest();
        request.setStatus(TemplateStatus.DISABLED.getCode());

        Integer status = promptTemplateService.update(templateId, request, 1002L).getStatus();

        assertThat(status).isEqualTo(TemplateStatus.DISABLED.getCode());
        verify(promptTemplateMapper).updateById(any(PromptTemplate.class));
    }

    @Test
    void disable_shouldRejectDisablingLastEnabledTemplate() {
        Long templateId = 1L;
        PromptTemplate existingTemplate = enabledTemplate(templateId, 10L);
        when(promptTemplateMapper.selectById(templateId)).thenReturn(existingTemplate);
        when(promptTemplateMapper.countEnabledByCharIdExcludingId(10L, TemplateStatus.ENABLED.getCode(), templateId))
                .thenReturn(0L);

        assertThatThrownBy(() -> promptTemplateService.disable(templateId, 1002L))
                .isInstanceOf(SnorlaxClientException.class);
        verify(promptTemplateMapper, never()).updateById(any(PromptTemplate.class));
    }

    private PromptTemplate enabledTemplate(Long id, Long charId) {
        PromptTemplate template = new PromptTemplate();
        template.setId(id);
        template.setCharId(charId);
        template.setStatus(TemplateStatus.ENABLED.getCode());
        template.setIsDeleted(false);
        return template;
    }
}
