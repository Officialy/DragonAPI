/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi;

import net.minecraft.world.level.ChunkPos;
import reika.dragonapi.auxiliary.trackers.KeyWatcher;
import reika.dragonapi.interfaces.configuration.*;

import static com.mojang.blaze3d.platform.InputConstants.KEY_TAB;

public enum DragonOptions implements IntegerConfig, BooleanConfig, StringArrayConfig, StringConfig, UserSpecificConfig {

    LOGLOADING("Console Loading Info", true),
    FILELOG("Log Loading Info To Separate File", false),
    DEBUGMODE("Debug Mode", false),
    SYNCPACKET("Sync Packet ID", 182),
    NORENDERS("Disable Renders For Debug", false),
    TABNBT("Show TileEntity NBT when using Debug Key", false),
    SOUNDCHANNELS("Increase sound channel count", true),
    UNNERFOBSIDIAN("Restore Obsidian Blast Resistance", true),
    NOHOTBARSWAP("Disable Hotbar Swapping", false),
    CHATERRORS("Log errors to chat", true),
    SORTCREATIVE("Sort Creative Tabs Alphabetically", true),
    CUSTOMRENDER("Custom/Donator Renders", true),
    OPONLYUPDATE("Only show update notice to Ops or SSP", false),
    PACKONLYUPDATE("Only show update notice to pack creator", false),
    GREGORES("Force Gregtech Ore Compatibility", true),
    LOGSYNCCME("Log Sync Packet CME Avoidance", true),
    SLOWSYNC("Slow Sync Packets - Only use this as a last resort", false),
    LAGWARNING("Minimum Delay (ms) for 'Can't Keep Up!' Log Warning", 0),
    CHECKSANITY("Check Environment Sanity", false),
    FIXSANITY("Attempt to Repair Environment Sanity", false),
    ADMINPERMBYPASS("Admins Bypass Permissions", true),
    SOUNDHASHMAP("Use HashMap for Sound Categories - Only use if necessary", false),
    FILEHASH("Compare mod file hashes between client and server", true),
    APRIL("Enable Temporally Dependent Amusement Behavior", true),
    //NOALPHATEST("Disable Alpha Clipping in WorldRenderer", true),
    PARTICLELIMIT("Particle Limit (Vanilla = 4000)", 4000),
    DEBUGKEY("Debug Overlay Key (LWJGL ID)", KEY_TAB), //Keyboard.KEY_TAB
    //RECURSE("Recursion Limit Override", -1),
    //COMPOUNDSYNC("Compound Sync Packet System - Use at own risk", false);
    DIRECTOC("Direct OpenComputers Support", false),
    AUTOREBOOT("Automatic Reboot Interval (Seconds)", -1),
    XPMERGE("Merge XP Orbs Like Items", true),
    RAINTICK("Extra Block Ticks When Raining", true),
    PROTECTNEW("Prevent Mobs From Targeting Players Immediately After Logging In", true),
    SKINCACHE("Cache Skins", true),
    BIOMEFIRE("Biome Humidity Dependent Fire Spread", true),
    ADMINPROFILERS("Restrict profiling abilities to admins", true),
    BYTECODELIST("Bytecodeexec command user UUID whitelist", new String[0]),
    CTRLCOLLECT("Automatic Collection of Inventories; set to 'NULL' to disable", KeyWatcher.Key.LCTRL.name()), //TypeHelper to Website Generator: String
    AFK("AFK Timer Threshold (Seconds); Set to 0 to Disable", 120), //2 min
    REROUTEEYES("Reroute Ender Eyes to Stronghold Entrances", false),
    WORLDSIZE("Expected Approximate Maximum World Size (Radius)", 5000),
    WORLDCENTERX("Expected Approximate World Center Location X", 0),
    WORLDCENTERZ("Expected Approximate World Center Location Z", 0),
    NORAINFX("Disable rain sound and particles", false),
    NOTIFYBYTEEXEC("Bytecodeexec command notifies other admins", false),
    PLAYERMOBCAP("Player-Specific Mob Caps", false),
    SETTINGWARN("Setting Warning Persistence (EVERYLOAD/SETTINGCHANGE/VERSION/ONCE)", "SETTINGCHANGE"),
    STOPUNLOADSPREAD("Prevent block spreading near unloaded chunks", true),
    VERSIONCHANGEWARN("Version Change Warning Level (0 = None, 1 = ReikaMods only, 2 = All)", 2),
    DEBUGLOG("Mod's Debug Log (false = None, true = All)", false),
    ;

    private final String label;
    private boolean defaultState;
    private int defaultValue;
    private String defaultString;
    private String[] defaultStringArray;
    private final Class type;
    private boolean enforcing = false;

    public static final DragonOptions[] optionList = values();

    DragonOptions(String l, boolean d) {
        label = l;
        defaultState = d;
        type = boolean.class;
    }

    DragonOptions(String l, boolean d, boolean tag) {
        this(l, d);
        enforcing = true;
    }

    DragonOptions(String l, int d) {
        label = l;
        defaultValue = d;
        type = int.class;
    }

