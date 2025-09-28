package com.bqsummer.plugin.message.center;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageCenterService implements InitializingBean {

    @Autowired
    private List<Channel> allChannel;

    private Map<ChannelType, Channel> channelByType;

    public void sendTemplate(List<NotifyUser> notifyUsers, List<Template> templates) {
        if (!CollectionUtils.isEmpty(notifyUsers) && !CollectionUtils.isEmpty(templates)) {
            for (Template template : templates) {
                Channel channel = channelByType.get(template.getType());
                if (channel == null) {
                    log.error("channel type not found : " + template.getType());
                    continue;
                }
                channel.send(notifyUsers, template);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        channelByType = allChannel.stream().collect(Collectors.toMap(Channel::getType, Function.identity()));
    }
}
