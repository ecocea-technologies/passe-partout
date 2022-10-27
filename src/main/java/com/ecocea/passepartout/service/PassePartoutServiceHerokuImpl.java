package com.ecocea.passepartout.service;

import com.ecocea.passepartout.Constants;
import com.ecocea.passepartout.config.properties.HerokuProperties;
import com.ecocea.passepartout.config.properties.PassePartoutProperties;
import com.ecocea.passepartout.service.object.AwsServiceInfo;
import com.ecocea.passepartout.service.object.Properties;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.iam.model.IamException;

import javax.annotation.Priority;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Priority(2)
@Profile("heroku")
public class PassePartoutServiceHerokuImpl implements PassePartoutService<Map<String, String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassePartoutServiceHerokuImpl.class);

    private final PassePartoutProperties properties;

    private final MailService mailService;

    public PassePartoutServiceHerokuImpl(PassePartoutProperties properties, MailService mailService) {
        this.properties = properties;
        this.mailService = mailService;
    }

    @Override
    public Optional<AwsServiceInfo> getAwsService() {
        if (!checkProperties()) {
            return Optional.empty();
        }

        HerokuProperties herokuProperties = this.properties.getHeroku();

        Map<String, String> vars = getHerokuConfigVar(herokuProperties.getToken(), herokuProperties.getSourceApplication());
        AwsService awsService;

        if (vars.containsKey(Constants.AWS_ACCESS_KEY_ID) && vars.containsKey(Constants.AWS_SECRET_ACCESS_KEY)) {
            awsService = new AwsServiceImpl(vars.get(Constants.AWS_ACCESS_KEY_ID), vars.get(Constants.AWS_SECRET_ACCESS_KEY));

            try {
                awsService.listAccessKeys();
            } catch (ExecutionException e) {
                if (IamException.class.equals(e.getCause().getClass())
                    && "InvalidClientTokenId".equals(((IamException) e.getCause()).awsErrorDetails().errorCode())) {
                    LOGGER.error("Invalid AWS credentials for Heroku project {}, this config will be ignored", getName());
                } else {
                    LOGGER.error("An error occurred while configuring Heroku project {}, this config will be ignored", getName(), e);
                }

                return Optional.empty();
            } catch (Exception e) {
                LOGGER.error("An error occurred while configuring Heroku project {}, this config will be ignored", getName(), e);
                return Optional.empty();
            }
        } else {
            LOGGER.warn("Heroku App {} does not have AWS vars, this config will be ignored", herokuProperties.getSourceApplication());
            return Optional.empty();
        }

        return Optional.of(new AwsServiceInfo(awsService, getName(), vars.get(Constants.AWS_ACCESS_KEY_ID)));
    }

    @Override
    public boolean checkProperties() {
        if (this.properties.getHeroku() == null) {
            return false;
        }

        if (StringUtils.isBlank(this.properties.getHeroku().getSourceApplication())) {
            return false;
        }

        if (StringUtils.isBlank(this.properties.getHeroku().getToken())) {
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Heroku: " + this.properties.getHeroku().getSourceApplication() + " (" + String.join(";", this.properties.getHeroku().getDispatchApplications())
               + ")";
    }

    @Override
    public Map<String, Map<String, String>> dispatchCredentials(String accessKeyId, String secretAccessKey) {
        HerokuProperties herokuProperties = this.properties.getHeroku();
        Map<String, Map<String, String>> toReturn = new HashMap<>();

        for (String appName : herokuProperties.getDispatchApplications()) {
            LOGGER.info("Changing credentials of app: {}", appName);
            Map<String, String> previousVars = null;

            try {
                previousVars = getHerokuConfigVar(herokuProperties.getToken(), appName);

                Map<String, String> vars = updateHerokuConfigVar(herokuProperties.getToken(), appName,
                                                                 Map.of(Constants.AWS_ACCESS_KEY_ID, accessKeyId, Constants.AWS_SECRET_ACCESS_KEY,
                                                                        secretAccessKey));
                if (MapUtils.isEmpty(vars) || !accessKeyId.equals(vars.get(Constants.AWS_ACCESS_KEY_ID)) || !secretAccessKey.equals(
                        vars.get(Constants.AWS_SECRET_ACCESS_KEY))) {
                    rollback(herokuProperties.getToken(), previousVars, appName);
                    this.mailService.sendHerokuUpdateError(appName, new IllegalArgumentException("Key Id or Secret does not match new one"));
                } else {
                    toReturn.put(appName, previousVars);
                }
            } catch (Exception e) {
                LOGGER.error("An error occurred while performing rotation for {}: {}!", appName, e.getMessage(), e);
                this.mailService.sendHerokuUpdateError(appName, e);

                if (previousVars != null) {
                    rollback(herokuProperties.getToken(), previousVars, appName);
                }

                throw e;
            }
        }

        return toReturn;
    }

    @Override
    public void rollBack(Map<String, ?> map) {
        map.forEach((appName, rollback) -> {
            LOGGER.info("Rollback for {}", appName);
            rollback(this.properties.getHeroku().getToken(), (Map<String, String>) rollback, appName);
        });
    }

    private Map<String, String> getHerokuConfigVar(String token, String appName) {
        Mono<Properties> response = getWebClient(token).get()
                                                       .uri(uriBuilder -> uriBuilder.path("/apps/" + appName + "/config-vars").build())
                                                       .retrieve()
                                                       .bodyToMono(Properties.class);

        return Objects.requireNonNullElse(response.block(), new Properties()).getProperties();
    }

    private Map<String, String> updateHerokuConfigVar(String token, String appName, Map<String, String> vars) {
        Mono<Properties> response = getWebClient(token).patch()
                                                       .uri(uriBuilder -> uriBuilder.path("/apps/" + appName + "/config-vars").build())
                                                       .body(Mono.just(vars), Map.class)
                                                       .retrieve()
                                                       .bodyToMono(Properties.class);

        return Objects.requireNonNullElse(response.block(), new Properties()).getProperties();
    }

    private void rollback(String token, Map<String, String> previousVars, String appName) {
        if (previousVars.containsKey(Constants.AWS_ACCESS_KEY_ID) && previousVars.containsKey(Constants.AWS_SECRET_ACCESS_KEY)) {
            updateHerokuConfigVar(token, appName,
                                  Map.of(Constants.AWS_ACCESS_KEY_ID, previousVars.get(Constants.AWS_ACCESS_KEY_ID),
                                         Constants.AWS_SECRET_ACCESS_KEY, previousVars.get(Constants.AWS_SECRET_ACCESS_KEY)));
        } else {
            Map<String, String> map = new HashMap<>();
            map.put(Constants.AWS_ACCESS_KEY_ID, null); //Null value delete config var
            map.put(Constants.AWS_SECRET_ACCESS_KEY, null);

            updateHerokuConfigVar(token, appName, map);
        }
    }

    private WebClient getWebClient(String token) {
        return WebClient.builder()
                        .baseUrl(Constants.HEROKU_BASE_URL)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, Constants.BEARER + token)
                        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.heroku+json; version=3")
                        .build();
    }
}
