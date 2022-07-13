package iiaxsisii.app.webserver.service;


import com.google.gson.Gson;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class HttpClientWrapper {

    Logger logger = LoggerFactory.getLogger(HttpClientWrapper.class);

    CloseableHttpClient httpClient = HttpClients.createDefault();
    Gson gson = new Gson();

    public String httpGetResponseAsString(String uri) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
        logger.debug("Http GET URI: " + uri);
        InputStream responseStream = httpResponse.getEntity().getContent();
        String response = new BufferedReader(
                new InputStreamReader(responseStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        logger.debug(response);
        return response;
    }

    public List<Object> httpGetResponseAsList(String uri) throws IOException {
        String responseString = httpGetResponseAsString(uri);
        return gson.fromJson(responseString, ArrayList.class);
    }
}
