package be.isach.ultracosmetics.cosmetics.pets;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.PetType;
import be.isach.ultracosmetics.player.UltraPlayer;

import org.bukkit.entity.WanderingTrader;

/**
 * Represents an instance of a wandering trader pet summoned by a player.
 *
 * @author Chris6ix
 * @since 14-09-2022
 */
public class PetWanderingTrader extends Pet {
    public PetWanderingTrader(UltraPlayer owner, PetType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @Override
    public void setupEntity() {
        ((WanderingTrader) entity).setDespawnDelay(0);
    }
}
