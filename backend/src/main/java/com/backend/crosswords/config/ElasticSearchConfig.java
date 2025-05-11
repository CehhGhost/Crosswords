package com.backend.crosswords.config;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.backend.crosswords.corpus.repositories.elasticsearch")
@EnableJpaRepositories(basePackages = {"com.backend.crosswords.corpus.repositories.jpa",
        "com.backend.crosswords.admin.repositories"})
@ComponentScan(basePackageClasses = ElasticSearchConfig.class)
public class ElasticSearchConfig {
    @Value("${opensearch.keystore.path}")
    private String keystorePath;

    @Value("${opensearch.keystore.password}")
    private String keystorePassword;

    @Bean
    RestClientBuilderCustomizer customizer() {
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                try {
                    builder.setSSLContext(buildSSLContext()).setSSLHostnameVerifier((hostname, session) -> true);
                } catch (final KeyManagementException | UnrecoverableKeyException | CertificateException | IOException |
                               NoSuchAlgorithmException | KeyStoreException ex) {
                    throw new RuntimeException("Failed to initialize SSL Context instance", ex);
                }
            }

            @Override
            public void customize(RestClientBuilder builder) {
                // No additional customizations needed
            }
        };
    }

    private SSLContext buildSSLContext() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
        KeyStore keystore = loadKeyStore(keystorePath, keystorePassword, "PKCS12");

        return SSLContextBuilder.create()
                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                .loadKeyMaterial(keystore, keystorePassword.toCharArray())
                .build();
    }

    private KeyStore loadKeyStore(String path, String password, String type) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }
}