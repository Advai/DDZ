package com.yourco.ddz.server.repository;

import com.yourco.ddz.server.persistence.GameParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GameParticipantRepository extends JpaRepository<GameParticipant, Long> {
  List<GameParticipant> findByGameId(String gameId);

  List<GameParticipant> findByUserId(UUID userId);

  @Query("SELECT gp FROM GameParticipant gp WHERE gp.userId = :userId AND gp.leftAt IS NULL")
  Optional<GameParticipant> findActiveGameForUser(UUID userId);

  Optional<GameParticipant> findByGameIdAndUserId(String gameId, UUID userId);
}
