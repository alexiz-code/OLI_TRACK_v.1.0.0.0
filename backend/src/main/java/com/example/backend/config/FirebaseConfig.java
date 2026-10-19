package com.example.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private static final String APP_NAME = "OLITRACK";

    private final FirebaseProperties properties;

    public FirebaseConfig(FirebaseProperties properties) {
        this.properties = properties;
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return resolveCredentials();
    }

    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {

        List<FirebaseApp> existingApps = FirebaseApp.getApps();

        for (FirebaseApp app : existingApps) {
            if (APP_NAME.equals(app.getName())) {
                log.info(
                        "FirebaseApp '{}' ya estaba inicializada, se reutiliza.",
                        APP_NAME
                );
                return app;
            }
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(properties.getProjectId())
                .setStorageBucket(properties.getStorageBucket())
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(
                options,
                APP_NAME
        );

        log.info(
                "FirebaseApp '{}' inicializada para el proyecto '{}'.",
                APP_NAME,
                properties.getProjectId()
        );

        return app;
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public Storage cloudStorage(FirebaseApp firebaseApp, GoogleCredentials credentials) {
        // Deferir la consulta remota del bucket hasta que se suba un archivo.
        // Si el bucket no existe, no debe impedir que arranque toda la API.
        return StorageOptions.newBuilder()
                .setProjectId(firebaseApp.getOptions().getProjectId())
                .setCredentials(credentials)
                .build()
                .getService();
    }

    private GoogleCredentials resolveCredentials() throws IOException {

        if (StringUtils.hasText(properties.getCredentialsPath())) {

            String path = properties.getCredentialsPath().trim();

            log.info(
                    "Credenciales de Firebase cargadas desde archivo local: {}",
                    path
            );

            try (InputStream inputStream = new FileInputStream(path)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        if (StringUtils.hasText(properties.getCredentialsJson())) {

            String credentialsBase64 =
                    properties.getCredentialsJson().trim();

            try {

                byte[] decoded = Base64.getDecoder()
                        .decode(credentialsBase64);

                log.info(
                        "Credenciales de Firebase cargadas desde variable de entorno en Base64."
                );

                try (InputStream inputStream =
                             new ByteArrayInputStream(decoded)) {

                    return GoogleCredentials.fromStream(inputStream);
                }

            } catch (IllegalArgumentException e) {

                throw new IOException(
                        "FIREBASE_CREDENTIALS_JSON no contiene un valor Base64 válido.",
                        e
                );
            }
        }

        log.info(
                "Credenciales de Firebase cargadas mediante Application Default Credentials."
        );

        return GoogleCredentials.getApplicationDefault();
    }

    static String decodeBase64(String value) {

        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "El valor Base64 no puede estar vacío."
            );
        }

        return new String(
                Base64.getDecoder().decode(value.trim()),
                StandardCharsets.UTF_8
        );
    }
}
