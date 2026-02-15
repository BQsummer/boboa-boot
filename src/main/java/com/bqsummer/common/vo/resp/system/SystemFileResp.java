package com.bqsummer.common.vo.resp.system;

import lombok.Data;

@Data
public class SystemFileResp {
    private String fileName;
    private String fileKey;
    private String contentType;
    private Long sizeBytes;
    private String category;
    private String storageType;
    private String accessUrl;
}
