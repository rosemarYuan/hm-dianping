package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class MailClient {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送简单邮件 (异步执行)
     * @Async 注解声明该方法会在独立的线程中执行，不会阻塞主线程
     */
    @Async
    public void sendMailAsync(String to, String subject, String content) {
        try {
            log.info("开始异步发送邮件至: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("邮件发送成功！收件人: {}", to);
        } catch (Exception e) {
            log.error("邮件发送失败, 收件人: " + to, e);
            // 注意：异步方法中的异常通常需要单独记录，因为主线程捕获不到
        }
    }
}