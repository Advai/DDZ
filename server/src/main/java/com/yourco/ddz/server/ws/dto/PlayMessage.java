package com.yourco.ddz.server.ws.dto;

import com.yourco.ddz.server.api.dto.CardDto;
import java.util.List;
import java.util.UUID;

/** WebSocket message for playing cards. */
public class PlayMessage extends GameActionMessage {
  private List<CardDto> cards;

  public PlayMessage() {}

  public PlayMessage(UUID playerId, List<CardDto> cards) {
    super(playerId);
    this.cards = cards;
  }

  public List<CardDto> getCards() {
    return cards;
  }

  public void setCards(List<CardDto> cards) {
    this.cards = cards;
  }
}
