package com.bqsummer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bqsummer.common.vo.Response;
import com.bqsummer.common.dto.config.Config;
import com.bqsummer.common.vo.req.config.CreateConfigReq;
import com.bqsummer.common.vo.req.config.UpdateConfigReq;
import com.bqsummer.service.configplus.proxy.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "plugin-manager/config")
public class ConfigController {

    @Value("${spring.profiles.active}")
    private String env;

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private ConfigService configService;

    /**
     * 分页查询
     *
     * @param pageSize
     * @param pageNum
     * @param name
     * @param catalog
     * @return
     */
    @GetMapping("/configs")
    public Page<Config> getConfigByPage(@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                        @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                                        @RequestParam(name = "name", required = false) String name,
                                        @RequestParam(name = "catalog", required = false) String catalog) {
        return configService.getAllConfigByPage(pageNum, pageSize, Config.builder().name(name).catalog(catalog).build());
    }

    /**
     * 失效配置
     *
     * @param id
     * @return
     */
//    @PostMapping(name = "/disable/config/{id}")
//    public Response disableConfig(@PathVariable("id") Long id) {
//        configService.disableConfig(id);
//        return Response.success();
//    }

    /**
     * 新增配置
     *
     * @param req
     * @return
     */
    @PostMapping(name = "/config")
    public Response createConfig(CreateConfigReq req) {
        req.setEnv(env);
        req.setApplication(appName);
        configService.addConfig(req);
        return Response.success();
    }

    /**
     * 更新配置
     *
     * @param req
     * @return
     */
    @PutMapping(name = "/config")
    public Response updateConfig(UpdateConfigReq req) {
        req.setEnv(env);
        req.setApplication(appName);
        configService.updateValue(req);
        return Response.success();
    }

    /**
     * 获取config所有类型
     *
     * @return
     */
    @GetMapping("/configTypes")
    public Map<String, String> getAllConfigTypes() {
        return configService.getAllConfigType();
    }
}
