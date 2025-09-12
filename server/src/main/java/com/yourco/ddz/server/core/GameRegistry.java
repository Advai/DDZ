package com.yourco.ddz.server.core;
import org.springframework.stereotype.Component; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
@Component public class GameRegistry {
  private final Map<String, GameInstance> games=new ConcurrentHashMap<>();
  public GameInstance createGame(){ var id="g-"+UUID.randomUUID(); var gi=GameInstance.newDemo(id); games.put(id,gi); return gi; }
  public GameInstance get(String id){ return games.get(id); }
}