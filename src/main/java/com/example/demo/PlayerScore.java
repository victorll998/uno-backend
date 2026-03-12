package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity  // ← tells JPA "this class is a database table"
@Table(name = "player_scores")
public class PlayerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto increment ID
    private Long id;

    private String playerName;
    private int cardsDrawn;
    private int finalHandSize;
    private LocalDateTime playedAt;  // timestamp

    // Constructor for saving a new score
    public PlayerScore(String playerName, int cardsDrawn, int finalHandSize) {
        this.playerName = playerName;
        this.cardsDrawn = cardsDrawn;
        this.finalHandSize = finalHandSize;
        this.playedAt = LocalDateTime.now();
    }

    // Required by JPA — always needs a no-arg constructor
    public PlayerScore() {}

    // Getters
    public Long getId() { return id; }
    public String getPlayerName() { return playerName; }
    public int getCardsDrawn() { return cardsDrawn; }
    public int getFinalHandSize() { return finalHandSize; }
    public LocalDateTime getPlayedAt() { return playedAt; }
}
