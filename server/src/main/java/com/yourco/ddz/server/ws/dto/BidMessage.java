package com.yourco.ddz.server.ws.dto;

import java.util.UUID;

/** WebSocket message for placing a bid. */
public class BidMessage extends GameActionMessage {
  private int bidValue;

  public BidMessage() {}

  public BidMessage(UUID playerId, int bidValue) {
    super(playerId);
    this.bidValue = bidValue;
  }

  public int getBidValue() {
    return bidValue;
  }

  public void setBidValue(int bidValue) {
    this.bidValue = bidValue;
  }
}
