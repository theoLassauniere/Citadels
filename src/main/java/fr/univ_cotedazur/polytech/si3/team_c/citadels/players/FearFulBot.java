package fr.univ_cotedazur.polytech.si3.team_c.citadels.players;

import fr.univ_cotedazur.polytech.si3.team_c.citadels.Colors;
import fr.univ_cotedazur.polytech.si3.team_c.citadels.District;
import fr.univ_cotedazur.polytech.si3.team_c.citadels.IPlayer;

import java.util.Collections;
import java.util.List;

/**
 * The fearful bot has a fear of other player,
 * it tries to choose character to avoid being destroyed, killed, stolen,
 * or having its hand exchanged with somebody
 * The main fear is to have a district destroyed,
 * the second is to have its hand exchanged because it could have interesting districts,
 * the third is to be stolen, and the last is to be killed.
 * So for the same number of other player could, destroy a district, kill the bot, steal the bot
 * or exchange the hand, the destruction of a district is the most important thing for the bot
 */
public class FearFulBot extends Bot {

    /**
     * This method calculates the bot's fear of having a district destroyed.
     *
     * @return 0 if nobody can be destroyed. Otherwise, it returns 6 plus the number of players who could have a reason to destroy multiplied by 2.
     */
    @Override
    protected double destroySecurity() {
        var nb = possibleDestruction(getPlayers()).size();
        if (nb == 0) {
            return 0;
        }
        return 6. + nb * 2.; //This calculation was derived from tests and appears to be the correct factor for the fearful bot.
    }

    @Override
    protected double stealCoin() {
        var players = possibleDestruction(getPlayers());
        return Math.min(0.22 * players.stream().filter(iPlayer -> iPlayer.getCoins() > getCoins()).toList().size(), 1);
    }

    /**
     * If other players can destroy certain districts, this method gives a bonus to choosing character which can't be destroyed.
     * The number 25 is returned because it provides a significant advantage to the character which can't be destroyed, but it leaves the choice.
     * (for example if the warlord is available and the bot has several red districts)
     */
    @Override
    protected double nonDestructibleSecurity() {
        var nb = possibleDestruction(getPlayers()).size();
        if (nb == 0) {
            return 0;
        }
        return 25;
    }

    /**
     * This method calculates the bot’s fear of having its cards exchanged with another player.
     *
     * @return It returns 5 plus the number of players who could have a reason to exchange, if this number is upper than half of the number of player it multiplied by 2.
     *
     */
    @Override
    protected double exchangePlayerSecurity() {
        var nb = possibleExchange(getPlayers());
        if (nb < getPlayers().size() / 2.) {
            return 5 + nb;
        }
        return 5 + nb * 2;
    }

    /**
     * This method calculates the bot's fear of being killed.
     *
     * @return 0 if nobody could kill or 3 plus the number of players who could have a reason to kill because it is the least significant fear.
     */
    @Override
    protected double killSecurity() {
        var nb = possibleKill(getPlayers());
        if (nb == 0) {
            return 0;
        }
        return nb + 4;
    }

    /**
     * This method calculates the bot's fear of being stolen.
     *
     * @return 0 if nobody has reasons to steal the bot. Otherwise, it returns 7 plus the number of players who could have a reason to steal.
     */
    @Override
    protected double stealSecurity() {
        var nb = possibleSteal(getPlayers());
        if (nb == 0) {
            return 0;
        }
        return 7 + nb; //The factor 7 is calculated thanks to tests.
    }

    @Override
    protected double numberOfDistrictToBuildMultiplier() {
        return 0.;
    }

    public FearFulBot(String name, int coins, List<District> districts) {
        super(name, coins, districts, 0.35);
    }

    public FearFulBot(String name) {
        this(name, 0, Collections.emptyList());
    }

    @Override
    protected double districtProfitability(District district) {
        if (getBuiltDistricts().contains(district)) return -1; // We can't build the same district twice
        return (district.getPoint() - 3) * 0.35 // we subtract 3 to the number of points, to have a negative profitability if the district gives 1 or 2 points
                + districtPropertyGain(district, District::numberOfDistrictsToDraw, this::numberOfDistrictsToDraw) / (getBuiltDistricts().size() + 1)
                + districtPropertyGain(district, District::numberOfDistrictsToKeep, this::numberOfDistrictsToKeep) / (getBuiltDistricts().size() + 1)
                + (district.isDestructible() ? 0 : 3);
    }

    /**
     * This method allows the bot to know if another player could destroy a specific district
     *
     * @param district the district to test if it could be destroyed
     * @param player   the player who maybe could destroy the district
     * @return true if the player could destroy the given district and false else
     */
    protected boolean couldDestroy(District district, IPlayer player) {
        return district.isDestructible() && player.getCoins() + 2 + player.getBuiltDistricts().stream().filter(district1 -> district1.getColor() == Colors.RED).count() >= district.getCost() - 1;
    }

    /**
     * This method allows the bot to know if another player could destroy one of its built district
     *
     * @param players the List of other player
     * @return the list of player who could destroy one built district
     */
    protected List<IPlayer> possibleDestruction(List<IPlayer> players) {
        return players.stream().filter(iPlayer -> getBuiltDistricts().stream().anyMatch(district -> couldDestroy(district, iPlayer))).toList();
    }

    /**
     * This method allows the bot to know the number of player could want to kill it
     *
     * @param players the List of other player
     * @return the number of player could want to kill it
     */
    protected double possibleKill(List<IPlayer> players) {
        var mostDangerous = getMostDangerousPlayersByBuiltDistricts(players);
        int numberOfPoint = getBuiltDistricts().stream().mapToInt(District::getPoint).sum();
        return (double) players.size() - mostDangerous.stream().takeWhile(iPlayer -> iPlayer.getBuiltDistricts().stream().mapToInt(District::getPoint).sum() >= numberOfPoint).toList().size();
    }

    /**
     * This method allows the bot to know the number of player could want to exchange his cards with it
     *
     * @param players the List of other player
     * @return the number of player could want to exchange his cards with it
     */
    protected double possibleExchange(List<IPlayer> players) {
        return players.stream().filter(iPlayer -> iPlayer.getHandSize() < getHandSize()).toList().size();
    }

    /**
     * This method allows the bot to know the number of player could want to steal it
     *
     * @param players the List of other player
     * @return the number of player could want to steal it
     */
    protected double possibleSteal(List<IPlayer> players) {
        return players.stream().filter(iPlayer -> iPlayer.getCoins() < getCoins()).toList().size();
    }
}