    DragonOptions(String l, String s) {
        label = l;
        defaultString = s;
        type = String.class;
    }

    DragonOptions(String l, String[] s) {
        label = l;
        defaultStringArray = s;
        type = String[].class;
    }

    public boolean isBoolean() {
        return type == boolean.class;
    }

    public boolean isNumeric() {
        return type == int.class;
    }

    public boolean isString() {
        return type == String.class;
    }

    public Class getPropertyType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public boolean getState() {
        return (Boolean)DragonAPI.config.getControl(this.ordinal());
    }

    public int getValue() {
        return (Integer)DragonAPI.config.getControl(this.ordinal());
    }

    public String getString() {
        return (String)DragonAPI.config.getControl(this.ordinal());
    }

    public boolean isDummiedOut() {
        return type == null;
    }

    @Override
    public boolean getDefaultState() {
        return defaultState;
    }

    @Override
    public int getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isEnforcingDefaults() {
        return enforcing;
    }

    @Override
    public boolean shouldLoad() {
        return true;
    }

    @Override
    public boolean isUserSpecific() {
        switch(this) {
            case LOGLOADING:
            case FILELOG:
            case DEBUGMODE:
            case NORENDERS:
            case SOUNDCHANNELS:
            case NOHOTBARSWAP:
            case CHATERRORS:
            case SORTCREATIVE:
            case CUSTOMRENDER:
            case APRIL:
                //case NOALPHATEST:
            case TABNBT:
            case PARTICLELIMIT:
            case DEBUGKEY:
            case NORAINFX:
            case SETTINGWARN:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isStringArray() {
        return type == String[].class;
    }

    @Override
    public String[] getStringArray() {
        return (String[])DragonAPI.config.getControl(this.ordinal());
    }

    @Override
    public String[] getDefaultStringArray() {
        return defaultStringArray;
    }

    @Override
    public String getDefaultString() {
        return defaultString;
    }

    public static KeyWatcher.Key getCollectKey() {
        if (CTRLCOLLECT.getString().equalsIgnoreCase("null"))
            return null;
        return KeyWatcher.Key.readFromConfig(DragonAPI.instance, CTRLCOLLECT);
    }

    /** In block coords */
    public static ChunkPos getWorldCenter() {
        return new ChunkPos(WORLDCENTERX.getValue(), WORLDCENTERZ.getValue());
    }



}
/*
    public static final Common COMMON;
    public static final Client CLIENT;
    private static final ForgeConfigSpec commonSpec;
    private static final ForgeConfigSpec clientSpec;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static void register(final ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, commonSpec);
        context.registerConfig(ModConfig.Type.CLIENT, clientSpec);
    }

    public static class Client {

        public final BooleanValue LOGLOADING;
        public final BooleanValue FILELOG;
        public final BooleanValue DEBUGMODE;
        public final BooleanValue NORENDERS;
        public final BooleanValue SOUNDCHANNELS;
        public final BooleanValue NOHOTBARSWAP;
        public final BooleanValue CHATERRORS;
        public final BooleanValue CUSTOMRENDER;
        public final EnumValue<SettingWarn> SETTINGWARN;
        public final BooleanValue APRIL;
        public final BooleanValue DEBUGLOG;
        public final BooleanValue LOGSYNCCME;
        public final BooleanValue TABNBT;
        public final ForgeConfigSpec.IntValue DEBUGKEY; //Keyboard.KEY_TAB
        public final ForgeConfigSpec.IntValue PARTICLELIMIT; //Keyboard.KEY_TAB

        Client(final ForgeConfigSpec.Builder builder) {
            builder.comment("Client Specific Settings").push("client");

            LOGLOADING = builder
                    .comment("Console Loading Info")
                    .translation("reika.dragonapi.")
                    .define("loadinginfo", true);
            PARTICLELIMIT = builder
                    .comment("Maximum number of particles to render")
                    .translation("reika.dragonapi.particlelimit")
                    .defineInRange("debug", 4000, 1, Integer.MAX_VALUE);
            FILELOG = builder
                    .comment("Log Loading Info To Separate File")
                    .translation("reika.dragonapi.")
                    .define("filelog", false);
            DEBUGMODE = builder
                    .comment("Debug Mode")
                    .translation("reika.dragonapi.")
                    .define("debugmode", false);
            NORENDERS = builder
                    .comment("Disable Renders For Debug")
                    .translation("reika.dragonapi.")
                    .define("norenders", true);
            SOUNDCHANNELS = builder
                    .comment("Increase sound channel count")
                    .translation("reika.dragonapi.")
                    .define("soundchannels", false);
            NOHOTBARSWAP = builder
                    .comment("Disable Hotbar Swapping")
                    .translation("reika.dragonapi.")
                    .define("nohotbarswap", true);
            CHATERRORS = builder
                    .comment("Log errors to chat")
                    .translation("reika.dragonapi.")
                    .define("chaterrors", true);
            CUSTOMRENDER = builder
                    .comment("Custom/Donator Renders")
                    .translation("reika.dragonapi.")
                    .define("customrender", true);
            SETTINGWARN = builder
                    .comment("Setting Warning Persistence (EVERYLOAD/SETTINGCHANGE/VERSION/ONCE)")
                    .translation("reika.dragonapi.settingwarn")
                    .defineEnum("settingwarn", SettingWarn.SETTINGCHANGE);
            TABNBT = builder
                    .comment("Show BlockEntity NBT when using Debug Key")
                    .translation("reika.dragonapi.")
                    .define("tabnbt", true);
            APRIL = builder
                    .comment("Enable Temporally Dependent Amusement Behavior - April 1st, Dec 25th, Oct 31st")
                    .translation("reika.dragonapi.")
                    .define("april", true);
            DEBUGKEY = builder
                    .comment("Debug Overlay Key (GLFW ID)")
                    .translation("reika.dragonapi.")
                    .defineInRange("debug", 258, 0, 400);
            DEBUGLOG = builder
                    .comment("Extra Logging for debugging")
                    .translation("reika.dragonapi.")
                    .define("debuglog", true);
            LOGSYNCCME = builder
                    .comment("Log Sync Packet CME Avoidance")
                    .translation("reika.dragonapi.")
                    .define("logsynccme", true);

            builder.pop();
        }

        public enum SettingWarn {
            SETTINGCHANGE,
            VERSION,
            ONCE,
            EVERYLOAD
        }
    }

    public static class Common {

        public final BooleanValue OPONLYUPDATE;
        public final BooleanValue PACKONLYUPDATE;
        public final BooleanValue GREGORES;
        public final BooleanValue CHECKSANITY;
        public final BooleanValue ADMINPERMBYPASS;
        public final BooleanValue FILEHASH;
        public final BooleanValue DIRECTOC;
        public final BooleanValue PROTECTNEW;
        public final BooleanValue ADMINPROFILERS;
        public final ForgeConfigSpec.IntValue AFK; //2 min
        public final BooleanValue PLAYERMOBCAP;
        public final BooleanValue SLOWSYNC;
        public final BooleanValue STOPUNLOADSPREAD;

        public final ForgeConfigSpec.IntValue VERSIONCHANGEWARN;

        Common(final ForgeConfigSpec.Builder builder) {
            builder.comment("Common settings").push("common");

            OPONLYUPDATE = builder
                    .comment("Only show update notice to Ops or SSP")
                    .translation("reika.dragonapi.oponlyupdate")
                    .define("oponlyupdate", false);
            PACKONLYUPDATE = builder
                    .comment("Only show update notice to pack creator")
                    .translation("reika.dragonapi.oponlyupdate")
                    .define("packonlyupdate", false);
            GREGORES = builder
                    .comment("Force Gregtech Ore Compatibility\n")
                    .translation("reika.dragonapi.gregores")
                    .define("gregores", false);
            CHECKSANITY = builder
                    .comment("Check Environment Sanity")
                    .translation("reika.dragonapi.checksanity")
                    .define("checksanity", false);
            ADMINPERMBYPASS = builder
                    .comment("Admins Bypass Permissions")
                    .translation("reika.dragonapi.adminpermbypass")
                    .define("adminpermbypass", false);
            FILEHASH = builder
                    .comment("Compare mod file hashes between client and server")
                    .translation("reika.dragonapi.filehash")
                    .define("filehash", false);
            DIRECTOC = builder
                    .comment("Direct OpenComputers Support")
                    .translation("reika.dragonapi.directoc")
                    .define("directoc", false);
            PROTECTNEW = builder
                    .comment("Prevent Mobs From Targeting Players Immediately After Logging In")
                    .translation("reika.dragonapi.protectnew")
                    .define("protectnew", false);
            ADMINPROFILERS = builder
                    .comment("Restrict profiling abilities to admins")
                    .translation("reika.dragonapi.adminprofilers")
                    .define("adminprofilers", false);
            AFK = builder
                    .comment("AFK Timer Threshold (Seconds); Set to 0 to Disable")
                    .translation("reika.dragonapi.afk")
                    .defineInRange("afk", 120, 0, Integer.MAX_VALUE);
            PLAYERMOBCAP = builder
                    .comment("Player-Specific Mob Caps")
                    .translation("reika.dragonapi.playermobcap")
                    .define("playermobcap", false);
            VERSIONCHANGEWARN = builder
                    .comment("Version Change Warning Level (0 = None, 1 = ReikaMods only, 2 = All)")
                    .translation("reika.dragonapi.versionchangewarn")
                    .defineInRange("versionchangewarn", 2, 0, 2);
            SLOWSYNC = builder
                    .comment("Slow Sync Packets - Only use this as a last resort)")
                    .translation("reika.dragonapi.slowsync")
                    .define("slowsync", false);
            STOPUNLOADSPREAD = builder
                    .comment("Prevent block spreading near unloaded chunks")
                    .translation("reika.dragonapi.stopunloadspread")
                    .define("stopunloadspread", true);

            builder.pop();
        }
    }

}
*/