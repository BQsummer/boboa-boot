package com.bqsummer.service.notify;

import com.alibaba.fastjson2.JSON;
import com.bqsummer.constant.ChannelType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class Channel {

    abstract ChannelType getType();

    @Autowired
    @Setter
    protected OkHttpClient okHttpClient;

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

    abstract boolean doSend(NotifyUser user, Template template) throws Exception;
}
