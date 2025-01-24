package mpds.mpds;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mpds.mpds.events.Disconnect;
import mpds.mpds.events.Join;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MPDS implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("mpds");

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

    @Override
    public void onInitialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("MPDS");
        Path configjson = configDir.resolve("Config.json");

        if (Files.notExists(configDir)) {
            try {
                Files.createDirectory(configDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


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

        sql.TABLE_NAME = config.get("TABLE_NAME");

        ServerName = config.get("ServerName");

        SA = Boolean.parseBoolean(config.get("SA"));
        SH = Boolean.parseBoolean(config.get("SH"));
        SF = Boolean.parseBoolean(config.get("SF"));
        SL = Boolean.parseBoolean(config.get("SL"));
        SEn = Boolean.parseBoolean(config.get("SEn"));
        SI = Boolean.parseBoolean(config.get("SI"));
        SEf = Boolean.parseBoolean(config.get("SEf"));

        try {
            sql.init();
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
                                    sql.updateSkip(StringArgumentType.getString(ctx, "player"), String.valueOf(BoolArgumentType.getBool(ctx, "skip")));
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
                                    skiprs = sql.showSkip();

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

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            try {
                sql.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        LOGGER.info("MPDS loaded");
    }

    public static void playSound(ServerPlayerEntity player, SoundEvent event) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(event), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
    }
}