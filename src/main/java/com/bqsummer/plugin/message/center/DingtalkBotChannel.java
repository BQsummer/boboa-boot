package com.bqsummer.plugin.message.center;

import com.alibaba.fastjson2.JSONObject;
import com.bqsummer.framework.exception.SnorlaxServerException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
    boolean doSend(NotifyUser user, Template template) {
        if (!(template instanceof TextDingtalkBotTemplate)) {
            throw new SnorlaxServerException("template type error. current type: " + template.getClass().getSimpleName());
        }
        if (user == null) {
            throw new SnorlaxServerException("notify user cannot be null");
        }
        if (StringUtils.isBlank(user.getDingtalkBotToken())) {
            throw new SnorlaxServerException("dingtalkBot token cannot be blank");
        }
        TextDingtalkBotTemplate textDingtalkBotTemplate = (TextDingtalkBotTemplate) template;
        String response = this.httpClientTemplate.doPostJson(buildUrl(user), textDingtalkBotTemplate.getMessageBody());
        log.info("dingtalk send result: user = {}, response = {}", user.getDingtalkBotToken(), response);
        return JSONObject.parseObject(response).getInteger("errcode") == 0;
    }

    private String buildUrl(NotifyUser user) {
        return DINGTALK_BOT_DOMAIN + user.getDingtalkBotToken();
    }


}
