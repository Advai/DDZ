package com.yourco.ddz.server.repository;

import com.yourco.ddz.server.api.dto.SessionStatsDto;
import com.yourco.ddz.server.persistence.GameResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {
  List<GameResult> findByGameId(String gameId);

  List<GameResult> findByUserId(UUID userId);

  List<GameResult> findByUserIdOrderByCompletedAtDesc(UUID userId);

  @Query(
      """
      SELECT new com.yourco.ddz.server.api.dto.SessionStatsDto(
          CAST(gr.userId AS string),
          u.username,
          u.displayName,
          SUM(gr.finalScore),
          SUM(CASE WHEN gr.wasLandlord = true AND gr.finalScore > 0 THEN 1 ELSE 0 END),
          SUM(CASE WHEN gr.wasLandlord = false AND gr.finalScore > 0 THEN 1 ELSE 0 END),
          SUM(CASE WHEN gr.finalScore > 0 THEN 1 ELSE 0 END),
          COUNT(gr)
      )
      FROM GameResult gr
      JOIN User u ON gr.userId = u.userId
      WHERE gr.sessionId = :sessionId
      GROUP BY gr.userId, u.username, u.displayName
      ORDER BY SUM(gr.finalScore) DESC
      """)
  List<SessionStatsDto> getSessionStats(@Param("sessionId") String sessionId);
}
