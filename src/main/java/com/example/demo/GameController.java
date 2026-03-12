// backend/src/main/java/com/yourgame/GameController.java
package com.example.demo;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController           // handles HTTP requests
@RequestMapping("/api/game")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "https://uno-frontend.vercel.app"  // ← your actual Vercel URL
})
public class GameController {

    private final GameService gameService;

    // Spring automatically injects GameService here
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // POST /api/game/new → start a game
    @PostMapping("/new")
    public GameState newGame() {
        return gameService.createGame();
    }

    // GET /api/game/{id} → get current state
    @GetMapping("/{gameId}")
    public GameState getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }

    // POST /api/game/{id}/draw → draw a card
    @PostMapping("/{gameId}/draw")
    public GameState drawCard(@PathVariable String gameId) {
        return gameService.drawCard(gameId);
    }


    // POST /api/game/{id}/finish?playerName=Alice → save score
    @PostMapping("/{gameId}/finish")
    public PlayerScore finishGame(
        @PathVariable String gameId,
        @RequestParam String playerName) {
        return gameService.saveScore(gameId, playerName);
    }

    // GET /api/game/scores?playerName=Alice → get history
    @GetMapping("/scores")
    public List<PlayerScore> getScores(@RequestParam String playerName) {
        return gameService.getScores(playerName);
    }

    // POST /api/game/{id}/play?cardIndex=2 → play a card
    @PostMapping("/{gameId}/play")
    public GameState playCard(
        @PathVariable String gameId,
        @RequestParam int cardIndex,
        @RequestParam(defaultValue = "Red") String chosenColor) {
        return gameService.playCard(gameId, cardIndex, chosenColor);
    }

    // POST /api/game/{id}/uno → player calls UNO!
    @PostMapping("/{gameId}/uno")
    public GameState callUno(@PathVariable String gameId) {
        return gameService.callUno(gameId);
    }

    // GET /api/game/scores/all → get everyone's scores
    @GetMapping("/scores/all")
    public List<PlayerScore> getAllScores() {
        return gameService.getAllScores();
    }
}