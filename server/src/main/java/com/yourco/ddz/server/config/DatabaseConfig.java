package com.yourco.ddz.server.config;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Database configuration to handle Fly.io's postgres:// URL format. Fly.io provides DATABASE_URL
 * with postgres:// prefix (e.g. postgres://user:pass@host:port/db), but JDBC requires
 * jdbc:postgresql:// prefix. The PostgreSQL JDBC driver will parse the username and password from
 * the URL.
 */
@Configuration
@Profile("production")
public class DatabaseConfig {

  @Bean
  @Primary
  public DataSource dataSource() {
    String databaseUrl = System.getenv("DATABASE_URL");

    if (databaseUrl == null) {
      throw new IllegalStateException("DATABASE_URL environment variable is not set");
    }

    // Parse Fly.io's postgres:// URL format
    // Format: postgres://username:password@host:port/database?params
    // Extract components and build JDBC URL separately
    String url = databaseUrl.substring("postgres://".length());

    // Extract credentials
    int atIndex = url.indexOf('@');
    String credentials = url.substring(0, atIndex);
    int colonIndex = credentials.indexOf(':');
    String username = credentials.substring(0, colonIndex);
    String password = credentials.substring(colonIndex + 1);

    // Extract host, port, database
    String hostPart = url.substring(atIndex + 1);
    String jdbcUrl = "jdbc:postgresql://" + hostPart;

    return DataSourceBuilder.create().url(jdbcUrl).username(username).password(password).build();
  }
}
