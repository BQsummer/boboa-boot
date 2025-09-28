package com.bqsummer.plugin.configplus.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateConfigReq {
    private Long id;
    private String env;
    private String application;
    private String name;
    private String desc;
    private String value;
    private String type;
    private String sensitive;
    private String catalog;
}
