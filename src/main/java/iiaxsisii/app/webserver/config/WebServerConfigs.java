package iiaxsisii.app.webserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties(prefix="webserver")
public class WebServerConfigs {
    private String awsAccountName;
    private String awsRegion;
    private String serviceName;
    private int vaultEngineVersion;
    private String iamTokenServiceEndpoint;
    private String vaultUrl;
    private String vaultToken;
    private String ec2MetadataAddress;
    private String consulUrl;

    public String getAwsAccountName() {
        return awsAccountName;
    }

    public void setAwsAccountName(String awsAccountName) {
        this.awsAccountName = awsAccountName;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getVaultEngineVersion() {
        return vaultEngineVersion;
    }

    public void setVaultEngineVersion(int vaultEngineVersion) {
        this.vaultEngineVersion = vaultEngineVersion;
    }

    public String getIamTokenServiceEndpoint() {
        return iamTokenServiceEndpoint;
    }

    public void setIamTokenServiceEndpoint(String iamTokenServiceEndpoint) { this.iamTokenServiceEndpoint = iamTokenServiceEndpoint; }

    public String getVaultUrl() {
        return vaultUrl;
    }

    public void setVaultUrl(String vaultUrl) {
        this.vaultUrl = vaultUrl;
    }

    public String getEc2MetadataAddress() {
        return ec2MetadataAddress;
    }

    public void setEc2MetadataAddress(String ec2MetadataAddress) {
        this.ec2MetadataAddress = ec2MetadataAddress;
    }

    public String getVaultToken() { return vaultToken; }

    public void setVaultToken(String vaultToken) { this.vaultToken = vaultToken; }

    public String getConsulUrl() {return consulUrl;}

    public void setConsulUrl(String consulUrl) { this.consulUrl = consulUrl; }
}
