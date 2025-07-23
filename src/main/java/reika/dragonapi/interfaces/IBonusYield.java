package reika.dragonapi.interfaces;

public interface IBonusYield {
    int  bonusChance();   // 0-100  (%)
    int  bonusMin();      // inclusive
    int  bonusMax();      // inclusive
}