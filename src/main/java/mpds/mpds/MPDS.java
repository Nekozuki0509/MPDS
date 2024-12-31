package mpds.mpds;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.JsonOps;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mpds.mpds.events.Disconnect;
import mpds.mpds.events.Join;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MPDS implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("mpds");

    static Connection connection = null;

    static String TABLE_NAME;

    public static String ServerName;

    public static final List<UUID> broken = new ArrayList<>();

    static HashMap<String, String> config;

    public static boolean AJM;

    public static boolean ASM;

    public static boolean AEM;

    public static boolean SA;

    public static boolean SH;

    public static boolean SF;

    public static boolean SL;

    public static boolean SEn;

    public static boolean SI;

    public static boolean SEf;

    public static Gson gson = new Gson();

    public static RegistryOps<JsonElement> wrappedOps;

    public static PreparedStatement onjoinstatement;

    public static PreparedStatement checkskip;

    public static PreparedStatement showskip;

    public static PreparedStatement updateskip;

    public static PreparedStatement bea;

    public static PreparedStatement befalse;

    public static PreparedStatement setserver;

    public static PreparedStatement ondisconnectstatement;

    @Override
    public void onInitialize() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("MPDS");
        Path configjson = configDir.resolve("Config.json");


        if (Files.notExists(configjson)) {
            try {
                Files.copy(Objects.requireNonNull(MPDS.class.getResourceAsStream("/mpdsconfig.json")), configjson);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("made MPDS config file.\nPlease set!");
            System.exit(0);
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(String.valueOf(configjson)), StandardCharsets.UTF_8))) {
            config = gson.fromJson(reader, new TypeToken<HashMap<String, String>>() {
            }.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AJM = Boolean.parseBoolean(config.get("AJM"));
        ASM = Boolean.parseBoolean(config.get("ASM"));
        AEM = Boolean.parseBoolean(config.get("AEM"));

        TABLE_NAME = config.get("TABLE_NAME");

        ServerName = config.get("ServerName");

        SA = Boolean.parseBoolean(config.get("SA"));
        SH = Boolean.parseBoolean(config.get("SH"));
        SF = Boolean.parseBoolean(config.get("SF"));
        SL = Boolean.parseBoolean(config.get("SL"));
        SEn = Boolean.parseBoolean(config.get("SEn"));
        SI = Boolean.parseBoolean(config.get("SI"));
        SEf = Boolean.parseBoolean(config.get("SEf"));

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + config.get("HOST") + "/" + config.get("DB_NAME") + "?autoReconnect=true", config.get("USER"), config.get("PASSWD"));

            onjoinstatement = connection.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE uuid = ?");

            checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

            showskip = connection.prepareStatement("SELECT * FROM skipplayer");

            updateskip = connection.prepareStatement("INSERT INTO skipplayer (Name, skip) VALUES (?, ?) AS new ON DUPLICATE KEY UPDATE Name=new.Name, skip=new.skip");

            bea = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET server=\"*\" WHERE uuid = ?");

            befalse = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET sync=\"false\" WHERE uuid = ?");

            setserver = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET server=? WHERE uuid = ?");

            ondisconnectstatement = connection.prepareStatement("INSERT INTO " + TABLE_NAME +
                    " (Name, uuid, Air, Health, enderChestInventory, exhaustion, foodLevel, saturationLevel, foodTickTimer, main, off, armor, selectedSlot, experienceLevel, experienceProgress, effects, sync) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \"true\") AS new " +
                    "ON DUPLICATE KEY UPDATE " +
                    "Air=new.Air," +
                    "Health=new.Health," +
                    "enderChestInventory=new.enderChestInventory," +
                    "exhaustion=new.exhaustion," +
                    "foodLevel=new.foodLevel," +
                    "saturationLevel=new.saturationLevel," +
                    "foodTickTimer=new.foodTickTimer," +
                    "main=new.main," +
                    "off=new.off," +
                    "armor=new.armor," +
                    "selectedSlot=new.selectedSlot," +
                    "experienceLevel=new.experienceLevel," +
                    "experienceProgress=new.experienceProgress," +
                    "effects=new.effects," +
                    "sync=new.sync");

            connection.prepareStatement
                    ("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                            "id int AUTO_INCREMENT PRIMARY KEY," +
                            "Name char(16)," +
                            "uuid char(36) UNIQUE," +
                            "Air int," +
                            "Health float," +
                            "enderChestInventory longtext," +
                            "exhaustion float," +
                            "foodLevel int," +
                            "saturationLevel float," +
                            "foodTickTimer int," +
                            "main longtext," +
                            "off longtext," +
                            "armor longtext," +
                            "selectedSlot int," +
                            "experienceLevel int," +
                            "experienceProgress int," +
                            "effects longtext," +
                            "sync char(5)," +
                            "server text" +
                            ")").executeUpdate();

            connection.prepareStatement
                    ("CREATE TABLE IF NOT EXISTS skipplayer(" +
                            "id int auto_increment PRIMARY KEY," +
                            "Name char(16) UNIQUE," +
                            "skip char(5)" +
                            ")").executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("FAIL TO CONNECT MYSQL");
            LOGGER.error("DID YOU CHANGE MPDS CONFIG?");
            e.printStackTrace();
        }

        ServerPlayConnectionEvents.JOIN.register(Join::onjoin);
        ServerPlayConnectionEvents.DISCONNECT.register(Disconnect::ondisconnect);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("updateskip").then(argument("player", StringArgumentType.word()).then(argument("skip", BoolArgumentType.bool())
                        .executes(ctx -> {
                            while (true) {
                                try {
                                    updateskip.setString(1, StringArgumentType.getString(ctx, "player"));
                                    updateskip.setString(2, String.valueOf(BoolArgumentType.getBool(ctx, "skip")));
                                    updateskip.executeUpdate();

                                    ctx.getSource().getServer().getPlayerManager().broadcast(Text.translatable("set " + StringArgumentType.getString(ctx, "player") + "'s data " + BoolArgumentType.getBool(ctx, "skip")).formatted(Formatting.YELLOW), false);

                                    return 1;
                                } catch (CommunicationsException ignored) {
                                } catch (Exception e) {
                                    ctx.getSource().getServer().getPlayerManager().broadcast(Text.translatable("THERE WERE SOME ERRORS : \n" + e.getMessage()).formatted(Formatting.RED), false);
                                    LOGGER.error("THERE WERE SOME ERRORS :");
                                    e.printStackTrace();

                                    return 1;
                                }
                            }
                        })
                ))));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("showskip")
                        .executes(ctx -> {
                            ResultSet skiprs;
                            StringBuilder skipp = new StringBuilder();

                            while (true) {
                                try {
                                    skiprs = showskip.executeQuery();

                                    while (skiprs.next()) {
                                        if ("false".equals(skiprs.getString("skip"))) continue;
                                        skipp.append("・").append(skiprs.getString("Name")).append("\n");
                                    }

                                    ServerPlayerEntity player;
                                    if ((player = ctx.getSource().getPlayer()) != null) {
                                        player.sendMessage(Text.of(skipp.toString()));
                                    } else {
                                        ctx.getSource().getServer().sendMessage(Text.of(skipp.toString()));
                                    }

                                    return 1;
                                } catch (CommunicationsException ignored) {
                                } catch (Exception e) {
                                    ctx.getSource().getServer().getPlayerManager().broadcast(Text.translatable("THERE WERE SOME ERRORS : \n" + e.getMessage()).formatted(Formatting.RED), false);
                                    LOGGER.error("THERE WERE SOME ERRORS :");
                                    e.printStackTrace();

                                    return 1;
                                }
                            }
                        })
                ));

        ServerTickEvents.START_SERVER_TICK.register(server -> wrappedOps = server.getRegistryManager().getOps(JsonOps.INSTANCE));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        LOGGER.info("MPDS loaded");
    }
}