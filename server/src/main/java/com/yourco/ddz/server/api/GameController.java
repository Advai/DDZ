package com.yourco.ddz.server.api;
import com.yourco.ddz.server.core.GameRegistry; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/games")
public class GameController {
  private final GameRegistry registry; public GameController(GameRegistry r){ this.registry=r; }
  @PostMapping public ResponseEntity<?> createGame(){ var g=registry.createGame(); return ResponseEntity.ok(Map.of("gameId", g.gameId())); }
}