package fr.univ_cotedazur.polytech.si3.team_c.citadels;

import fr.univ_cotedazur.polytech.si3.team_c.citadels.characters.*;
import fr.univ_cotedazur.polytech.si3.team_c.citadels.districts.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BotTest {

    Player player1, player2;
    Bot bot1, bot2;

    @BeforeEach
    void setUp() {
        player1 = bot1 = new Bot("Bot 1", 2, List.of(new Battlefield(), new Castle(), new Church(), new DragonGate()));
        player2 = bot2 = new Bot("Bot 2", 2, List.of(new Battlefield(), new Castle(), new Church(), new DragonGate(), new Docks(), new Laboratory()));
    }

    @Test
    void coins() {
        assertEquals(2, player1.getCoins());
        assertFalse(player1.pay(3));
        assertTrue(player1.pay(2));
        assertFalse(player1.pay(-1));
        assertEquals(0, player1.getCoins());
        assertEquals(Action.INCOME, player1.nextAction(Set.of(Action.DRAW, Action.INCOME))); // The player has no money and should therefore get some gold
    }

    @Test
    void objective() {
        var objective = bot1.districtObjective();
        assertTrue(objective.isPresent());
        assertEquals(objective, bot1.districtObjective()); // The objective should be consistent
        assertEquals(new DragonGate(), objective.get());

        Bot bot = new Bot(player1.getName(), 1500, List.of(new Cathedral(), new Palace(), new TownHall(), new Fortress()));
        objective = bot.districtObjective();
        assertTrue(objective.isPresent());
        assertEquals(objective, bot.districtObjective()); // Even with all profitability being equals the objective should be consistent
    }

    @Test
    void basicActions() {
        assertEquals(Action.INCOME, player1.nextAction(Set.of(Action.DRAW, Action.INCOME, Action.BUILD))); // The player already has a lot of card, he should get some coins
        player1.gainCoins(4); // To be sure the player will build something we give 4 golds and not wait for having enough to build the DragonGate
        assertEquals(Action.BUILD, player1.nextAction(Set.of(Action.BUILD)));
        assertTrue(player1.buildDistrict(new DragonGate(), 0));
        assertEquals(List.of(new DragonGate()), player1.getBuiltDistricts());
        assertEquals(0, player1.getCoins());

        assertEquals(Action.INCOME, player1.nextAction(Set.of(Action.DRAW, Action.INCOME, Action.BUILD))); // The player has no gold he should definitely get some
        player1.gainCoins(1); // So the player can't build anything
        assertEquals(Action.NONE, player1.nextAction(Set.of(Action.BUILD))); // The player has nothing to build, he should end his turn there

        player1.gainCoins(20); // So the player have enough to build everything in hand, and so he doesn't get coins
        for (var district : player1.getHandDistricts())
            assertTrue(player1.buildDistrict(district, 0)); // We build everything remaining to force the player to draw cards
        assertEquals(Action.DRAW, player1.nextAction(Set.of(Action.DRAW, Action.INCOME, Action.BUILD))); // The player already has a lot of gold but no cards
    }

    @Test
    void specialIncome() {
        player1.addDistrictToHand(new WatchTower());
        player1.addDistrictToHand(new Prison());
        player1.setCharacter(new King());
        player1.gainCoins(6);
        assertTrue(player1.buildDistrict(new DragonGate(), 0));
        assertEquals(0, player1.gainSpecialIncome()); // The player has no yellow districts

        assertEquals(Action.INCOME, player1.nextAction(Set.of(Action.DRAW, Action.BUILD, Action.SPECIAL_INCOME, Action.INCOME))); // The player has a lot of cards in hand
        assertEquals(2, player1.gainIncome());
        assertEquals(4, player1.getCoins());
        var action = player1.nextAction(Set.of(Action.BUILD, Action.SPECIAL_INCOME));
        assertNotEquals(Action.SPECIAL_INCOME, action); // The player didn't build anything, he clearly shouldn't claim his special income now
        assertEquals(action, player1.nextAction(Set.of(Action.BUILD, Action.SPECIAL_INCOME))); // The player shouldn't change his mind
        assertEquals(Action.NONE, player1.nextAction(Set.of(Action.SPECIAL_INCOME))); // The player has no yellow districts built

        assertEquals(Action.INCOME, player1.nextAction(Set.of(Action.DRAW, Action.BUILD, Action.SPECIAL_INCOME, Action.INCOME))); // The player has a lot of cards in hand
        assertEquals(2, player1.gainIncome());
        assertEquals(6, player1.getCoins());
        assertEquals(Action.BUILD, player1.nextAction(Set.of(Action.BUILD, Action.SPECIAL_INCOME))); // The player has enough to build whatever he wants
        assertTrue(player1.buildDistrict(new WatchTower(), 0));
        assertEquals(Action.NONE, player1.nextAction(Set.of(Action.SPECIAL_INCOME))); // The player has no yellow districts built

        player1.pickCharacter(List.of(new Merchant(), new King(), new Bishop()));
        assertEquals(Action.INCOME, player1.nextAction(Set.of(Action.DRAW, Action.BUILD, Action.SPECIAL_INCOME, Action.INCOME))); // The player has a lot of cards in hand
        assertEquals(2, player1.gainIncome());
        assertEquals(7, player1.getCoins());
        assertEquals(Action.BUILD, player1.nextAction(Set.of(Action.BUILD, Action.SPECIAL_INCOME))); // The player has enough to build whatever he wants
        assertTrue(player1.buildDistrict(new Prison(), 0)); // We force the player to build a red card (so we can test his special income)
        assertEquals(Action.NONE, player1.nextAction(Set.of(Action.SPECIAL_INCOME))); // The player has no card of the color of his character
        assertEquals(0, player1.gainSpecialIncome());

        assertEquals(new Warlord(), player1.pickCharacter(List.of(new Merchant(), new Warlord()))); // The warlord is more profitable as the player will gain at least one more coins that with the merchant
        assertEquals(2, player1.gainSpecialIncome()); // The player has 2 red cards
    }

    @Test
    void pickCharacter() {
        assertTrue(player1.getCharacter().isEmpty());
        assertEquals(new Architect(), player1.pickCharacter(List.of(new Architect())));
        assertEquals(new Architect(), player1.getCharacter().get());
        assertEquals(new Magician(), player1.pickCharacter(List.of(new Magician())));
        assertEquals(new Magician(), player1.getCharacter().get());
    }

    @Test
    void pickDistrictsFromDeck() {
        // The player already has a castle in hand, since duplicates are not allowed, he should take the other one
        assertEquals(List.of(new Cathedral()), player1.pickDistrictsFromDeck(List.of(new Castle(), new Cathedral()), 1));

        // The player already has a castle in hand, he should take the other two
        assertEquals(List.of(new Fortress(), new Cathedral()), player1.pickDistrictsFromDeck(List.of(new Fortress(), new Cathedral(), new Church()), 2));

        // The player has no choice but to take a church, even if he already has one
        assertEquals(List.of(new Church()), player1.pickDistrictsFromDeck(List.of(new Church(), new Church()), 1));

        assertEquals(1, player1.pickDistrictsFromDeck(List.of(new Fortress(), new Cathedral()), 1).size());
        assertEquals(2, player1.pickDistrictsFromDeck(List.of(new Fortress(), new Cathedral()), 2).size());
        assertEquals(1, player1.pickDistrictsFromDeck(List.of(new Fortress(), new Cathedral(), new Graveyard()), 1).size());
    }

    @Test
    void pickDistrictsToBuild() {
        Bot bot = new Bot("bot", 9, List.of(new Battlefield(), new Castle(), new DragonGate(), new Church())) {
            @Override
            protected double districtProfitability(District district) {
                return getHandDistricts().size() - getHandDistricts().indexOf(district); // This bot wants to build the card in the order there are in his hand
            }
        };
        bot.setCharacter(new Bishop());
        assertEquals(List.of(new Battlefield()), bot.pickDistrictsToBuild(0)); // Only one district should be built, and it should be the first in his hand
        assertEquals(List.of(new Battlefield()), bot.getBuiltDistricts()); // The district has been correctly built
        assertEquals(List.of(new Castle()), bot.pickDistrictsToBuild(2)); // The player can build 2 districts but only one of his objective can be afforded
        assertEquals(List.of(new Battlefield(), new Castle()), bot.getBuiltDistricts());
    }

    @Test
    void getScore() {
        // Test bonus score with first player to end bonus and districts score
        player1.gainCoins(10000);
        List<District> districts = List.of(
                new Battlefield(), new Castle(), new Monastery(),
                new TheKeep(), new Palace(), new Temple(),
                new Manor(), new Prison(), new HauntedCity());
        for (int i = 0; i < districts.size(); i++) {
            player1.addDistrictToHand(districts.get(i));
            player1.buildDistrict(districts.get(i), i);
        }
        player1.endsGame();
        assertEquals(26, player1.getDistrictsScore());
        assertEquals(30, player1.getScore(8));

        // Test bonus score with districts score, colors bonus and eight districts bonus
        setUp();
        player1.gainCoins(10000);
        for (District district : districts) player1.addDistrictToHand(district);
        for (int i = districts.size() - 1; i >= 0; i--) {
            player1.buildDistrict(districts.get(i), districts.size() - i);
        }
        assertEquals(26, player1.getDistrictsScore());
        assertEquals(31, player1.getScore(8));
    }

    @Test
    void numberOfDistrictsToDraw() {
        Observatory observatory = new Observatory();
        Bot bot1 = new Bot("bot1", 1500, List.of(new Temple(), new Battlefield(), new Castle()
                , new Cathedral(), observatory, new Docks(), new DragonGate(), new Fortress()));
        assertEquals(2, bot1.numberOfDistrictsToDraw());
        for (District district : bot1.getHandDistricts()) {
            if (!district.equals(observatory)) bot1.buildDistrict(district, 0);
        }
        assertEquals(2, bot1.numberOfDistrictsToDraw());
        bot1.buildDistrict(observatory, 0);
        assertEquals(3, bot1.numberOfDistrictsToDraw());
    }

    @Test
    void numberOfDistrictsToKeep() {
        Library library = new Library();
        Bot bot1 = new Bot("bot1", 1500, List.of(new Temple(), new Battlefield(), new Castle()
                , new Cathedral(), library, new Docks(), new DragonGate(), new Fortress()));
        assertEquals(1, bot1.numberOfDistrictsToKeep());
        for (District district : bot1.getHandDistricts()) {
            if (!district.equals(library)) bot1.buildDistrict(district, 0);
        }
        assertEquals(1, bot1.numberOfDistrictsToKeep());
        bot1.buildDistrict(library, 0);
        assertEquals(2, bot1.numberOfDistrictsToKeep());
    }

    @Test
    void createActionSetTest() {
        player1.pickCharacter(List.of(new King()));
        player1.createActionSet();
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.SPECIAL_INCOME, Action.DRAW, Action.INCOME)), player1.getActionSet());
        player1.pickCharacter(List.of(new Bishop()));
        player1.createActionSet();
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.SPECIAL_INCOME, Action.DRAW, Action.INCOME)), player1.getActionSet());
        player1.pickCharacter(List.of(new Warlord()));
        player1.createActionSet();
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.SPECIAL_INCOME, Action.DRAW, Action.INCOME)), player1.getActionSet());
        player1.pickCharacter(List.of(new Merchant()));
        player1.createActionSet();
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.SPECIAL_INCOME, Action.DRAW, Action.INCOME)), player1.getActionSet());
        bot1.gainCoins(10);
        bot1.addDistrictToHand(new Smithy());
        bot1.addDistrictToHand(new Laboratory());
        bot1.buildDistrict(new Smithy(), 0);
        bot1.buildDistrict(new Laboratory(), 0);
        bot1.createActionSet();
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.SPECIAL_INCOME, Action.DRAW, Action.INCOME, Action.TAKE_THREE, Action.DISCARD)), player1.getActionSet());
    }

    @Test
    void removeActionSetTest() {
        player1.pickCharacter(List.of(new King()));
        player1.createActionSet();
        assertTrue(player1.removeAction(Action.SPECIAL_INCOME));
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.DRAW, Action.INCOME)), player1.getActionSet());
        assertTrue(player1.removeAction(Action.INCOME));
        assertEquals(new HashSet<>(List.of(Action.BUILD, Action.DRAW)), player1.getActionSet());
        assertTrue(player1.removeAction(Action.BUILD));
        assertEquals(new HashSet<>(List.of(Action.DRAW)), player1.getActionSet());
        assertTrue(player1.removeAction(Action.DRAW));
        assertEquals(new HashSet<>(), player1.getActionSet());
        assertFalse(player1.removeAction(Action.DRAW));
        assertEquals(new HashSet<>(), player1.getActionSet());
    }


    @Test
    void MagicianTest() {
        Game g = new Game();
        Bot bot1 = new Bot("bot 1", 2, List.of(new Battlefield(), new Castle(), new Church(), new DragonGate())) {
            @Override
            public Set<Action> createActionSet() { //Override of the createActionSet in Player Method to manipulate the actionTest of the player and test the playerTurn method of Game
                setActionSet(new HashSet<>(getCharacter().orElseThrow().getAction().orElseThrow()));
                return getActionSet();
            }

        };
        g.addPlayer(bot1);
        g.addPlayer(bot2);
        bot1.pickCharacter(List.of(new Magician())); // Create a bot with the character magician
        bot2.pickCharacter(List.of(new King()));
        assertEquals(Set.of(Action.EXCHANGE_DECK, Action.EXCHANGE_PLAYER), bot1.createActionSet());
        assertEquals(List.of(new Battlefield(), new Castle(), new Church()), bot1.chooseCardsToExchangeWithDeck());
        assertEquals(player2, bot1.choosePlayerToExchangeCards(List.of(bot2)));
        assertEquals(Action.EXCHANGE_PLAYER, bot1.nextAction());
        bot2.removeFromHand(List.of(new DragonGate(), new Docks(), new Laboratory()));
        assertNull(bot1.choosePlayerToExchangeCards(List.of(bot2)));
        bot1.removeAction(Action.EXCHANGE_DECK);
        assertEquals(Action.NONE, bot1.nextAction());
    }

    @Test
    void cardToDiscardTest() {
        assertEquals(new Church(), bot1.cardToDiscard());
        assertEquals(Action.DISCARD, bot1.nextAction(Set.of(Action.DISCARD)));
        bot1.gainCoins(14);
        bot1.getHandDistricts().forEach(d -> bot1.buildDistrict(d, 0));
        assertNotEquals(Action.DISCARD, bot1.nextAction(Set.of(Action.DISCARD)));
        bot1.addDistrictToHand(new Manor());
        assertNotEquals(Action.DISCARD, bot1.nextAction(Set.of(Action.DISCARD)));
    }

    @Test
    void cardToDiscard() {
        bot1.gainCoins(1000);
        bot1.getHandDistricts().forEach(d -> bot1.buildDistrict(d, 0));
        System.out.println(bot1.getCoins());
        assertEquals(Action.TAKE_THREE, bot1.nextAction(Set.of(Action.TAKE_THREE)));
        bot1.pay(987);
        assertNotEquals(Action.TAKE_THREE, bot1.nextAction(Set.of(Action.TAKE_THREE)));
        bot1.gainCoins(1000);
        bot1.addDistrictToHand(new Manor());
        assertNotEquals(Action.TAKE_THREE, bot1.nextAction(Set.of(Action.TAKE_THREE)));
    }
}