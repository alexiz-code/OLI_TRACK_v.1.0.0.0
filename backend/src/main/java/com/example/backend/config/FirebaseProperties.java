package com.example.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de Firebase. Los valores provienen de variables de entorno,
 * nunca se escriben en el código fuente.
 *
 * firebase.credentials-path  -> ruta al JSON de la service account (opcional si se usa credentials-json)
 * firebase.credentials-json  -> contenido del JSON en base64 (útil en despliegues cloud)
 * firebase.project-id        -> ID del proyecto de Firebase
 * firebase.storage-bucket    -> bucket de Firebase Storage
 */
@Validated
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    private String credentialsPath;

    private String credentialsJson;

    @NotBlank(message = "firebase.project-id es obligatorio")
    private String projectId;

    @NotBlank(message = "firebase.storage-bucket es obligatorio")
    private String storageBucket;

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public String getCredentialsJson() {
        return credentialsJson;
    }

    public void setCredentialsJson(String credentialsJson) {
        this.credentialsJson = credentialsJson;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }
}
