package com.bqsummer.common.vo.req.inbox;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminCreateInboxMessageReq {

    @NotNull
    @Min(1)
    @Max(3)
    private Integer msgType;

    @Size(max = 100)
    private String title;

    @NotBlank
    private String content;

    private Long senderId;

    @Size(max = 50)
    private String bizType;

    private Long bizId;

    private Map<String, Object> extra;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer sendType;

    private List<Long> targetUserIds;
}
