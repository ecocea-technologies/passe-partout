package com.ecocea.passepartout.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "passe-partout")
public class PassePartoutProperties {

    private List<String> email;

    private String from;

    private Integer deleteAfter;

    private Integer generateAfter;

    private HerokuProperties heroku;

    private GitlabProperties gitlab;

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Integer getDeleteAfter() {
        return deleteAfter;
    }

    public void setDeleteAfter(Integer deleteAfter) {
        this.deleteAfter = deleteAfter;
    }

    public Integer getGenerateAfter() {
        return generateAfter;
    }

    public void setGenerateAfter(Integer generateAfter) {
        this.generateAfter = generateAfter;
    }

    public HerokuProperties getHeroku() {
        return heroku;
    }

    public void setHeroku(HerokuProperties heroku) {
        this.heroku = heroku;
    }

    public GitlabProperties getGitlab() {
        return gitlab;
    }

    public void setGitlab(GitlabProperties gitlab) {
        this.gitlab = gitlab;
    }
}
