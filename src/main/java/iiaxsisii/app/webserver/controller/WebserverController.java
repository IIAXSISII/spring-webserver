package iiaxsisii.app.webserver.controller;

import iiaxsisii.app.webserver.config.ConsulConfigService;
import iiaxsisii.app.webserver.config.WebServerConfigs;
import iiaxsisii.app.webserver.security.AWSCredentialsWrapper;
import iiaxsisii.app.webserver.service.HttpClientWrapper;
import iiaxsisii.app.webserver.service.VaultDriver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@RestController
@EnableConfigurationProperties(value = ConsulConfigService.class)
public class WebserverController {

    @Autowired
    WebServerConfigs serverConfigs;

    @Autowired
    VaultDriver vaultDriver;

    @Autowired
    AWSCredentialsWrapper awsCredentialsWrapper;

    @Autowired
    HttpClientWrapper httpClientWrapper;

    @Autowired
    ConsulConfigService consulConfigs;

    Logger logger = LoggerFactory.getLogger(HttpClientWrapper.class);

    @GetMapping("iam/secrets/{secretPath}/{secretKey}")
    public Map<String, String> getSecretsViaIam(@PathVariable String secretPath, @PathVariable String secretKey) throws Exception {
        String iamRole = awsCredentialsWrapper.getMyIdentity();

        Map<String, String> result = new HashMap<>();

        String secret = vaultDriver.getSecretViaIam(secretPath, secretKey);
        AwsCredentials awsCredentials = awsCredentialsWrapper.getCredentials();
        String accessKeyId = awsCredentials.accessKeyId();

        if (StringUtils.isBlank(secret)) {
            result.put("ERROR", "Secret not found for the provided key!");
        }
        result.put("secretValue", secret);
        result.put("secretKey", secretKey);
        result.put("iamRole", iamRole);
        result.put("accessKey", accessKeyId);
        result.put("vaultUrl", serverConfigs.getVaultUrl());
        result.put("vaultRole", serverConfigs.getServiceName());
        result.put("secretPathPrefix", vaultDriver.vaultSecretPathPrefix + "/"
                + serverConfigs.getAwsAccountName() + "/"
                + serverConfigs.getServiceName() + "/"
                + secretPath);
        return result;
    }

    @RequestMapping("/")
    public Map index() throws SocketException {

        Map<String, String> responseMap = new HashMap<>();

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface networkInterface : Collections.list(nets))
            displayInterfaceInformation(networkInterface, responseMap);
        return responseMap;
    }

    void displayInterfaceInformation(NetworkInterface networkInterface, Map<String, String> responseMap) throws SocketException {
        responseMap.put("Network Display Name", networkInterface.getDisplayName());
        responseMap.put("Network Name", networkInterface.getName());
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        int count = 0;
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            count++;
            responseMap.put(count +" InetAddress", inetAddress.getAddress().toString());
            responseMap.put(count + " HostAddress", inetAddress.getHostAddress().toString());
        }
    }

    @GetMapping("token/secrets/{secretPath}/{secretKey}")
    public Map<String, String> getSecretsViaToken(@PathVariable String secretPath, @PathVariable String secretKey) throws Exception {
        Map<String, String> result = new HashMap<>();
        String secret = vaultDriver.getSecretViaToken(secretPath, secretKey);
        if (StringUtils.isBlank(secret)) {
            result.put("ERROR", "Secret not found for the provided key!");
        }
        result.put("secretValue", secret);
        result.put("secretKey", secretKey);
        result.put("secretPathPrefix", vaultDriver.vaultSecretPathPrefix + "/" + serverConfigs.getAwsAccountName() + "/" + serverConfigs.getServiceName() + "/" + secretPath);
        result.put("vaultUrl", serverConfigs.getVaultUrl());
        return result;
    }

    @GetMapping("consul/configs")
    public Map<String, String> getConsulConfigs() throws IOException {
       return consulConfigs.getAllConfigs();
    }

    @GetMapping("consul/configs/{configKey}")
    public String getConsulConfig(@PathVariable String configKey) throws IOException {
        return consulConfigs.getConfig(configKey);
    }
}
