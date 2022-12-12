package reika.dragonapi.libraries.registry;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.opengl.GL11;
import reika.dragonapi.instantiable.data.KeyedItemStack;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.libraries.java.ReikaStringParser;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public enum ReikaDyeHelper {

    BLACK(0x191919),
    RED(0x993333),
    GREEN(0x667F33),
    BROWN(0x664C33),
    BLUE(0x334CB2),
    PURPLE(0x7F3FB2),
    CYAN(0x4C7F99),
    LIGHTGRAY(0x999999, "Light Gray"),
    GRAY(0x4C4C4C),
    PINK(0xF27FA5),
    LIME(0x7FCC19),
    YELLOW(0xE5E533),
    LIGHTBLUE(0x6699D8, "Light Blue"),
    MAGENTA(0xB24CD8),
    ORANGE(0xD87F33),
    WHITE(0xFFFFFF);

    public final int color;
    public final String colorName;
    public final String colorNameNoSpaces;

    private static final Random rand = new Random();

    public static final ReikaDyeHelper[] dyes = ReikaDyeHelper.values();
    private final ArrayList<ItemStack> items = new ArrayList<>();
    private static final MultiMap<KeyedItemStack, ReikaDyeHelper> colorMap = new MultiMap<KeyedItemStack, ReikaDyeHelper>(MultiMap.CollectionType.HASHSET).setNullEmpty();
    private static final MultiMap<ReikaDyeHelper, ReikaDyeHelper> similarityMap = new MultiMap<>(MultiMap.CollectionType.HASHSET);

    ReikaDyeHelper(int c) {
        color = c;
        colorName = ReikaStringParser.capFirstChar(this.name());
        colorNameNoSpaces = ReikaStringParser.stripSpaces(colorName);
    }

    ReikaDyeHelper(int c, String n) {
        color = c;
        colorName = n;
        colorNameNoSpaces = ReikaStringParser.stripSpaces(n);
    }

    public static ReikaDyeHelper getByName(String s) {
        return ReikaDyeHelper.valueOf(s.toUpperCase(Locale.ENGLISH).replaceAll(" ", ""));
    }

    public static boolean isDyeItem(ItemStack is) {
        return is != null && getColorsFromItem(is) != null;
    }

    public static ReikaDyeHelper getColorFromItem(ItemStack is) {
        if (is == null)
            return null;
        if (is.getItem() instanceof DyeItem) {
            return dyes[((DyeItem) is.getItem()).getDyeColor().ordinal()]; //todo check if this works
//            return getColorFromID(is.getItemDamage());
        }
        Collection<ReikaDyeHelper> c = getDyeByOreDictionary(is);
        return c != null ? c.iterator().next() : null;
    }

    public static Collection<ReikaDyeHelper> getColorsFromItem(ItemStack is) {
        if (is == null)
            return null;
        Collection<ReikaDyeHelper> c = getDyeByOreDictionary(is);
        return c != null ? Collections.unmodifiableCollection(c) : null;
    }

    private static Collection<ReikaDyeHelper> getDyeByOreDictionary(ItemStack is) {
        Collection<ReikaDyeHelper> c = colorMap.get(createKey(is));
        return c == null || c.isEmpty() ? null : c;
    }

    public static void buildItemCache() {
        colorMap.clear();
        for (int i = 0; i < dyes.length; i++) {
            dyes[i].items.clear();

//            for (ItemStack is : OreDictionary.getOres(dyes[i].getOreDictName())) {
//                addItemMapping(dyes[i], is);
//            }
        }
    }

    private static void addItemMapping(ReikaDyeHelper dye, ItemStack is) {
        dye.items.add(is);
        colorMap.addValue(createKey(is), dye);
    }

    private static KeyedItemStack createKey(ItemStack is) {
        return new KeyedItemStack(is).setIgnoreNBT(true).setSized(false).setSimpleHash(true);
    }

    public static ReikaDyeHelper getColorFromID(int id) {
        return id >= 0 && id < dyes.length ? dyes[id] : BLACK;
    }

    public static ReikaDyeHelper getRandomColor() {
        return getColorFromID(rand.nextInt(16));
    }

    public int getDamage() {
        return this.ordinal();
    }

    public int getColor() {
        return color;//todo ReikaTextureHelper.isDefaultResourcePack() ? color : this.getColorOverride();
    }

    public int getDefaultColor() {
        return color;
    }

  /*  private int getColorOverride() {
        return ReikaTextureHelper.getColorOverride(this);
    }*/

    public Color getJavaColor() {
        return Color.decode(String.valueOf(FMLLoader.getDist() == Dist.CLIENT ? this.getColor() : color));
    }

    public int getRed() {
        return this.getJavaColor().getRed();
    }

    public int getBlue() {
        return this.getJavaColor().getBlue();
    }

    public int getGreen() {
        return this.getJavaColor().getGreen();
    }

    public ItemStack get() {
        return switch (this) {
            case BLACK -> new ItemStack(Items.BLACK_DYE);
            case RED -> new ItemStack(Items.RED_DYE);
            case GREEN -> new ItemStack(Items.GREEN_DYE);
            case BROWN -> new ItemStack(Items.BROWN_DYE);
            case BLUE -> new ItemStack(Items.BLUE_DYE);
            case PURPLE -> new ItemStack(Items.PURPLE_DYE);
            case CYAN -> new ItemStack(Items.CYAN_DYE);
            case LIGHTGRAY -> new ItemStack(Items.LIGHT_GRAY_DYE);
            case GRAY -> new ItemStack(Items.GRAY_DYE);
            case PINK -> new ItemStack(Items.PINK_DYE);
            case LIME -> new ItemStack(Items.LIME_DYE);
            case YELLOW -> new ItemStack(Items.YELLOW_DYE);
            case LIGHTBLUE -> new ItemStack(Items.LIGHT_BLUE_DYE);
            case MAGENTA -> new ItemStack(Items.MAGENTA_DYE);
            case ORANGE -> new ItemStack(Items.ORANGE_DYE);
            case WHITE -> new ItemStack(Items.WHITE_DYE);
        };
    }

    public ItemStack getWoolStack() {
        return switch (this) {
            case BLACK -> new ItemStack(Blocks.BLACK_WOOL);
            case RED -> new ItemStack(Blocks.RED_WOOL);
            case GREEN -> new ItemStack(Blocks.GREEN_WOOL);
            case BROWN -> new ItemStack(Blocks.BROWN_WOOL);
            case BLUE -> new ItemStack(Blocks.BLUE_WOOL);
            case PURPLE -> new ItemStack(Blocks.PURPLE_WOOL);
            case CYAN -> new ItemStack(Blocks.CYAN_WOOL);
            case LIGHTGRAY -> new ItemStack(Blocks.LIGHT_GRAY_WOOL);
            case GRAY -> new ItemStack(Blocks.GRAY_WOOL);
            case PINK -> new ItemStack(Blocks.PINK_WOOL);
            case LIME -> new ItemStack(Blocks.LIME_WOOL);
            case YELLOW -> new ItemStack(Blocks.YELLOW_WOOL);
            case LIGHTBLUE -> new ItemStack(Blocks.LIGHT_BLUE_WOOL);
            case MAGENTA -> new ItemStack(Blocks.MAGENTA_WOOL);
            case ORANGE -> new ItemStack(Blocks.ORANGE_WOOL);
            case WHITE -> new ItemStack(Blocks.WHITE_WOOL);
        };
    }

    public double[] getRedstoneParticleVelocityForColor() {
        if (this == WHITE)
            return new double[]{20, 20, 20};
        double[] c = new double[]{this.getRed() / 255D, this.getGreen() / 255D, this.getBlue() / 255D};
        return c;
    }

    public String getOreDictName() {
        return "dye" + ReikaStringParser.stripSpaces(colorName);
    }

    public void setGLColorBlend() {
//        Color c = this.getJavaColor();
        RenderSystem.setShaderColor(this.getRed() / 255f, this.getGreen() / 255f, this.getBlue() / 255f, 255);
//        GL11.glColor3d(this.getRed() / 255D, this.getGreen() / 255D, this.getBlue() / 255D);
    }

    public Set<ReikaDyeHelper> getSimilarColors() {
        return Collections.unmodifiableSet((Set<ReikaDyeHelper>) similarityMap.get(this));
    }

    static {
        similarityMap.addValue(BLACK, GRAY);
        similarityMap.addValue(BLACK, LIGHTGRAY);
        similarityMap.addValue(BLACK, WHITE);

        similarityMap.addValue(RED, PURPLE);
        similarityMap.addValue(RED, PINK);
        similarityMap.addValue(RED, MAGENTA);
        similarityMap.addValue(RED, ORANGE);

        similarityMap.addValue(GREEN, LIME);
        similarityMap.addValue(GREEN, CYAN);

        similarityMap.addValue(BROWN, ORANGE);

        similarityMap.addValue(BLUE, PURPLE);
        similarityMap.addValue(BLUE, CYAN);
        similarityMap.addValue(BLUE, LIGHTBLUE);

        similarityMap.addValue(PURPLE, RED);
        similarityMap.addValue(PURPLE, PINK);
        similarityMap.addValue(PURPLE, MAGENTA);

        similarityMap.addValue(CYAN, BLUE);
        similarityMap.addValue(CYAN, GREEN);

        similarityMap.addValue(LIGHTGRAY, WHITE);
        similarityMap.addValue(LIGHTGRAY, GRAY);
        similarityMap.addValue(LIGHTGRAY, BLACK);

        similarityMap.addValue(GRAY, WHITE);
        similarityMap.addValue(GRAY, LIGHTGRAY);
        similarityMap.addValue(GRAY, BLACK);

        similarityMap.addValue(PINK, RED);
        similarityMap.addValue(PINK, MAGENTA);

        similarityMap.addValue(LIME, YELLOW);
        similarityMap.addValue(LIME, GREEN);

        similarityMap.addValue(YELLOW, LIME);
        similarityMap.addValue(YELLOW, ORANGE);

        similarityMap.addValue(LIGHTBLUE, BLUE);
        similarityMap.addValue(LIGHTBLUE, WHITE);

        similarityMap.addValue(MAGENTA, PURPLE);
        similarityMap.addValue(MAGENTA, PINK);
        similarityMap.addValue(MAGENTA, RED);

        similarityMap.addValue(ORANGE, RED);
        similarityMap.addValue(ORANGE, YELLOW);

        similarityMap.addValue(WHITE, LIGHTGRAY);
        similarityMap.addValue(WHITE, GRAY);
        similarityMap.addValue(WHITE, BLACK);
    }
}
