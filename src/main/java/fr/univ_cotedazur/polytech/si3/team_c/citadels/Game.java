package fr.univ_cotedazur.polytech.si3.team_c.citadels;

import fr.univ_cotedazur.polytech.si3.team_c.citadels.characters.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Game {
    private static final Logger LOGGER = Logger.getGlobal();
    private final Map<Character, Player> characterPlayerMap;
    private List<Player> playerList;

    private Deck deck;

    private int crown;
    private int currentTurn = 0;
    private final Map<Action, Player> eventActions;

    /**
     * The characters the player can interact with
     */
    private List<Character> charactersToInteractWith;
    private final Random random = new Random();

    public Game() {
        this(Collections.emptyList());
    }

    public Game(int numberPlayers, Player... players) {
        this(List.of(players));
        int initLength = playerList.size();
        for (int i = 1; i <= numberPlayers - initLength; i++) {
            Bot bot = new Bot("bot" + i, 2, deck.draw(2));
            playerList.add(bot);
            bot.setPlayers(() -> new ArrayList<>(playerList.stream().filter(player -> !player.equals(bot)).toList()));
        }
    }

    public Game(List<Player> players) {
        deck = new Deck();
        playerList = new ArrayList<>(players);
        charactersToInteractWith = new ArrayList<>();
        characterPlayerMap = new HashMap<>();
        for (Player p : playerList) {
            p.pickDistrictsFromDeck(deck.draw(2), 2);
            p.setPlayers(() -> new ArrayList<>(playerList.stream().filter(player -> !player.equals(p)).toList()));
        }
        eventActions = new EnumMap<>(Action.class);
    }

    public List<Player> getPlayerList() {
        return new ArrayList<>(playerList);
    }

    public List<IPlayer> getIPlayerList() {
        return new ArrayList<>(playerList);
    }

    public Deck getDeck() {
        return deck;
    }

    /**
     * Add a player to the game
     */
    protected void addPlayer(Player player) {
        player.setPlayers(() -> new ArrayList<>(playerList.stream().filter(p -> !p.equals(player)).toList()));
        if (playerList == null) playerList = new ArrayList<>(List.of(player));
        else this.playerList.add(player);
    }

    protected void setDefaultDeck() {
        this.deck = new Deck();
    }

    public int getCrown() {
        return crown;
    }

    public void setCrown(Player player) {
        setCrown(playerList.indexOf(player));
    }

    private void setCrown(int player) {
        crown = player;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public List<Character> getCharactersToInteractWith() {
        return new ArrayList<>(charactersToInteractWith);
    }

    public void registerPlayerForEventAction(Player player, Action eventAction) {
        eventActions.put(eventAction, player);
    }

    public void unregisterPlayerForEventAction(Player player, Action eventAction) {
        if (eventActions.get(eventAction) == player) eventActions.remove(eventAction);
    }

    public <T> void callEventAction(Action eventAction, Player caller, T param) {
        if (!eventActions.containsKey(eventAction)) return;
        String text = eventAction.doEventAction(this, caller, eventActions.get(eventAction), param);
        if (text != null)
            LOGGER.info(text);
    }


    public void start() {
        if (playerList.isEmpty()) throw new IllegalStateException("No players in this game");
        LOGGER.log(Level.INFO, "Game starts");
        setCrown(random.nextInt(playerList.size()));
        for (int i = 1; true; i++) {
            LOGGER.log(Level.INFO, "===== Turn {0} =====", i);
            currentTurn = i;
            if (gameTurn()) break;
        }
        LOGGER.log(Level.INFO, this::winnersDisplay);
        LOGGER.log(Level.INFO, "Game ends");
    }

    /**
     * Reset the list of characters
     */
    public static List<Character> defaultCharacterList() {
        return new ArrayList<>(List.of(new Assassin(), new Thief(), new Magician(), new King(),
                new Bishop(), new Merchant(), new Architect(), new Warlord()));
    }

    /**
     * Each player selects a character in the character list
     */
    public void characterSelectionTurn() {
        List<Character> characterList = defaultCharacterList();
        int p = getCrown();
        for (int i = 0; i < playerList.size(); i++) {
            var player = playerList.get((p + i) % playerList.size());
            List<IPlayer> beforePlayers;
            if ((p + i) % playerList.size() < p) {
                beforePlayers = new ArrayList<>(playerList.subList(p, playerList.size()));
                beforePlayers.addAll(playerList.subList(0, (p + i) % playerList.size()));
            } else beforePlayers = new ArrayList<>(playerList.subList(p, (p + i) % playerList.size()));
            var choosenCharacter = player.pickCharacter(characterList);
            characterPlayerMap.put(choosenCharacter, player);
            LOGGER.log(Level.INFO, "{0} has chosen the {1}", new Object[]{player.getName(), choosenCharacter});
            player.setPossibleCharacters(characterList, beforePlayers);
            characterList.remove(choosenCharacter);
        }
    }

    /**
     * Player chooses the action he wants to play during his turn
     */
    public void playerTurn(Player player) {
        LOGGER.info(player::toString);
        player.createActionSet();
        charactersToInteractWith.remove(player.getCharacter().orElseThrow());
        if (player.sufferAction(SufferedActions.STOLEN)) {
            Player robber = (Player) player.actionCommitter(SufferedActions.STOLEN).orElseThrow();
            LOGGER.log(Level.INFO, "{0} was robbed because he was the {1}", new Object[]{player.getName(), player.getCharacter().orElseThrow()});
            LOGGER.log(Level.INFO, "{0} gains {1} coins from {2} and has now {3} coins",
                    new Object[]{robber.getName(), player.getCoins(), player.getName(), player.getCoins() + robber.getCoins()});

            robber.gainCoins(player.getCoins());
            player.pay(player.getCoins());
            // The player who has been robbed give all his coins to the Thief
        }
        if (player.sufferAction(SufferedActions.KILLED)) {
            LOGGER.log(Level.INFO, "{0} was killed because he was the {1}", new Object[]{player.getName(), player.getCharacter().orElseThrow()});
            return;
        }
        Action startOfTurnAction = player.playStartOfTurnAction();
        if (startOfTurnAction != Action.NONE)
            startOfTurnAction.doAction(this, player);

        Action action;
        while ((action = player.nextAction()) != Action.NONE) {
            LOGGER.log(Level.INFO, "{0} wants to {1}", new Object[]{player.getName(), action.getDescription()});
            LOGGER.info(action.doAction(this, player));
            player.removeAction(action);
        }
    }

    /**
     * The method which checks if the game must end according to the number of districts built for the player
     */
    public boolean end(IPlayer player) {
        return player.getBuiltDistricts().size() >= 8;
    }

    /**
     * Defines a round to play in the game
     */
    public boolean gameTurn() {
        int previousCrown = getCrown();
        charactersToInteractWith = defaultCharacterList();
        characterSelectionTurn();
        LOGGER.log(Level.INFO, "The game turn begins");
        boolean isEnd = false;
        for (Character character : defaultCharacterList()) {
            if (characterPlayerMap.containsKey(character)) {
                Player player = characterPlayerMap.get(character);
                LOGGER.log(Level.INFO, "It is now {0}''s turn", character);
                playerTurn(player);
                if (end(player)) {
                    if (!isEnd) player.endsGame();
                    isEnd = true;
                }
            }
        }
        Optional<Character> characterKing = playerList.get(previousCrown).getCharacter();
        if (getCrown() == previousCrown && characterKing.isPresent() && !characterKing.get().startTurnAction().equals(Action.GET_CROWN))
            setCrown((getCrown() + 1) % playerList.size());
        return isEnd;
    }

    /**
     * @return a tuple having as key the list of winning players and as value the score
     */
    public SimpleEntry<List<Player>, Integer> getWinners() {
        List<Player> winners = new ArrayList<>();
        int max = 0;
        for (Player player : playerList) {
            int score = player.getScore(currentTurn);
            if (score > max) {
                winners = new ArrayList<>(List.of(player));
                max = score;
            } else if (score == max) winners.add(player);
        }
        return new SimpleEntry<>(winners, max);
    }

    /**
     * @return the string for the winners display
     */
    public String winnersDisplay() {
        SimpleEntry<List<Player>, Integer> winners = getWinners();
        StringBuilder result = new StringBuilder();
        if (winners.getKey().size() == 1)
            result.append("The player ").append(winners.getKey().get(0).getName()).append(" won");
        else
            result.append("There is an equality between players : ")
                    .append(winners.getKey().stream().map(Player::getName).collect(Collectors.joining(", ")));
        return result.append(" with ").append(winners.getValue()).append(" points !").toString();
    }

    public static void main(String... args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$s] %5$s%6$s%n");
        new Game(4).start();
    }
}
