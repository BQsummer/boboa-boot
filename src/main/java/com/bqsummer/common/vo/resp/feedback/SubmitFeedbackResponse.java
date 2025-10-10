package com.bqsummer.common.vo.resp.feedback;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitFeedbackResponse {
    private Long id;  // 创建的反馈ID
}
