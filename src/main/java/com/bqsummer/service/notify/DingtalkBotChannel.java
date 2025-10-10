package com.bqsummer.service.notify;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.constant.ChannelType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service
public class DingtalkBotChannel extends Channel {

    public static final String DINGTALK_BOT_DOMAIN = "https://oapi.dingtalk.com/robot/send?access_token=";

    @Setter
    private TextDingtalkBotTemplate template;

    @Override
    ChannelType getType() {
        return ChannelType.DINGTALK;
    }

    @Override
    boolean doSend(NotifyUser user, Template template) throws Exception {
        if (!(template instanceof TextDingtalkBotTemplate textDingtalkBotTemplate)) {
            throw new Exception("template type error. current type: " + template.getClass().getSimpleName());
        }
        if (user == null) {
            throw new Exception("notify user cannot be null");
        }
        if (StringUtils.isBlank(user.getDingtalkBotToken())) {
            throw new Exception("dingtalkBot token cannot be blank");
        }
        String response = Objects.requireNonNull(okHttpClient.newCall(new Request.Builder()
                .url(buildUrl(user))
                .post(RequestBody.create(textDingtalkBotTemplate.getMessageBody(), MediaType.parse("application/json; charset=utf-8")))
                .build()).execute().body()).string();

        log.info("dingtalk send result: user = {}, response = {}", user.getDingtalkBotToken(), response);
        return JSONObject.parseObject(response).getInteger("errcode") == 0;
    }

    private String buildUrl(NotifyUser user) {
        return DINGTALK_BOT_DOMAIN + user.getDingtalkBotToken();
    }


}
