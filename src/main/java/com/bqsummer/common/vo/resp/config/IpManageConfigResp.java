package com.bqsummer.common.vo.resp.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpManageConfigResp {
    private String ipWhiteList;
    private String ipBlackList;
}
