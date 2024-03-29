package fr.univ_cotedazur.polytech.si3.team_c.citadels;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("java:S2160")
public abstract class District extends Card {
    private final int cost;
    private final int point;

    protected District(String name, int cost, Colors color, int point) {
        super(name, color);
        this.cost = cost;
        this.point = point;
    }

    /**
     * @return the cost of the district
     **/
    public int getCost() {
        return cost;
    }

    /**
     * @return the points given by the district
     */
    public int getPoint() {
        return point;
    }

    /**
     * @return true if the district is destructible else false
     */
    public boolean isDestructible() {
        return true;
    }

    /**
     * @return The number of cards to draw after the district is built
     */
    public Optional<Integer> numberOfDistrictsToDraw() {
        return Optional.empty();
    }

    /**
     * @return The number of cards to keep after the district is built
     */
    public Optional<Integer> numberOfDistrictsToKeep() {
        return Optional.empty();
    }

    /**
     * @param builtInLastTurn a boolean that is true if the card was built last turn
     * @return The colors that will be taken into account when counting bonuses
     */
    public List<Colors> bonusColors(boolean builtInLastTurn) {
        return List.of(getColor());
    }

    /**
     * @param color the color to be compared
     * @return true if the card color match to the color
     */
    public boolean matchColor(Colors color) {
        return getColor().equals(color);
    }

    @Override
    public String toString() {
        String text = super.toString() + " ($" + getCost();
        if (getPoint() != getCost()) text += ", " + getPoint() + " pts";
        return text + ")";
    }
}
