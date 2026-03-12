// backend/src/main/java/com/yourgame/GameState.java
package com.example.demo;

import java.util.*;

public class GameState {
    private String gameId;
    private List<String> deck;        // remaining cards
    private List<String> playerHand;  // cards player is holding
    private List<String> discardPile;   
    private List<String> cpuHand;        // CPU opponent
    private int turnNumber;
    private String status;            // "playing" or "finished"
    private String message;          // feedback to player
    private String currentTurn;          // "player" or "cpu"
    private int pendingDrawCount;        //  cards next player must draw
    private boolean unoCallRequired;   // player is on 1 card and hasn't called it
    private boolean unoCalled;         // player successfully called UNO!


    // Constructor — builds a fresh game
    public GameState(String gameId) {
        this.gameId = gameId;
        this.deck = new ArrayList<>();
        this.playerHand = new ArrayList<>();
        this.discardPile = new ArrayList<>(); 
        this.cpuHand = new ArrayList<>();
        this.turnNumber = 1;
        this.currentTurn = "player";     // player always goes first
        this.message = "";
        this.pendingDrawCount = 0;
        this.status = "playing";
        this.unoCallRequired = false;
        this.unoCalled = false;
    }

    // Getters and setters (VS Code can auto-generate these)
    public String getGameId() { return gameId; }
    public List<String> getDeck() { return deck; }
    public List<String> getPlayerHand() { return playerHand; }
    public int getTurnNumber() { return turnNumber; }
    public String getStatus() { return status; }
    public void setTurnNumber(int t) { this.turnNumber = t; }
    public void setStatus(String s) { this.status = s; }
    public List<String> getDiscardPile() { return discardPile; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getTopCard() {
        if (discardPile.isEmpty()) return null;
        return discardPile.get(discardPile.size() - 1);
    }
    public List<String> getCpuHand() { return cpuHand; }
    public String getCurrentTurn() { return currentTurn; }
    public int getPendingDrawCount() { return pendingDrawCount; }
    public void setCurrentTurn(String t) { this.currentTurn = t; }
    public void setPendingDrawCount(int c) { this.pendingDrawCount = c; }
    public boolean isUnoCallRequired() { return unoCallRequired; }
    public boolean isUnoCalled() { return unoCalled; }
    public void setUnoCallRequired(boolean u) { this.unoCallRequired = u; }
    public void setUnoCalled(boolean u) { this.unoCalled = u; }
}