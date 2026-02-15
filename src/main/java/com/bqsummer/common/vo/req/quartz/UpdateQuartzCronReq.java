package com.bqsummer.common.vo.req.quartz;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateQuartzCronReq {

    @NotBlank(message = "cron expression cannot be blank")
    private String cronExpression;
}
