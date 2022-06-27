package iiaxsisii.app.webserver.controller;

import iiaxsisii.app.webserver.security.AWSCredentialsWrapper;
import iiaxsisii.app.webserver.service.HttpClientWrapper;
import iiaxsisii.app.webserver.service.VaultDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@RestController
public class WebserverController {

    @Autowired
    VaultDriver vaultDriver;

    @Autowired
    AWSCredentialsWrapper awsCredentialsWrapper;

    @Autowired
    HttpClientWrapper httpClientWrapper;

    @GetMapping("/secrets/{secretKey}")
    public Map<String, String> getSecretsViaIam(@PathVariable String secretKey) throws Exception {
        String iamMetadataAddress = "http://169.254.169.254/latest/meta-data/iam/info";
        String iamRole = iamMetadataAddress; //httpClientWrapper.httpGetResponseAsString(iamMetadataAddress);
        iamRole = awsCredentialsWrapper.getMyIdentity();

        Map<String, String> result = new HashMap<>();

        String secret = vaultDriver.getSecret(secretKey);
        AwsCredentials awsCredentials = awsCredentialsWrapper.getCredentials();
        String accessKeyId = awsCredentials.accessKeyId();

        result.put("secretValue", secret);
        result.put("secretKey", secretKey);
        result.put("secretPathPrefix", VaultDriver.vaultSecretPathPrefix);
        result.put("iamRole", iamRole);
        result.put("accessKey", accessKeyId);

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
}
