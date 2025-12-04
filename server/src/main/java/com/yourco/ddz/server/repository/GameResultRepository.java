package com.yourco.ddz.server.repository;

import com.yourco.ddz.server.persistence.GameResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {
  List<GameResult> findByGameId(String gameId);

  List<GameResult> findByUserId(UUID userId);

  List<GameResult> findByUserIdOrderByCompletedAtDesc(UUID userId);
}
