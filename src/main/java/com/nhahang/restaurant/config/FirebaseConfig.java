package com.nhahang.restaurant.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value; // Thêm import này
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream; // Thêm import này
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets; // Thêm import này

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT}")
    private String firebaseServiceAccountJson;
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        InputStream serviceAccount = new ByteArrayInputStream(
            firebaseServiceAccountJson.getBytes(StandardCharsets.UTF_8)
        );

        FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance();
        }
    }
}