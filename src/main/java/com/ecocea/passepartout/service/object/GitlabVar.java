package com.ecocea.passepartout.service.object;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class GitlabVar {

    @JsonProperty("variable_type")
    private String type;

    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;

    @JsonProperty("protected")
    private Boolean protect;

    @JsonProperty("masked")
    private Boolean masked;

    @JsonProperty("environment_scope")
    private String env;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getProtect() {
        return protect;
    }

    public void setProtect(Boolean protect) {
        this.protect = protect;
    }

    public Boolean getMasked() {
        return masked;
    }

    public void setMasked(Boolean masked) {
        this.masked = masked;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof GitlabVar)) {
            return false;
        }

        GitlabVar gitlabVar = (GitlabVar) o;
        return Objects.equals(type, gitlabVar.type) && Objects.equals(key, gitlabVar.key) && Objects.equals(value, gitlabVar.value)
               && Objects.equals(protect, gitlabVar.protect) && Objects.equals(masked, gitlabVar.masked) && Objects.equals(env, gitlabVar.env);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, value, protect, masked, env);
    }
}
