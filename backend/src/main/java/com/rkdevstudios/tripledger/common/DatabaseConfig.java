package com.rkdevstudios.tripledger.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DatabaseConfig {

    private final Environment env;

    public DatabaseConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            String url = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/tripledger");
            String username = env.getProperty("spring.datasource.username", "postgres");
            String password = env.getProperty("spring.datasource.password", "postgres");
            
            return DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }

        try {
            URI uri = new URI(databaseUrl);
            String[] userInfo = uri.getUserInfo().split(":");
            String username = userInfo[0];
            String password = userInfo.length > 1 ? userInfo[1] : "";
            
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            
            String dbUrl = "jdbc:postgresql://" + host + (port != -1 ? ":" + port : "") + path;

            return DataSourceBuilder.create()
                    .url(dbUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        } catch (URISyntaxException | NullPointerException e) {
            throw new RuntimeException("Failed to parse DATABASE_URL environment variable: " + databaseUrl, e);
        }
    }
}
