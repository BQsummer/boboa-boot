package com.bqsummer.service.notify;

import com.alibaba.fastjson2.JSON;
import com.bqsummer.constant.ChannelType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class TextDingtalkBotTemplate extends Template {

    @Getter
    private String messageBody;

    public TextDingtalkBotTemplate(String messageBody) {
        this.messageBody = messageBody;
    }

    public String buildTextMessage() {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("content", this.messageBody);
        body.put("msgtype", "text");
        body.put("text", content);
        return JSON.toJSONString(body);
    }

    @Override
    ChannelType getType() {
        return ChannelType.DINGTALK;
    }
}
