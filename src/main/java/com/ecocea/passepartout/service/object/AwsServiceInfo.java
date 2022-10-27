package com.ecocea.passepartout.service.object;

import com.ecocea.passepartout.service.AwsService;

public class AwsServiceInfo {

    private final AwsService awsService;

    private final String name;

    private final String accessKeyId;

    public AwsServiceInfo(AwsService awsService, String name, String accessKeyId) {
        this.awsService = awsService;
        this.name = name;
        this.accessKeyId = accessKeyId;
    }

    public AwsService getAwsService() {
        return awsService;
    }

    public String getName() {
        return name;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }
}
