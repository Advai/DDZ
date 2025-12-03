package com.yourco.ddz.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Cache bust: 2025-12-02 - Force rebuild to deploy playerCount fixes
@SpringBootApplication
public class DdzServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(DdzServerApplication.class, args);
  }
}
