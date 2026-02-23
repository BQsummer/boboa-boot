package com.bqsummer.common.vo.req.im;

import lombok.Data;

@Data
public class RegenerateLastReplyRequest {

    /**
     * Optional edited content for the latest user message before regenerate.
     */
    private String editedUserContent;
}
