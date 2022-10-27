package com.ecocea.passepartout.config.properties;

import java.util.List;

public class HerokuProperties {

    private String sourceApplication;

    private List<String> dispatchApplications;

    private String token;

    public String getSourceApplication() {
        return sourceApplication;
    }

    public void setSourceApplication(String sourceApplication) {
        this.sourceApplication = sourceApplication;
    }

    public List<String> getDispatchApplications() {
        return dispatchApplications;
    }

    public void setDispatchApplications(List<String> dispatchApplications) {
        this.dispatchApplications = dispatchApplications;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
