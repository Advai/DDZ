package com.yourco.ddz.server.core;

import static org.junit.jupiter.api.Assertions.*;

import com.yourco.ddz.server.service.GamePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for GameRegistry. */
class GameRegistryTest {

  private GameRegistry registry;

  @Mock private GamePersistenceService mockPersistenceService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    registry = new GameRegistry(mockPersistenceService);
  }

  @Test
  void testCreateGame() {
    GameInstance instance = registry.createGame(3);

    assertNotNull(instance);
    assertEquals(3, instance.maxPlayers());
    assertNotNull(instance.gameId());
    assertTrue(instance.gameId().startsWith("g-"));
  }

  @Test
  void testGetGame() {
    GameInstance created = registry.createGame(3);

    GameInstance retrieved = registry.get(created.gameId());
    assertNotNull(retrieved);
    assertEquals(created.gameId(), retrieved.gameId());
  }

  @Test
  void testGetNonExistentGame() {
    GameInstance retrieved = registry.get("nonexistent-game-id");
    assertNull(retrieved);
  }

  @Test
  void testGetJoinCode() {
    GameInstance instance = registry.createGame(3);

    String joinCode = registry.getJoinCode(instance.gameId());
    assertNotNull(joinCode);
    assertTrue(joinCode.length() >= 4 && joinCode.length() <= 8); // Join codes are short strings
  }

  @Test
  void testGetByJoinCode() {
    GameInstance created = registry.createGame(3);
    String joinCode = registry.getJoinCode(created.gameId());

    GameInstance retrieved = registry.getByJoinCode(joinCode);
    assertNotNull(retrieved);
    assertEquals(created.gameId(), retrieved.gameId());
  }

  @Test
  void testGetByInvalidJoinCode() {
    GameInstance retrieved = registry.getByJoinCode("INVALID");
    assertNull(retrieved);
  }

  @Test
  void testGetAllGames() {
    assertTrue(registry.getAllGames().isEmpty());

    registry.createGame(3);
    registry.createGame(5);

    assertEquals(2, registry.getAllGames().size());
  }

  @Test
  void testMultipleGamesHaveUniqueJoinCodes() {
    GameInstance game1 = registry.createGame(3);
    GameInstance game2 = registry.createGame(3);

    String code1 = registry.getJoinCode(game1.gameId());
    String code2 = registry.getJoinCode(game2.gameId());

    assertNotEquals(code1, code2);
  }
}
