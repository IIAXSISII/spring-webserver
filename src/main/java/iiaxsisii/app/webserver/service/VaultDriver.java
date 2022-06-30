package iiaxsisii.app.webserver.service;

import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.response.LogicalResponse;
import iiaxsisii.app.webserver.config.WebServerConfigs;
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

import javax.annotation.PostConstruct;
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
    WebServerConfigs serverConfigs;

    @Autowired
    private AWSCredentialsWrapper awsCredentials;

    @Autowired
    HttpClientWrapper httpClientWrapper;

    private Vault vaultClient = null;

    private String awsAccount;
    private Region awsRegion;
    private String serviceName;
    public String vaultSecretPathPrefix;
    private String iamTokenServiceEndpoint;
    private String vaultUrl;

    private final String awsSTSRequestBody = "Action=GetCallerIdentity&Version=2011-06-15";
    private final static HashMap<String, List<String>> requestHeaders = new HashMap<>() {{
        put("Content-Type", Arrays.asList("application/x-www-form-urlencoded; charset=utf-8"));
    }};

    private void loginWithToken() throws Exception {
        final VaultConfig config = new VaultConfig()
                .address(vaultUrl)
                .engineVersion(2)
                .prefixPath(vaultSecretPathPrefix)
                .token(serverConfigs.getVaultToken())
                .build();
        this.vaultClient = new Vault(config);
    }

    private void loginWithAwsIam() throws Exception {

        final SdkHttpFullRequest unsignedRequest = SdkHttpFullRequest.builder()
                .uri(new URI(iamTokenServiceEndpoint))
                .method(SdkHttpMethod.POST)
                .headers(requestHeaders)
                .contentStreamProvider(() -> new ByteArrayInputStream(awsSTSRequestBody.getBytes(StandardCharsets.UTF_8)))
                .build();

        final AwsCredentials credentials = awsCredentials.getCredentials();
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
                .engineVersion(2)
                .prefixPath(vaultSecretPathPrefix)
                .build();
        final Vault vault = new Vault(config);
        final AuthResponse authResponse = vault.auth()
                .loginByAwsIam(serviceName,
                        Base64.getEncoder().encodeToString(iamTokenServiceEndpoint.getBytes(StandardCharsets.UTF_8)),
                        Base64.getEncoder().encodeToString(awsSTSRequestBody.getBytes(StandardCharsets.UTF_8)),
                        Base64.getEncoder().encodeToString(signedHeaders.toString().getBytes(StandardCharsets.UTF_8)),
                        awsAccount);

        // Adding the token into config enables the creation of
        //  an authenticated Vault client with provided config
        config.token(authResponse.getAuthClientToken());
        this.vaultClient = new Vault(config);
    }

    public String getSecretViaToken(String secretFolderPath, String key)  throws Exception {
        loginWithToken();
        return getSecret(secretFolderPath, key);
    }

    public String getSecretViaIam(String secretFolderPath, String key)  throws Exception {
        loginWithAwsIam();
        return getSecret(secretFolderPath, key);
    }

    public String getSecret(String secretFolderPath, String key) throws Exception {
        // Given the path awsAccountName/serviceName/secretFolder/secretKey or awsAccountName/serviceName/secretKey
        //  for the secret awsAccountName/serviceName is static for each service and
        //  secretFolder/secretKey or /secretKey is the secret itself.
        String secretFullPath;
        Logical vaultLogical = vaultClient.logical();
        LogicalResponse vaultResponse;
        if (StringUtils.equals(secretFolderPath, StringUtils.EMPTY)) {
            vaultResponse = vaultLogical.read(vaultSecretPathPrefix + "/" + awsAccount + "/" + serviceName);
        } else {
            vaultResponse = vaultLogical.read(vaultSecretPathPrefix + "/" + awsAccount + "/" + serviceName + "/" + secretFolderPath);
        }
        final HashMap<String, String> secrets = new HashMap<>(vaultResponse.getData());
        return secrets.get(key);
    }

    @PostConstruct
    public void postConstruct() {
        this.awsRegion = Region.of(serverConfigs.getAwsRegion());
        this.awsAccount = serverConfigs.getAwsAccountName();
        this.serviceName = serverConfigs.getServiceName();
        this.vaultSecretPathPrefix = "secret";
        this.iamTokenServiceEndpoint = serverConfigs.getIamTokenServiceEndpoint();
        this.vaultUrl = serverConfigs.getVaultUrl();
    }
}