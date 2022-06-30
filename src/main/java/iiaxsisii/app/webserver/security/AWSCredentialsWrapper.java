package iiaxsisii.app.webserver.security;

import iiaxsisii.app.webserver.config.WebServerConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import javax.annotation.PostConstruct;

@Component
public class AWSCredentialsWrapper {

    @Autowired
    WebServerConfigs serverConfigs;

    private DefaultCredentialsProvider defaultCredentialsProvider;
    private Region region;

    public AWSCredentialsWrapper() {
        defaultCredentialsProvider = DefaultCredentialsProvider.create();
    }

    public AwsCredentials getCredentials () {
        final AwsCredentials credentials = defaultCredentialsProvider.resolveCredentials();
        return credentials;
    }

    public String getMyIdentity() {
        try (StsClient stsClient = StsClient.builder()
                .region(region)
                .credentialsProvider(defaultCredentialsProvider)
                .build()) {
            GetCallerIdentityResponse response = stsClient.getCallerIdentity();
            return response.arn();
        } catch (StsException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void postConstruct() {
        this.region = Region.of(serverConfigs.getAwsRegion());
    }
}
