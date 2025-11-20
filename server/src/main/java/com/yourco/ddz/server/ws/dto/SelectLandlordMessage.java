package com.yourco.ddz.server.ws.dto;

import java.util.UUID;

/** WebSocket message for selecting a landlord during snake draft. */
public class SelectLandlordMessage extends GameActionMessage {
  private UUID selectedPlayerId;

  public SelectLandlordMessage() {}

  public SelectLandlordMessage(UUID playerId, UUID selectedPlayerId) {
    super(playerId);
    this.selectedPlayerId = selectedPlayerId;
  }

  public UUID getSelectedPlayerId() {
    return selectedPlayerId;
  }

  public void setSelectedPlayerId(UUID selectedPlayerId) {
    this.selectedPlayerId = selectedPlayerId;
  }
}
