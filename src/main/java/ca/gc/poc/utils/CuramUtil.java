package ca.gc.poc.utils;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The type Curam rest api util.
 */
public class CuramUtil {

    /**
     * The constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CuramUtil.class);

    /**
     * The constant instance.
     */
    private static CuramUtil instance;

    /**
     * The Cookies.
     */
    private List<String> cookies;

    /**
     * The Rest template.
     */
    private RestTemplate restTemplate;

    /**
     * The Curam host.
     */
    private String curamHost;

    /**
     * Gets instance.
     *
     * @return the instance
     * @throws Exception the exception
     */
    public static CuramUtil getInstance() throws Exception {
        if (instance == null) {
            login();
        }

        return instance;
    }

    /**
     * Login.
     *
     * @throws Exception the exception
     */
    private static void login() throws Exception {
        // Update trust strategy to go through Curam with proper credentials
        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;
        SSLContext sslContext = SSLContexts
                .custom()
                .loadTrustMaterial(null, trustStrategy)
                .build();
        SSLConnectionSocketFactory csf =
                new SSLConnectionSocketFactory(
                        sslContext,
                        new NoopHostnameVerifier()
                );
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .setSSLSocketFactory(csf)
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        requestFactory
                .setHttpClient(httpClient);

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.add("referer", "https://localhost");

        // Get variables from properties file
        Properties properties = new Properties();
        InputStream inputStream = CuramUtil.class.getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        if (inputStream != null) inputStream.close();

        String curamHost = properties.getProperty("curam.host");
        String curamUsername = properties.getProperty("curam.username");
        String curamPassword = properties.getProperty("curam.password");
        String curamLoginEndpoint = properties.getProperty("curam.endpoint.login");

        // Create endpoint for logging-in to Curam
        String curamLoginUrl = curamHost + curamLoginEndpoint + "?" +
                "j_username=" + curamUsername +
                "&" +
                "j_password=" + curamPassword;

        HttpEntity<String> request = new HttpEntity<>(headers);

        // Send POST request for logging-in
        ResponseEntity<String> loginResponse =
                restTemplate.exchange(
                        curamLoginUrl,
                        HttpMethod.GET,
                        request,
                        String.class
                );

        // Check response status code
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            logger.info(LocalDateTime.now() + " - Curam Login successful");
        } else {
            String message = "Could not login to Curam. Reason:\n\n" + loginResponse.getBody();
            throw new Exception(message);
        }

        instance = new CuramUtil();
        instance.curamHost = curamHost;
        instance.restTemplate = restTemplate;
        instance.cookies = loginResponse.getHeaders().getValuesAsList("Set-Cookie");
    }

    /**
     * Send user account.
     *
     * @param payload     the payload
     * @param destination the destination
     * @return the string
     * @throws Exception the exception
     */
    public String sendToService(String payload, String destination) throws Exception {

        // Set headers for user account request
        HttpHeaders headers = new HttpHeaders();
        headers.add("cookie", cookies.get(0));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("referer", "https://localhost");

        // Set service destination
        String curamServiceEndpoint = this.curamHost + destination;

        // Build the user account request
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        // Send POST request for user account
        ResponseEntity<String> userAccountResponse =
                restTemplate
                        .postForEntity(
                                curamServiceEndpoint,
                                request,
                                String.class
                        );

        // Check response status code and return it
        String result;

        if (userAccountResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
            result = "Curam user account successfully added";
        } else if (userAccountResponse.getStatusCode() == HttpStatus.FOUND) {
            String message = "Could not reach Curam, returned a 302 - FOUND";
            throw new Exception(message);
        } else {
            String message = "Could not create Curam user account. Reason:\n\n" + userAccountResponse.getBody();
            throw new Exception(message);
        }

        return result;
    }
}
