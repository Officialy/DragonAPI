package reika.dragonapi.interfaces;

// For recipes that have a defined XP yield per output item, such as blast furnace recipes.
public interface IHasXP {
    /** XP (per output item) as loaded from JSON. */
    float xpPerItem();
}
