package com.ecocea.passepartout.service;

import org.springframework.scheduling.annotation.Async;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface MailService {

    void sendHerokuUpdateError(String appName, Exception e);

    void sendGitlabUpdateError(String appName, Exception e);

    void sendDeletedOldKey(String userName, List<String> keys);

    @Async
    void sendDeleteCurrentKey(String userName);

    void sendAllKeysExpired(String name);

    void sendGeneratedNewKey(String name, String accessKeyId, String userName);

    void sendMessageFromTemplate(List<String> to, String from, String subject, String templateName, Map<String, Object> params);

    void sendMessage(List<String> to, String from, String subject, String content);

    void sendMessageWithAttachment(List<String> to, String from, String subject, String content, String attachmentName, InputStream attachment);
}
