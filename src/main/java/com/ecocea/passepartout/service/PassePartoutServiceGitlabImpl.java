package com.ecocea.passepartout.service;

import com.ecocea.passepartout.Constants;
import com.ecocea.passepartout.config.properties.GitlabProperties;
import com.ecocea.passepartout.config.properties.PassePartoutProperties;
import com.ecocea.passepartout.service.object.AwsServiceInfo;
import com.ecocea.passepartout.service.object.GitlabVar;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.iam.model.IamException;

import javax.annotation.Priority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Priority(1)
@Profile("gitlab")
public class PassePartoutServiceGitlabImpl implements PassePartoutService<List<GitlabVar>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassePartoutServiceGitlabImpl.class);

    private final PassePartoutProperties properties;

    private final MailService mailService;

    public PassePartoutServiceGitlabImpl(PassePartoutProperties properties, MailService mailService) {
        this.properties = properties;
        this.mailService = mailService;
    }

    @Override
    public Optional<AwsServiceInfo> getAwsService() {
        if (!checkProperties()) {
            return Optional.empty();
        }

        AwsService awsService;
        Optional<GitlabVar> awsAccessKeyId = getVar(Constants.AWS_ACCESS_KEY_ID);
        Optional<GitlabVar> awsSecretAccessKey = getVar(Constants.AWS_SECRET_ACCESS_KEY);

        if (awsAccessKeyId.isPresent() && awsSecretAccessKey.isPresent()) {
            awsService = new AwsServiceImpl(awsAccessKeyId.get().getValue(), awsSecretAccessKey.get().getValue());

            try {
                awsService.listAccessKeys();
            } catch (ExecutionException e) {
                if (IamException.class.equals(e.getCause().getClass())
                    && "InvalidClientTokenId".equals(((IamException) e.getCause()).awsErrorDetails().errorCode())) {
                    LOGGER.error("Invalid AWS credentials for Gitlab project {}, this config will be ignored", getName());
                } else {
                    LOGGER.error("An error occurred while configuring Gitlab project {}, this config will be ignored", getName(), e);
                }

                return Optional.empty();
            } catch (Exception e) {
                LOGGER.error("An error occurred while configuring Gitlab project {}, this config will be ignored", getName(), e);
                return Optional.empty();
            }
        } else {
            LOGGER.warn("Gitlab project {} does not have AWS vars, this config will be ignored", getName());
            return Optional.empty();
        }

        return Optional.of(new AwsServiceInfo(awsService, getName(), awsAccessKeyId.get().getValue()));
    }

    @Override
    public boolean checkProperties() {
        if (this.properties.getGitlab() == null) {
            return false;
        }

        if (StringUtils.isBlank(this.properties.getGitlab().getEnvironment())) {
            return false;
        }

        if (StringUtils.isBlank(this.properties.getGitlab().getToken())) {
            return false;
        }

        if (StringUtils.isBlank(this.properties.getGitlab().getProjectId())) {
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Gitlab: " + this.properties.getGitlab().getProjectId() + " - " + this.properties.getGitlab().getEnvironment() +
               " (" + String.join(";", this.properties.getGitlab().getDispatchProjects()) + ")";
    }

    @Override
    public Map<String, List<GitlabVar>> dispatchCredentials(String accessKeyId, String secretAccessKey) {
        GitlabProperties gitlabProperties = this.properties.getGitlab();
        Map<String, List<GitlabVar>> toReturn = new HashMap<>();

        for (String projectId : gitlabProperties.getDispatchProjects()) {
            LOGGER.info("Changing credentials of app: {}", getName());
            Optional<GitlabVar> prevAccessKeyId = Optional.empty();
            Optional<GitlabVar> prevSecretAccessKey = Optional.empty();

            try {
                prevAccessKeyId = getVar(Constants.AWS_ACCESS_KEY_ID);
                prevSecretAccessKey = getVar(Constants.AWS_SECRET_ACCESS_KEY);

                Optional<GitlabVar> accessKeyIdVar = updateVar(gitlabProperties.getToken(), projectId, gitlabProperties.getEnvironment(),
                                                               Constants.AWS_ACCESS_KEY_ID, accessKeyId);
                Optional<GitlabVar> secretAccessKeyVar = updateVar(gitlabProperties.getToken(), projectId, gitlabProperties.getEnvironment(),
                                                                   Constants.AWS_SECRET_ACCESS_KEY, secretAccessKey);

                if (accessKeyIdVar.isEmpty() || secretAccessKeyVar.isEmpty() || !accessKeyId.equals(accessKeyIdVar.get().getValue())
                    || !secretAccessKey.equals(secretAccessKeyVar.get().getValue())) {
                    prevAccessKeyId.ifPresent(
                            gitlabVar -> updateVar(gitlabProperties.getToken(), projectId, gitlabProperties.getEnvironment(), gitlabVar));
                    prevSecretAccessKey.ifPresent(
                            gitlabVar -> updateVar(gitlabProperties.getToken(), projectId, gitlabProperties.getEnvironment(), gitlabVar));
                    this.mailService.sendGitlabUpdateError(projectId, new IllegalArgumentException("Key Id or Secret does not match new one"));
                } else {
                    List<GitlabVar> rollback = new ArrayList<>();
                    prevAccessKeyId.ifPresent(rollback::add);
                    prevSecretAccessKey.ifPresent(rollback::add);
                    toReturn.put(projectId, rollback);
                }
            } catch (Exception e) {
                LOGGER.error("An error occurred while performing rotation for {}: {}!", getName(), e.getMessage(), e);
                this.mailService.sendGitlabUpdateError(projectId, e);
                prevAccessKeyId.ifPresent(gitlabVar -> updateVar(gitlabProperties.getToken(), projectId, gitlabProperties.getEnvironment(), gitlabVar));
                prevSecretAccessKey.ifPresent(gitlabVar -> updateVar(gitlabProperties.getToken(), projectId, gitlabProperties.getEnvironment(), gitlabVar));
                throw e;
            }
        }

        return toReturn;
    }

    @Override
    public void rollBack(Map<String, ?> vars) {
        for (Map.Entry<String, ?> entry : vars.entrySet()) {
            LOGGER.info("Rollback for {}", entry.getKey());
            for (GitlabVar gitlabVar : (List<GitlabVar>) entry.getValue()) {
                updateVar(this.properties.getGitlab().getToken(), entry.getKey(), this.properties.getGitlab().getEnvironment(), gitlabVar);
            }
        }
    }

    private Optional<GitlabVar> getVar(String key) {
        GitlabProperties p = this.properties.getGitlab();
        Mono<GitlabVar> response = getWebClient(p.getToken()).get()
                                                             .uri(b -> b.path("/projects/" + p.getProjectId() + "/variables/" + key)
                                                                        .query("filter[environment_scope]=" + p.getEnvironment())
                                                                        .build())
                                                             .retrieve()
                                                             .bodyToMono(GitlabVar.class)
                                                             .onErrorResume(WebClientResponseException.NotFound.class, notFound -> Mono.empty());

        return response.blockOptional();
    }

    private Optional<GitlabVar> updateVar(String token, String projectId, String environment, GitlabVar gitlabVar) {
        return updateVar(token, projectId, environment, gitlabVar.getKey(), gitlabVar.getValue());
    }

    private Optional<GitlabVar> updateVar(String token, String projectId, String environment, String key, String value) {
        Mono<GitlabVar> response = getWebClient(token).put()
                                                      .uri(b -> b.path("/projects/" + projectId + "/variables/" + key)
                                                                 .query("filter[environment_scope]=" + environment)
                                                                 .build())
                                                      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                                      .body(BodyInserters.fromFormData("value", value).with("environment_scope", environment))
                                                      .retrieve()
                                                      .bodyToMono(GitlabVar.class)
                                                      .onErrorResume(WebClientResponseException.NotFound.class, notFound -> Mono.empty());

        return response.blockOptional();
    }

    private WebClient getWebClient(String token) {
        return WebClient.builder().baseUrl(Constants.GITLAB_BASE_URL).defaultHeader(Constants.PRIVATE_TOKEN, token).build();
    }
}
