package com.bqsummer.common.vo.resp.kb;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class KbEntryResponse {

    private Long id;

    private String title;

    private Boolean enabled;

    private Integer priority;

    private String template;

    private Map<String, Object> params;

    private String contextScope;

    private Integer lastN;

    private Boolean alwaysEnabled;

    private String keywords;

    private String keywordMode;

    private Boolean vectorEnabled;

    private BigDecimal vectorThreshold;

    private Integer vectorTopK;

    private BigDecimal probability;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
