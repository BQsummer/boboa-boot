package com.bqsummer.service.notify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotifyUser {
    private String emailAddress;
    private String dingtalkBotToken;

    @Override
    public String toString() {
        return "NotifyUser{" +
                "emailAddress='" + emailAddress + '\'' +
                ", dingtalkBotToken='" + dingtalkBotToken + '\'' +
                '}';
    }
}
