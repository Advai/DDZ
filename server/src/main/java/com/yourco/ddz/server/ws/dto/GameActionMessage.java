package com.yourco.ddz.server.ws.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;

/**
 * Base class for all game action messages sent via WebSocket.
 *
 * <p>Uses Jackson polymorphic deserialization to handle different action types.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = BidMessage.class, name = "BID"),
  @JsonSubTypes.Type(value = PlayMessage.class, name = "PLAY"),
  @JsonSubTypes.Type(value = PassMessage.class, name = "PASS"),
  @JsonSubTypes.Type(value = SelectLandlordMessage.class, name = "SELECT_LANDLORD")
})
public abstract class GameActionMessage {
  private UUID playerId;

  public GameActionMessage() {}

  public GameActionMessage(UUID playerId) {
    this.playerId = playerId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public void setPlayerId(UUID playerId) {
    this.playerId = playerId;
  }
}
