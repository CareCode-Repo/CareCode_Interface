package com.carecode.core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

/**
 * FCM 초기화.
 *
 * <p>서비스 계정 자격증명이 설정되지 않은 환경(로컬/CI)에서도 애플리케이션이 뜨도록
 * 자격증명이 없으면 {@link FirebaseMessaging} 빈을 만들지 않는다.
 * 푸시 발송기는 이 빈이 없으면 자동으로 비활성 상태가 된다.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(
            ResourceLoader resourceLoader,
            @Value("${app.notification.fcm.credentials-location:}") String credentialsLocation) {

        if (credentialsLocation == null || credentialsLocation.isBlank()) {
            log.info("FCM 자격증명이 설정되지 않아 푸시 알림이 비활성화됩니다. "
                    + "(app.notification.fcm.credentials-location)");
            return null;
        }

        try {
            Resource resource = resourceLoader.getResource(credentialsLocation);
            if (!resource.exists()) {
                log.warn("FCM 자격증명 파일을 찾을 수 없어 푸시 알림이 비활성화됩니다: {}", credentialsLocation);
                return null;
            }

            try (InputStream credentialStream = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialStream))
                        .build();

                FirebaseApp app = FirebaseApp.getApps().isEmpty()
                        ? FirebaseApp.initializeApp(options)
                        : FirebaseApp.getInstance();

                log.info("FCM 초기화 완료");
                return FirebaseMessaging.getInstance(app);
            }
        } catch (Exception e) {
            // 푸시 설정 실패가 서비스 전체 기동을 막지 않도록 한다.
            log.error("FCM 초기화 실패 - 푸시 알림이 비활성화됩니다", e);
            return null;
        }
    }
}
