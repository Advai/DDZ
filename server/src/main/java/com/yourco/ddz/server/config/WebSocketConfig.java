package com.yourco.ddz.server.config;

import com.yourco.ddz.server.ws.GameWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@org.springframework.context.annotation.Profile("!test")
public class WebSocketConfig implements WebSocketConfigurer {
  private final GameWebSocketHandler handler;

  public WebSocketConfig(GameWebSocketHandler h) {
    this.handler = h;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws/game/*").setAllowedOrigins("*");
  }

  @Bean
  public org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean
      createWebSocketContainer() {
    var c = new org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean();
    c.setMaxTextMessageBufferSize(65536);
    c.setMaxBinaryMessageBufferSize(65536);
    return c;
  }
}
