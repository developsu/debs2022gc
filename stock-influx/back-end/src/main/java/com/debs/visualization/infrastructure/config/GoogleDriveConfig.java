package com.debs.visualization.infrastructure.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Builder;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration
public class GoogleDriveConfig {

    private static final String appName = "debs-google-drive";
    private static final String jsonFileName = "spring-market-347503-100e3c98ce16.json";

    @Bean
    public Drive myDrive() {
        try {
            final Resource resource = new ClassPathResource(jsonFileName);
            return new Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(
                    ServiceAccountCredentials.fromStream(resource.getInputStream())
                                             .createScoped(List.of(DriveScopes.DRIVE_READONLY))
                                             .createDelegated("debs-google-drive@spring-market-347503.iam.gserviceaccount.com")
                )
            ).setApplicationName(appName).build();
        } catch (GeneralSecurityException | IOException e) {
            throw new BeanCreationException(e.getMessage());
        }
    }
}
