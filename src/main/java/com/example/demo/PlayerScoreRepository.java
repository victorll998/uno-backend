package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// JpaRepository<YourClass, IDType> — Spring generates all DB code
public interface PlayerScoreRepository extends JpaRepository<PlayerScore, Long> {

    // Spring auto-generates this query just from the method name!
    // SELECT * FROM player_scores WHERE player_name = ?
    List<PlayerScore> findByPlayerNameOrderByPlayedAtDesc(String playerName);
}
