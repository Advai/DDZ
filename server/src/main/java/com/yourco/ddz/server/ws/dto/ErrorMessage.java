package com.yourco.ddz.server.ws.dto;

/** WebSocket error message sent when an action fails. */
public class ErrorMessage {
  private String type = "ERROR";
  private String error;

  public ErrorMessage() {}

  public ErrorMessage(String error) {
    this.error = error;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}
