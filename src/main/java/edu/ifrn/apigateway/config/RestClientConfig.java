package edu.ifrn.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    RestClient rickAndMortyClient(
            @Value("${app.providers.rickandmorty.base-url}") String baseUrl,
            @Value("${app.providers.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${app.providers.read-timeout-ms:5000}") long readTimeout) {
        return client(baseUrl, connectTimeout, readTimeout);
    }

    @Bean
    RestClient simpsonsClient(
            @Value("${app.providers.simpsons.base-url}") String baseUrl,
            @Value("${app.providers.connect-timeout-ms:2000}") long connectTimeout,
            @Value("${app.providers.read-timeout-ms:5000}") long readTimeout) {
        return client(baseUrl, connectTimeout, readTimeout);
    }

    private RestClient client(String baseUrl, long connectTimeout, long readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
