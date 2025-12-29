package com.yourco.ddz.server.ws.dto;

import com.yourco.ddz.server.api.dto.GameStateResponse;
import com.yourco.ddz.server.api.dto.SpectatorGameInfo;

/** WebSocket message sent to clients when game state changes. */
public class GameUpdateMessage {
  private String type = "GAME_UPDATE";
  private GameStateResponse state;
  private SpectatorGameInfo spectatorInfo;
  private String message; // Optional message (e.g., "Player X bid 3")

  public GameUpdateMessage() {}

  public GameUpdateMessage(GameStateResponse state, String message) {
    this.state = state;
    this.message = message;
  }

  public GameUpdateMessage(SpectatorGameInfo spectatorInfo, String message) {
    this.spectatorInfo = spectatorInfo;
    this.message = message;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public GameStateResponse getState() {
    return state;
  }

  public void setState(GameStateResponse state) {
    this.state = state;
  }

  public SpectatorGameInfo getSpectatorInfo() {
    return spectatorInfo;
  }

  public void setSpectatorInfo(SpectatorGameInfo spectatorInfo) {
    this.spectatorInfo = spectatorInfo;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
