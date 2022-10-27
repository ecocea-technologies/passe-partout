package com.ecocea.passepartout.config.properties;

import java.util.List;

public class GitlabProperties {

    private String environment;

    private String projectId;

    private String token;

    private List<String> dispatchProjects;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getDispatchProjects() {
        return dispatchProjects;
    }

    public void setDispatchProjects(List<String> dispatchProjects) {
        this.dispatchProjects = dispatchProjects;
    }
}
