package com.bqsummer.common.vo.req.kb;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class KbEntryCreateRequest {

    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    private Boolean enabled = true;

    private Integer priority = 0;

    @NotBlank(message = "template cannot be blank")
    private String template;

    private Map<String, Object> params;

    private String contextScope = "LAST_USER";

    @Min(value = 1, message = "lastN must be >= 1")
    @Max(value = 100, message = "lastN must be <= 100")
    private Integer lastN = 1;

    private Boolean alwaysEnabled = false;

    private String keywords;

    private String keywordMode = "CONTAINS";

    private Boolean vectorEnabled = false;

    @DecimalMin(value = "0.000000", message = "vectorThreshold must be >= 0")
    @DecimalMax(value = "1.000000", message = "vectorThreshold must be <= 1")
    private BigDecimal vectorThreshold = new BigDecimal("0.800000");

    @Min(value = 1, message = "vectorTopK must be >= 1")
    @Max(value = 100, message = "vectorTopK must be <= 100")
    private Integer vectorTopK = 5;

    @DecimalMin(value = "0.0000", message = "probability must be >= 0")
    @DecimalMax(value = "1.0000", message = "probability must be <= 1")
    private BigDecimal probability = new BigDecimal("1.0000");
}
