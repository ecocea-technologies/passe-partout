package com.ecocea.passepartout.service;

import com.ecocea.passepartout.Constants;
import com.ecocea.passepartout.config.properties.PassePartoutProperties;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceImpl.class);

    protected final PassePartoutProperties properties;

    protected final TemplateEngine templateEngine;

    protected final SendGrid sendGrid;

    protected final MessageSource messageSource;

    public MailServiceImpl(PassePartoutProperties properties, TemplateEngine templateEngine, SendGrid sendGrid, MessageSource messageSource) {
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.sendGrid = sendGrid;
        this.messageSource = messageSource;
        ((SpringTemplateEngine) templateEngine).setMessageSource(messageSource);
    }

    @Async
    @Override
    public void sendHerokuUpdateError(String appName, Exception e) {
        sendMessageFromTemplate(this.properties.getEmail(), this.properties.getFrom(),
                                this.messageSource.getMessage("heroku.update-error.subject", null, Locale.FRANCE),
                                Constants.MAIL_TEMPLATE_UPDATE_ERROR, Map.of("appName", appName,
                                                                             "message", StringUtils.defaultIfBlank(e.getMessage(), ""),
                                                                             "stackTrace", ExceptionUtils.getStackTrace(e)));
    }

    @Async
    @Override
    public void sendGitlabUpdateError(String appName, Exception e) {
        sendMessageFromTemplate(this.properties.getEmail(), this.properties.getFrom(),
                                this.messageSource.getMessage("gitlab.update-error.subject", null, Locale.FRANCE),
                                Constants.MAIL_TEMPLATE_UPDATE_ERROR, Map.of("appName", appName,
                                                                             "message", StringUtils.defaultIfBlank(e.getMessage(), ""),
                                                                             "stackTrace", ExceptionUtils.getStackTrace(e)));
    }

    @Async
    @Override
    public void sendDeletedOldKey(String userName, List<String> keys) {
        sendMessageFromTemplate(this.properties.getEmail(), this.properties.getFrom(),
                                this.messageSource.getMessage("deleted-old-key.subject", null, Locale.FRANCE),
                                Constants.MAIL_TEMPLATE_DELETED_OLD_KEY, Map.of("userName", userName, "keys", keys));
    }

    @Async
    @Override
    public void sendDeleteCurrentKey(String userName) {
        sendMessageFromTemplate(this.properties.getEmail(), this.properties.getFrom(),
                                this.messageSource.getMessage("delete-current-key.subject", null, Locale.FRANCE),
                                Constants.MAIL_TEMPLATE_DELETE_CURRENT_KEY, Map.of("userName", userName));
    }

    @Async
    @Override
    public void sendAllKeysExpired(String name) {
        sendMessageFromTemplate(this.properties.getEmail(), this.properties.getFrom(),
                                this.messageSource.getMessage("all-keys-expired.subject", null, Locale.FRANCE),
                                Constants.MAIL_TEMPLATE_ALL_KEYS_EXPIRED, Map.of("name", name, "deleteAfter", this.properties.getDeleteAfter()));
    }

    @Async
    @Override
    public void sendGeneratedNewKey(String name, String accessKeyId, String userName) {
        sendMessageFromTemplate(this.properties.getEmail(), this.properties.getFrom(),
                                this.messageSource.getMessage("generated-new-key.subject", null, Locale.FRANCE),
                                Constants.MAIL_TEMPLATE_GENERATED_NEW_KEY,
                                Map.of("name", name, "generateAfter", this.properties.getGenerateAfter(), "accessKeyId", accessKeyId,
                                       "userName", userName));
    }

    @Override
    public void sendMessageFromTemplate(List<String> to, String from, String subject, String templateName, Map<String, Object> params) {
        sendMessage(to, from, subject, buildFromTemplate(templateName, params));
    }

    @Override
    public void sendMessage(List<String> to, String from, String subject, String content) {
        sendMessageWithAttachment(to, from, subject, content, null, null);
    }

    @Override
    public void sendMessageWithAttachment(List<String> to, String from, String subject, String content, String attachmentName, InputStream attachment) {
        try {
            Content content1 = new Content("text/html", content);
            Mail mail = new Mail();
            mail.setFrom(new Email(from));
            mail.setSubject("[PassePartout] " + subject);
            mail.addContent(content1);

            Personalization personalization = new Personalization();
            to.forEach(s -> personalization.addTo(new Email(s)));

            mail.addPersonalization(personalization);

            if (attachment != null && StringUtils.isNotBlank(attachmentName)) {
                mail.addAttachments(new Attachments.Builder(attachmentName, attachment).build());
            }

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = this.sendGrid.api(request);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                LOGGER.error("An error occurred while sending mail: {} !", response.getBody());
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred while sending mail: {} !", e.getMessage(), e);
        }
    }

    protected String buildFromTemplate(String templateName, Map<String, Object> params) {
        Context context = new Context();
        context.setVariables(params);
        context.setLocale(Locale.FRANCE);
        return this.templateEngine.process(Constants.MAIL_TEMPLATE_FOLDER + templateName, context);
    }
}
