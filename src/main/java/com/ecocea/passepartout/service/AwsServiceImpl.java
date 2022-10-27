package com.ecocea.passepartout.service;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.AccessKey;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyResponse;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AwsServiceImpl implements AwsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsServiceImpl.class);

    private final IamAsyncClient client;

    public AwsServiceImpl() {
        //Credentials are in config var, see: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html
        this.client = IamAsyncClient.builder().region(Region.AWS_GLOBAL).credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
    }

    public AwsServiceImpl(String accessKeyId, String secretAccessKey) {
        this.client = IamAsyncClient.builder()
                                    .region(Region.AWS_GLOBAL)
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                                    .build();
    }

    @Override
    public List<AccessKeyMetadata> listAccessKeys() throws ExecutionException, InterruptedException {
        List<AccessKeyMetadata> results = new ArrayList<>();

        try {
            boolean done = false;
            String newMarker = null;

            while (!done) {
                ListAccessKeysResponse response;
                ListAccessKeysRequest request;

                if (newMarker == null) {
                    request = ListAccessKeysRequest.builder().build();
                } else {
                    request = ListAccessKeysRequest.builder().marker(newMarker).build();
                }

                response = this.client.listAccessKeys(request).get();
                results.addAll(response.accessKeyMetadata());

                if (BooleanUtils.isFalse(response.isTruncated())) {
                    done = true;
                } else {
                    newMarker = response.marker();
                }
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred while listing access keys: {}!", e.getMessage(), e);
            throw e;
        }

        return results;
    }

    @Override
    public AccessKey createAccessKey() {
        try {
            CreateAccessKeyRequest request = CreateAccessKeyRequest.builder().build();

            CreateAccessKeyResponse response = this.client.createAccessKey(request).get();
            return response.accessKey();
        } catch (Exception e) {
            LOGGER.error("An error occurred while creating access key: {}!", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void deleteAccessKey(String accessKeyId) {
        try {
            DeleteAccessKeyRequest request = DeleteAccessKeyRequest.builder().accessKeyId(accessKeyId).build();

            this.client.deleteAccessKey(request).get();
        } catch (Exception e) {
            LOGGER.error("An error occurred while creating access key: {}!", e.getMessage(), e);
        }
    }
}
