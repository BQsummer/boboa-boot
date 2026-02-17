package com.bqsummer.common.vo.resp.inbox;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class InboxStatsResp {
    private Long totalMessages;
    private Long totalUserMessages;
    private Long readUserMessages;
    private Long unreadUserMessages;
    private Long deletedUserMessages;
    private Long todayBatchSendCount;
    private Map<Integer, Long> msgTypeStats;
}
