package com.bqsummer.plugin.message.center;

import com.alibaba.fastjson2.JSON;
import com.bqsummer.framework.http.HttpClientTemplate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
public abstract class Channel {

    abstract ChannelType getType();

    @Autowired
    @Setter
    protected HttpClientTemplate httpClientTemplate;

    public void send(List<NotifyUser> notifyUsers, Template template) {
        if (!CollectionUtils.isEmpty(notifyUsers)) {
            for (NotifyUser user : notifyUsers) {
                try {
                    boolean isSuccess = doSend(user, template);
                } catch (Exception e) {
                    log.error("send notify channel failed. user = {}, channel = {}", user, JSON.toJSONString(this));
                }
            }
        }
    }

    abstract boolean doSend(NotifyUser user, Template template);
}
