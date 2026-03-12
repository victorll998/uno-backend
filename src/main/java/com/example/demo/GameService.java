// backend/src/main/java/com/yourgame/GameService.java
package com.example.demo;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GameService {

    private final PlayerScoreRepository scoreRepository;
    private Map<String, GameState> games = new HashMap<>();

    public GameService(PlayerScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    // ─── Create Game ────────────────────────────────────────────
    public GameState createGame() {
        String gameId = UUID.randomUUID().toString();
        GameState game = new GameState(gameId);

        buildUnoDeck(game);
        Collections.shuffle(game.getDeck());

        // Deal 7 cards to player
        for (int i = 0; i < 7; i++) {
            game.getPlayerHand().add(game.getDeck().remove(0));
        }

        // Deal 7 cards to CPU
        for (int i = 0; i < 7; i++) {
            game.getCpuHand().add(game.getDeck().remove(0));
        }

        // Flip first non-wild card to start discard pile
        String firstCard = game.getDeck().remove(0);
        while (firstCard.startsWith("Wild")) {
            game.getDeck().add(firstCard);
            firstCard = game.getDeck().remove(0);
        }
        game.getDiscardPile().add(firstCard);
        game.setMessage("Game started! Top card: " + firstCard);

        games.put(gameId, game);
        return game;
    }

    // ─── Draw Card ───────────────────────────────────────────────
    public GameState drawCard(String gameId) {
        GameState game = games.get(gameId);
        if (game == null) throw new RuntimeException("Game not found");

        // Block draw if it's CPU's turn
        if (!game.getCurrentTurn().equals("player")) {
            game.setMessage("It's not your turn!");
            return game;
        }

        if (game.getDeck().isEmpty()) {
            game.setMessage("Deck is empty!");
            return game;
        }

        // If there's a pending draw penalty, draw those cards
        int cardsToDraw = game.getPendingDrawCount() > 0
            ? game.getPendingDrawCount() : 1;

        for (int i = 0; i < cardsToDraw; i++) {
            if (!game.getDeck().isEmpty()) {
                game.getPlayerHand().add(game.getDeck().remove(0));
            }
        }

        game.setPendingDrawCount(0);
        game.setUnoCallRequired(false);
        game.setUnoCalled(false);
        game.setMessage("You drew " + cardsToDraw + " card(s).");
        game.setTurnNumber(game.getTurnNumber() + 1);
        game.setCurrentTurn("cpu");

        // CPU takes its turn automatically
        cpuTurn(game);

        return game;
    }

    // ─── Play Card ───────────────────────────────────────────────
    public GameState playCard(String gameId, int cardIndex, String chosenColor) {
        GameState game = games.get(gameId);
        if (game == null) throw new RuntimeException("Game not found");

        // Block play if not player's turn
        if (!game.getCurrentTurn().equals("player")) {
            game.setMessage("It's not your turn!");
            return game;
        }

        // Check if player forgot to call UNO last turn
        if (game.isUnoCallRequired()) {
            // Player is playing again without having called UNO — penalise!
            for (int i = 0; i < 2; i++) {
                if (!game.getDeck().isEmpty()) {
                    game.getPlayerHand().add(game.getDeck().remove(0));
                }
            }
            game.setUnoCallRequired(false);
            game.setUnoCalled(false);
            game.setMessage("⚠️ You forgot to call UNO! Draw 2 penalty cards.");
            return game;
        }

        // Block play if player has a pending draw penalty
        if (game.getPendingDrawCount() > 0) {
            game.setMessage("You must draw " + game.getPendingDrawCount()
                + " cards first!");
            return game;
        }

        List<String> hand = game.getPlayerHand();
        if (cardIndex < 0 || cardIndex >= hand.size()) {
            throw new RuntimeException("Invalid card index");
        }

        String cardToPlay = hand.get(cardIndex);
        String topCard = game.getTopCard();

        // Validate the move
        if (!isValidPlay(cardToPlay, topCard)) {
            game.setMessage("❌ Can't play " + cardToPlay
                + " on " + topCard);
            return game;
        }

        // Play the card
        hand.remove(cardIndex);
        game.getDiscardPile().add(cardToPlay);
        game.setTurnNumber(game.getTurnNumber() + 1);

        // Detect if player is now on 1 card
        if (hand.size() == 1) {
            if (!game.isUnoCalled()) {
                // They haven't called UNO yet — flag it
                game.setUnoCallRequired(true);
                game.setMessage(game.getMessage() + " | ⚠️ Call UNO!");
            }
        } else {
            // Reset UNO flags if they draw back up
            game.setUnoCallRequired(false);
            game.setUnoCalled(false);
        }

        // Check win
        if (hand.isEmpty()) {
            game.setStatus("finished");
            game.setMessage("🎉 You win! UNO!");
            return game;
        }

        // Apply special card effects
        applyCardEffect(game, cardToPlay, chosenColor, "cpu");

        // CPU takes its turn if it's now CPU's turn
        if (game.getCurrentTurn().equals("cpu")) {
            cpuTurn(game);
        }

        return game;
    }

    // ─── Card Effects ────────────────────────────────────────────
    private void applyCardEffect(GameState game, String card,
                                  String chosenColor, String nextPlayer) {
        if (card.contains("Skip") || card.contains("Reverse")) {
            // Skip and Reverse both skip the next player (2-player game)
            game.setMessage("✅ Played " + card + " — " + nextPlayer
                + " is skipped! Your turn again.");
            game.setCurrentTurn("player");  // player goes again

        } else if (card.contains("Draw Two")) {
            game.setPendingDrawCount(2);
            game.setMessage("✅ Played Draw Two — "
                + nextPlayer + " must draw 2!");
            game.setCurrentTurn(nextPlayer);

        } else if (card.startsWith("Wild Draw Four")) {
            game.setPendingDrawCount(4);
            // Apply chosen color by adding a fake top card
            String colorCard = chosenColor + " Wild";
            game.getDiscardPile().add(colorCard);
            game.setMessage("✅ Wild Draw Four! Color: "
                + chosenColor + " — " + nextPlayer + " draws 4!");
            game.setCurrentTurn(nextPlayer);

        } else if (card.startsWith("Wild")) {
            // Change color
            String colorCard = chosenColor + " Wild";
            game.getDiscardPile().add(colorCard);
            game.setMessage("✅ Wild! Color changed to " + chosenColor);
            game.setCurrentTurn(nextPlayer);

        } else {
            // Normal card — just pass turn
            game.setMessage("✅ Played " + card);
            game.setCurrentTurn(nextPlayer);
        }
    }

    // ─── CPU Turn ────────────────────────────────────────────────
    private void cpuTurn(GameState game) {
        if (!game.getCurrentTurn().equals("cpu")) return;
        if (game.getStatus().equals("finished")) return;

        List<String> cpuHand = game.getCpuHand();
        String topCard = game.getTopCard();

        // Handle pending draw penalty first
        if (game.getPendingDrawCount() > 0) {
            int count = game.getPendingDrawCount();
            for (int i = 0; i < count; i++) {
                if (!game.getDeck().isEmpty()) {
                    cpuHand.add(game.getDeck().remove(0));
                }
            }
            game.setPendingDrawCount(0);
            game.setMessage(game.getMessage()
                + " | CPU drew " + count + " cards.");
            game.setCurrentTurn("player");
            return;
        }

        // Find a valid card to play
        int playableIndex = -1;
        for (int i = 0; i < cpuHand.size(); i++) {
            if (isValidPlay(cpuHand.get(i), topCard)) {
                playableIndex = i;
                break;
            }
        }

        if (playableIndex == -1) {
            // CPU has no valid card — draws one
            if (!game.getDeck().isEmpty()) {
                String drawn = game.getDeck().remove(0);
                cpuHand.add(drawn);
                game.setMessage(game.getMessage() + " | CPU drew a card.");
            }
            game.setCurrentTurn("player");
            return;
        }

        // CPU plays the card
        String cpuCard = cpuHand.remove(playableIndex);
        game.getDiscardPile().add(cpuCard);

        // Check CPU win
        if (cpuHand.isEmpty()) {
            game.setStatus("finished");
            game.setMessage("💀 CPU wins! Better luck next time.");
            return;
        }

        // CPU picks red for wilds (simple strategy)
        String cpuChosenColor = "Red";
        applyCardEffect(game, cpuCard, cpuChosenColor, "player");
        game.setMessage(game.getMessage().replace("✅", "🤖 CPU played"));
    }

    // ─── Validation ──────────────────────────────────────────────
    private boolean isValidPlay(String cardToPlay, String topCard) {
        if (cardToPlay.startsWith("Wild")) return true;

        String[] playParts = cardToPlay.split(" ", 2);
        String[] topParts = topCard.split(" ", 2);

        if (playParts.length < 2 || topParts.length < 2) return false;

        String playColor = playParts[0];
        String playValue = playParts[1];
        String topColor  = topParts[0];
        String topValue  = topParts[1];

        return playColor.equals(topColor) || playValue.equals(topValue);
    }

    // ─── Build Deck ──────────────────────────────────────────────
    private void buildUnoDeck(GameState game) {
        String[] colors = {"Red", "Green", "Blue", "Yellow"};
        String[] specials = {"Skip", "Reverse", "Draw Two"};

        for (String color : colors) {
            game.getDeck().add(color + " 0");
            for (int num = 1; num <= 9; num++) {
                game.getDeck().add(color + " " + num);
                game.getDeck().add(color + " " + num);
            }
            for (String special : specials) {
                game.getDeck().add(color + " " + special);
                game.getDeck().add(color + " " + special);
            }
        }

        for (int i = 0; i < 4; i++) game.getDeck().add("Wild");
        for (int i = 0; i < 4; i++) game.getDeck().add("Wild Draw Four");
    }

    // ─── Score / History ─────────────────────────────────────────
    public PlayerScore saveScore(String gameId, String playerName) {
        GameState game = games.get(gameId);
        if (game == null) throw new RuntimeException("Game not found");
        PlayerScore score = new PlayerScore(
            playerName,
            game.getTurnNumber() - 1,
            game.getPlayerHand().size()
        );
        return scoreRepository.save(score);
    }

    public List<PlayerScore> getScores(String playerName) {
        return scoreRepository
            .findByPlayerNameOrderByPlayedAtDesc(playerName);
    }

    public GameState getGame(String gameId) {
        return games.get(gameId);
    }

    // Player clicks the UNO! button
    public GameState callUno(String gameId) {
        GameState game = games.get(gameId);
        if (game == null) throw new RuntimeException("Game not found");

        if (game.getPlayerHand().size() == 1) {
            game.setUnoCalled(true);
            game.setUnoCallRequired(false);
            game.setMessage("🗣️ UNO! One card left!");
        } else {
            game.setMessage("You can only call UNO with 1 card!");
        }

        return game;
    }

    public List<PlayerScore> getAllScores() {
    return scoreRepository.findAll(
        org.springframework.data.domain.Sort
            .by(org.springframework.data.domain.Sort.Direction.DESC, "playedAt")
    );
}
}