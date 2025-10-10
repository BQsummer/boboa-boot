package com.bqsummer.common.vo.req.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFeedbackStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;         // NEW|IN_PROGRESS|RESOLVED|REJECTED

    @Size(max = 500, message = "处理备注不能超过500字符")
    private String remark;         // 处理备注
}
