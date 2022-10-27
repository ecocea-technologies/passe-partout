package com.ecocea.passepartout.service;

import com.ecocea.passepartout.service.object.AwsServiceInfo;

import java.util.Map;
import java.util.Optional;

public interface PassePartoutService<T extends Object> {

    Optional<AwsServiceInfo> getAwsService();

    /**
     * @return properties valid to be a source
     */
    boolean checkProperties();

    String getName();

    Map<String, T> dispatchCredentials(String accessKeyId, String secretAccessKey);

    void rollBack(Map<String, ?> map);
}
