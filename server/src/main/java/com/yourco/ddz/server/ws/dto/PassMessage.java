package com.yourco.ddz.server.ws.dto;

import java.util.UUID;

/** WebSocket message for passing (not playing any cards). */
public class PassMessage extends GameActionMessage {
  public PassMessage() {}

  public PassMessage(UUID playerId) {
    super(playerId);
  }
}
