package com.ecocea.passepartout.service;

import software.amazon.awssdk.services.iam.model.AccessKey;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface AwsService {

    List<AccessKeyMetadata> listAccessKeys() throws ExecutionException, InterruptedException;

    AccessKey createAccessKey();

    void deleteAccessKey(String accessKeyId);
}
