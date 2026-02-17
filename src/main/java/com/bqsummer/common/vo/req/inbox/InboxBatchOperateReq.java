package com.bqsummer.common.vo.req.inbox;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InboxBatchOperateReq {

    @NotEmpty
    private List<Long> messageIds;
}
