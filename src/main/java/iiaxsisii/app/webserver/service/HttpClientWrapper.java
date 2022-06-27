package iiaxsisii.app.webserver.service;


import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class HttpClientWrapper {

    CloseableHttpClient httpClient = HttpClients.createDefault();

    public String httpGetResponseAsString(String uri) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpGet);
        return response.toString();
    }
}
