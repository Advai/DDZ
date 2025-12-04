package com.yourco.ddz.server.repository;

import com.yourco.ddz.server.persistence.Game;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
  Optional<Game> findByJoinCode(String joinCode);
}
