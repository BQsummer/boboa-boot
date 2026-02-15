package com.bqsummer.common.vo.req.system;

import lombok.Data;

@Data
public class UpdateSystemFileReq {
    private String key;
    private String fileName;
    private String category;
}
