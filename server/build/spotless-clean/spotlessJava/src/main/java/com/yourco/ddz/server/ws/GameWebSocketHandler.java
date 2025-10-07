package com.yourco.ddz.server.ws;

import com.yourco.ddz.engine.core.PlayerAction;
import com.yourco.ddz.server.core.GameRegistry;
import java.net.URI;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
  private final GameRegistry registry;

  public GameWebSocketHandler(GameRegistry r) {
    this.registry = r;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession s) throws Exception {
    s.sendMessage(new TextMessage("connected"));
  }

  @Override
  protected void handleTextMessage(WebSocketSession s, TextMessage msg) throws Exception {
    URI uri = s.getUri();
    if (uri == null) return;
    var parts = uri.getPath().split("/");
    var gameId = parts[parts.length - 1];
    var gi = registry.get(gameId);
    if (gi == null) {
      s.sendMessage(new TextMessage("error: game not found"));
      return;
    }
    var pid = UUID.randomUUID();
    gi.loop().submit(new PlayerAction(pid, "PLAY", msg.getPayload()));
    gi.loop().tick();
    s.sendMessage(
        new TextMessage("ok: action applied; total=" + gi.loop().state().actionLog().size()));
  }
}
