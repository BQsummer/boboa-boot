package com.bqsummer.common.vo.req;

import lombok.Data;

@Data
public class CreateAiCharacterReq {
    private String name;
    private String imageUrl;
    private String author;
    private String visibility; // PUBLIC / PRIVATE
    private Integer status;    // 1/0
}

