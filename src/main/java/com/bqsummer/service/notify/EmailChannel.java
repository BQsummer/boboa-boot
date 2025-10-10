package com.bqsummer.service.notify;

import com.bqsummer.configuration.MessageCenterProperties;
import com.bqsummer.constant.ChannelType;
import com.bqsummer.framework.exception.SnorlaxServerException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
@Slf4j
public class EmailChannel extends Channel {

    @Autowired
    private MessageCenterProperties properties;

    @Override
    ChannelType getType() {
        return ChannelType.EMAIL;
    }

    @Override
    boolean doSend(NotifyUser user, Template template) {
        if (!(template instanceof EmailTemplate)) {
            throw new SnorlaxServerException("template type error, current: " + template.getClass().getSimpleName());
        }
        if (user == null || StringUtils.isBlank(user.getEmailAddress())) {
            throw new SnorlaxServerException("email channel error, email is empty");
        }
        EmailTemplate emailTemplate = (EmailTemplate) template;
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", properties.getSmtpHost());
        props.put("mail.smtp.port", properties.getSmtpPort());
        props.put("mail.user", properties.getAccount());
        props.put("mail.password", properties.getPassword());
        // int类型可能不会生效
        props.put("mail.smtp.timeout", properties.getSmtpTimeout());
        props.put("mail.smtp.connectiontimeout", properties.getSmtpConnectTimeout());

        Session mailSession = Session.getInstance(props, new EmailAuthenticator(properties.getAccount(), properties.getPassword()));
        MimeMessage message = new MimeMessage(mailSession);

        try {
            //设置发件人
            InternetAddress form = new InternetAddress(properties.getAccount());
            message.setFrom(form);
            //设置收件人
            InternetAddress to = new InternetAddress(user.getEmailAddress());
            to.setPersonal(user.getEmailAddress());
            message.setRecipient(Message.RecipientType.TO, to);
            message.setSubject(emailTemplate.getTitle(), StandardCharsets.UTF_8.toString());
            message.setContent(emailTemplate.getEmailBody(), "text/html;charset=UTF-8");

            //发送邮件
            Transport.send(message);
        } catch (Exception e) {
            log.error("email channel send failed. user = {}, title = {}", user, emailTemplate.getTitle(), e);
            throw new SnorlaxServerException("email channel send failed");
        }
        return true;
    }

    public static class EmailAuthenticator extends Authenticator {

        private final String account;

        private final String password;

        public EmailAuthenticator(String account, String password) {
            this.account = account;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(getAccount(), getPassword());
        }

        public String getAccount() {
            return account;
        }

        public String getPassword() {
            return password;
        }

    }
}
