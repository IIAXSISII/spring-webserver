package iiaxsisii.app.webserver.config;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import iiaxsisii.app.webserver.service.HttpClientWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@ConfigurationProperties
public class ConsulConfigService {

    @Autowired
    WebServerConfigs serverConfigs;

    @Autowired
    HttpClientWrapper httpClient;

    private Gson gson = new Gson();
    private Map<String, String> allConfigs = new HashMap<>();
    private static String CONSUL_KV_API_VERSION_PATH = "v1/kv/";

    public String getConfig(String key) throws IOException {
        if (!allConfigs.containsKey(getConsulKeyPrefix() + key)) {
            fetchAllConfigs();
        }
        return allConfigs.get(getConsulKeyPrefix() + key);
    }

    public String serverConsulConfigsBaseUrl() {
        String url = serverConfigs.getConsulUrl();
        if (! url.endsWith("/")) {
            url = url + "/";
        }
        return url + CONSUL_KV_API_VERSION_PATH;
    }

    public void fetchAllConfigs() throws IOException {
        allConfigs.clear();
        List<Object> configsKeys = httpClient.httpGetResponseAsList(
                serverConsulConfigsBaseUrl()
                + getConsulKeyPrefix()
                + "?keys");

        for (Object configKeyObject : configsKeys) {
            String configKey = (String) configKeyObject;
            String key = StringUtils.substringAfterLast(configKey,"/");
            String jsonResponse = httpClient.httpGetResponseAsString(serverConsulConfigsBaseUrl()
                    + getConsulKeyPrefix()
                    + key);
            String config = getValueFromConsulResponse(jsonResponse);
            allConfigs.put(configKey, config);
        }
    }

    public Map<String, String> getAllConfigs() throws IOException {
        if (allConfigs.isEmpty()) {
            fetchAllConfigs();
        }
        return ImmutableMap.copyOf(allConfigs);
    }

    private String getValueFromConsulResponse(String jsonResponse) {
        JsonArray consulConfigs = gson.fromJson(jsonResponse, JsonArray.class);
        JsonObject consulConfig = (JsonObject) consulConfigs.get(0);
        String encodedValue = consulConfig.get("Value").getAsString();
        return new String(Base64.getDecoder().decode(encodedValue));
    }

    private String getConsulKeyPrefix() {
        return serverConfigs.getAwsAccountName()
                + "/"
                + serverConfigs.getServiceName()
                + "/";
    }
}
