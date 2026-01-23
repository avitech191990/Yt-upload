package com.main.yt.common;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.youtube.YouTube;
import org.springframework.stereotype.Component;

@Component
public class GoogleClientFactory {

    private static final NetHttpTransport HTTP = new NetHttpTransport();
    private static final JacksonFactory JSON = JacksonFactory.getDefaultInstance();

    private GoogleCredential credential() throws Exception {

        String clientId = requireEnv("CLIENT_ID");
        String clientSecret = requireEnv("CLIENT_SECRET");
        String refreshToken = requireEnv("REFRESH_TOKEN");

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP)
                .setJsonFactory(JSON)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        credential.refreshToken();
        return credential;
    }

    public Drive drive() throws Exception {
        return new Drive.Builder(
                HTTP,
                JSON,
                credential()
        ).setApplicationName(requireEnv("APPLICATION_NAME")).build();
    }

    public YouTube youtube() throws Exception {
        return new YouTube.Builder(
                HTTP,
                JSON,
                credential()
        ).setApplicationName(requireEnv("APPLICATION_NAME")).build();
    }

    // ============================
    // SAFETY CHECK
    // ============================
    private String requireEnv(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + key
            );
        }
        return val;
    }
}
