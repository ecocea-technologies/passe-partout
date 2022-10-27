package com.ecocea.passepartout.service;

import com.ecocea.passepartout.config.properties.PassePartoutProperties;
import com.ecocea.passepartout.service.object.AwsServiceInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iam.model.AccessKey;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RunService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunService.class);

    private final PassePartoutProperties properties;

    private final MailService mailService;

    private final List<PassePartoutService<?>> services;

    public RunService(PassePartoutProperties properties, MailService mailService, List<PassePartoutService<?>> services) {
        this.mailService = mailService;
        this.properties = properties;
        this.services = services;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Optional<AwsServiceInfo> awsService = this.services.stream()
                                                           .map(PassePartoutService::getAwsService)
                                                           .filter(Optional::isPresent)
                                                           .findFirst()
                                                           .map(Optional::get);

        if (awsService.isEmpty()) {
            LOGGER.warn("No valid properties found!");
            return;
        }

        LOGGER.info("Checking keys for source {}!", awsService.get().getName());

        List<AccessKeyMetadata> keys = awsService.get().getAwsService().listAccessKeys();
        String userName = keys.iterator().next().userName();

        LOGGER.info("{} access keys retrieved for user {}!", keys.size(), userName);
        boolean dispatched = false;

        if (keys.stream().allMatch(key -> key.createDate().plus(this.properties.getGenerateAfter(), ChronoUnit.DAYS).isBefore(Instant.now()))) {
            AccessKey newKey = awsService.get().getAwsService().createAccessKey();

            keys.add(AccessKeyMetadata.builder()
                                      .accessKeyId(newKey.accessKeyId())
                                      .createDate(newKey.createDate())
                                      .status(newKey.status())
                                      .userName(newKey.userName())
                                      .build());

            Map<PassePartoutService<?>, Map<String, ?>> doneService = new HashMap<>();
            try {
                for (PassePartoutService<?> service : this.services) {
                    doneService.put(service, service.dispatchCredentials(newKey.accessKeyId(), newKey.secretAccessKey()));
                }

                dispatched = true;
                this.mailService.sendGeneratedNewKey(this.services.stream().map(PassePartoutService::getName).collect(Collectors.joining(" | ")),
                                                     newKey.accessKeyId(), newKey.userName());
            } catch (Exception e) {
                LOGGER.error("An error occurred while dispatching new keys for {}: {}!", e.getMessage(), userName, e);
                doneService.forEach(PassePartoutService::rollBack);
                awsService.get().getAwsService().deleteAccessKey(newKey.accessKeyId());
                keys.removeIf(key -> key.accessKeyId().equals(newKey.accessKeyId()));
            }
        } else {
            LOGGER.info("No need to generate new key for {}", userName);
        }

        if (keys.stream().allMatch(key -> key.createDate().plus(this.properties.getDeleteAfter(), ChronoUnit.DAYS).isBefore(Instant.now()))) {
            LOGGER.warn("All keys expired for {}!", userName);
            this.mailService.sendAllKeysExpired(userName);
        } else { //Else to prevent deleting all keys
            List<AccessKeyMetadata> toDelete = keys.stream()
                                                   .filter(key -> key.createDate()
                                                                     .plus(this.properties.getDeleteAfter(), ChronoUnit.DAYS)
                                                                     .isBefore(Instant.now()))
                                                   .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(toDelete)) {
                if (toDelete.stream().anyMatch(accessKeyMetadata -> awsService.get().getAccessKeyId().equals(accessKeyMetadata.accessKeyId()))) {
                    if (!dispatched) {
                        LOGGER.error("Trying to delete the used key {}! New key not dispatched to {}?", awsService.get().getAccessKeyId(),
                                     awsService.get().getName());
                        this.mailService.sendDeleteCurrentKey(userName);
                    } else {
                        LOGGER.warn("Current key used for {}, is expired but a new one has been dispatched, wait next run to delete!", userName);
                    }

                    return;
                }

                toDelete.forEach(accessKeyMetadata -> {
                    awsService.get().getAwsService().deleteAccessKey(accessKeyMetadata.accessKeyId());
                    LOGGER.warn("Deleted old key {} for {}!", accessKeyMetadata.accessKeyId(), accessKeyMetadata.userName());
                });

                this.mailService.sendDeletedOldKey(userName, toDelete.stream().map(AccessKeyMetadata::accessKeyId).collect(Collectors.toList()));
            } else {
                LOGGER.info("No need to delete any key for {}", userName);
            }
        }
    }
}
