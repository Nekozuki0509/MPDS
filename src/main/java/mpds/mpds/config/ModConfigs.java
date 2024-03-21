package mpds.mpds.config;

import com.mojang.datafixers.util.Pair;

public class ModConfigs {
    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;


    public static String HOST;
    public static String DB_NAME;
    public static String TABLE_NAME;
    public static String USER;
    public static String PASSWD;
    public static String SERVER;

    public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of( "mdpsconfig").provider(configs).request();

        assignConfigs();
    }

    private static void createConfigs() {
        configs.addKeyValuePair(new Pair<>("HOST", "000.000.000.000"), "String", "it's mysql host ip");
        configs.addKeyValuePair(new Pair<>("DB_NAME", "test"), "String", "it's mysql database name **YOU MUST CREATE THIS DB!!**");
        configs.addKeyValuePair(new Pair<>("TABLE_NAME", "test"), "String", "it's mysql table name(auto create)");
        configs.addKeyValuePair(new Pair<>("USER", "test"), "String", "it's mysql user name");
        configs.addKeyValuePair(new Pair<>("PASSWD", "test"), "String", "it's mysql user's password");
        configs.addKeyValuePair(new Pair<>("SERVER", "s"), "String", "it's this server name");
    }

    private static void assignConfigs() {
        HOST = CONFIG.getOrDefault("HOST", "000.000.000.000");
        DB_NAME = CONFIG.getOrDefault("DB_NAME", "test");
        TABLE_NAME = CONFIG.getOrDefault("TABLE_NAME", "test");
        USER = CONFIG.getOrDefault("USER", "test");
        PASSWD = CONFIG.getOrDefault("PASSWD", "test");
        SERVER = CONFIG.getOrDefault("SERVER", "s");

        System.out.println("All " + configs.getConfigsList().size() + " have been set properly");
    }
}