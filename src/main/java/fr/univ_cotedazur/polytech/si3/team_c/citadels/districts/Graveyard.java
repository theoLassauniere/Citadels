package fr.univ_cotedazur.polytech.si3.team_c.citadels.districts;

import fr.univ_cotedazur.polytech.si3.team_c.citadels.Action;
import fr.univ_cotedazur.polytech.si3.team_c.citadels.Colors;
import fr.univ_cotedazur.polytech.si3.team_c.citadels.District;

import java.util.List;
import java.util.Optional;

public class Graveyard extends District {
    public Graveyard() {
        super("Graveyard", 5, Colors.PURPLE, 5);
    }

    @Override
    public Optional<List<Action>> getEvenementialAction() {
        return Optional.of(List.of(Action.GRAVEYARD));
    }
}
