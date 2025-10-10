package com.bqsummer.common.vo.req.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchUpdateStatusRequest {
    @NotEmpty
    private List<Long> ids;

    @NotBlank
    @Pattern(regexp = "NEW|IN_PROGRESS|RESOLVED|REJECTED", message = "invalid status")
    private String status;

    @Size(max = 1024)
    private String remark;
}
