package com.yourco.ddz.server.api.dto;

import com.yourco.ddz.engine.core.PlayedHand;
import java.util.List;

public record PlayedHandDto(String comboType, List<CardDto> cards) {
  public static PlayedHandDto from(PlayedHand hand) {
    if (hand == null) {
      return null;
    }
    return new PlayedHandDto(hand.type().name(), hand.cards().stream().map(CardDto::from).toList());
  }
}
