package iiaxsisii.app.webserver.service;

import iiaxsisii.app.webserver.security.AWSCredentialsWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

@Component
public class VaultDriver {

    @Autowired
    private AWSCredentialsWrapper awsCredentials;

    private Vault vaultClient = null;

    private final static String awsAccount = "rtb";
    private final static String serviceName = "bidder-3p-settings";
    public final static String vaultSecretPathPrefix = awsAccount + "/" + serviceName;

    private final static int engineVersion = 2;
    private final static Region awsRegion = Region.US_EAST_1;
    private final static String iamTokenServiceEndpoint = "https://sts.amazonaws.com/";
    private final static String tokenServiceRequestBody = "Action=GetCallerIdentity&Version=2011-06-15";
    private final static String vaultUrl = "https://vault-development.triplelift.net/";

    private final static HashMap<String, List<String>> requestHeaders = new HashMap<>() {{
        put("Content-Type", Arrays.asList("application/x-www-form-urlencoded; charset=utf-8"));
    }};


    private void loginWithAwsIam() throws Exception {
        final AwsCredentials credentials = awsCredentials.getCredentials();
        final String vaultRoleName = awsCredentials.getMyIdentity();

        final SdkHttpFullRequest unsignedRequest = SdkHttpFullRequest.builder()
                .uri(new URI(iamTokenServiceEndpoint))
                .method(SdkHttpMethod.POST)
                .headers(requestHeaders)
                .contentStreamProvider(() -> new ByteArrayInputStream(tokenServiceRequestBody.getBytes(StandardCharsets.UTF_8)))
                .build();

        final SdkHttpFullRequest signedRequest = Aws4Signer
                .create()
                .sign(unsignedRequest, Aws4SignerParams
                        .builder()
                        .signingName("sts")
                        .signingRegion(awsRegion)
                        .awsCredentials(credentials)
                        .build());

        final JsonObject signedHeaders = new JsonObject();
        signedRequest
                .headers()
                .entrySet()
                .forEach(entry -> {
                    final JsonArray array = new JsonArray();
                    entry.getValue().forEach(array::add);
                    signedHeaders.add(entry.getKey(), array);
                });


        final VaultConfig config = new VaultConfig()
                .address(vaultUrl)
                .engineVersion(engineVersion)
                .prefixPath(vaultSecretPathPrefix)
                .build();
        final Vault vault = new Vault(config);
        final AuthResponse authResponse = vault.auth()
                .loginByAwsIam(vaultRoleName,
                        Base64.getEncoder().encodeToString(iamTokenServiceEndpoint.getBytes(StandardCharsets.UTF_8)),
                        Base64.getEncoder().encodeToString(tokenServiceRequestBody.getBytes(StandardCharsets.UTF_8)),
                        Base64.getEncoder().encodeToString(signedHeaders.toString().getBytes(StandardCharsets.UTF_8)),
                        awsAccount);

        // Adding the token into config enables the creation of
        //  an authenticated Vault client with provided config
        config.token(authResponse.getAuthClientToken());
        this.vaultClient = new Vault(config);
    }

    public String getSecret(String key)  throws Exception {
        return getSecret(StringUtils.EMPTY, key);
    }
    public String getSecret(String secretFolderPath, String key) throws Exception {
        if (vaultClient == null) {
            loginWithAwsIam();
        }

        // Given the path awsAccountName/serviceName/secretFolder/secretKey or awsAccountName/serviceName/secretKey
        //  for the secret awsAccountName/serviceName is static for each service and
        //  secretFolder/secretKey or /secretKey is the secret itself.
        String secretFullPath;
        if (StringUtils.equals(secretFolderPath, StringUtils.EMPTY)) {
            secretFullPath = vaultSecretPathPrefix;
        } else {
            secretFullPath = vaultSecretPathPrefix + "/" + secretFolderPath;
        }
        final HashMap<String, String> secrets = new HashMap<>(vaultClient.logical()
                .read(secretFullPath)
                .getData());

        return secrets.get(key);
    }
}