package mpds.mpds;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.JsonOps;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mpds.mpds.mixin.HungerManagerAccessor;
import mpds.mpds.mixin.PlayerManagerInvoker;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;
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

    public static Connection connection = null;

    static final List<UUID> broken = new ArrayList<>();

    static Path configjson;

    static HashMap<String, String> config;

    public static Gson gson = new Gson();

    PreparedStatement onjoinstatement;

    PreparedStatement checkskip;

    PreparedStatement showskip;

    PreparedStatement updateskip;

    PreparedStatement bea;

    PreparedStatement befalse;

    PreparedStatement setserver;

    PreparedStatement ondisconnectstatement;

    @Override
    public void onInitialize() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        configjson = FabricLoader.getInstance().getConfigDir().resolve("mpdsconfig.json");
        if (Files.notExists(configjson)) {
            try {
                Files.copy(Objects.requireNonNull(MPDS.class.getResourceAsStream("/mpdsconfig.json")), configjson);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(String.valueOf(configjson)), StandardCharsets.UTF_8))) {
            config = gson.fromJson(reader, new TypeToken<HashMap<String, String>>() {
            }.getType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + config.get("HOST") + "/" + config.get("DB_NAME") + "?autoReconnect=true", config.get("USER"), config.get("PASSWD"));

            onjoinstatement = connection.prepareStatement("SELECT * FROM " + config.get("TABLE_NAME") + " WHERE uuid = ?");

            checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

            showskip = connection.prepareStatement("SELECT * FROM skipplayer");

            updateskip = connection.prepareStatement("INSERT INTO skipplayer (Name, skip) VALUES (?, ?) AS new ON DUPLICATE KEY UPDATE Name=new.Name, skip=new.skip");

            bea = connection.prepareStatement("UPDATE " + config.get("TABLE_NAME") + " SET server=\"*\" WHERE uuid = ?");

            befalse = connection.prepareStatement("UPDATE " + config.get("TABLE_NAME") + " SET sync=\"false\" WHERE uuid = ?");

            setserver = connection.prepareStatement("UPDATE " + config.get("TABLE_NAME") + " SET server=? WHERE uuid = ?");

            ondisconnectstatement = connection.prepareStatement("INSERT INTO " + config.get("TABLE_NAME") +
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
                    ("CREATE TABLE IF NOT EXISTS " + config.get("TABLE_NAME") + "(" +
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

        ServerPlayConnectionEvents.JOIN.register(this::onjoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::ondisconnect);

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

        LOGGER.info("MPDS loaded");
    }

    private void onjoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        new Thread(() -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
            broken.add(player.getUuid());
            if (!Boolean.parseBoolean(config.get("DJM"))) player.sendMessage(Text.translatable("loading " + player.getName().getString() + "'s data...").formatted(Formatting.YELLOW));
            LOGGER.info("loading {}'s data...", player.getName().getString());
            while (true) {
                try {
                    checkskip.setString(1, player.getName().getString());
                    ResultSet checkskiprs = checkskip.executeQuery();
                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (!Boolean.parseBoolean(config.get("DSM"))) minecraftServer.getPlayerManager().broadcast(Text.translatable("skip loading because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW), false);
                        if (!Boolean.parseBoolean(config.get("DSM"))) player.sendMessage(Text.translatable("skip loading because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW));
                        LOGGER.warn("skip loading because {}'s data includes skip list", player.getName().getString());
                        broken.remove(player.getUuid());
                        player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_GLASS_BREAK), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 1f);
                        return;
                    }
                    onjoinstatement.setString(1, player.getUuid().toString());
                    ResultSet resultSet;
                    if ((resultSet = onjoinstatement.executeQuery()).next()) {
                        for (int i = 0; "false".equals(resultSet.getString("sync")); i++) {
                            if (i == 10) {
                                if (config.get("SERVER").equals(resultSet.getString("server")) || "*".equals(resultSet.getString("server"))) {
                                    if (!Boolean.parseBoolean(config.get("DJM"))) player.sendMessage(Text.translatable("saved " + player.getName().getString() + "'s correct data").formatted(Formatting.AQUA));
                                    LOGGER.info("saved {}'s correct data", player.getName().getString());
                                    broken.remove(player.getUuid());
                                    player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_PLAYER_LEVELUP), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                                    return;
                                }
                                if (!Boolean.parseBoolean(config.get("DEM"))) player.sendMessage(Text.translatable("IT LOOKS " + player.getName().getString() + "'s DATA WAS BROKEN!\nPLEASE CONNECT TO " + resultSet.getString("server") + "!").formatted(Formatting.RED));
                                LOGGER.error("IT LOOKS {}'s DATA WAS BROKEN!\nPLEASE CONNECT TO {}!", player.getName().getString(), resultSet.getString("server"));
                                player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_ANVIL_DESTROY), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                                return;
                            }
                            Thread.sleep(1000);
                            resultSet = onjoinstatement.executeQuery();
                            resultSet.next();
                        }
                        befalse.setString(1, player.getUuid().toString());
                        befalse.executeUpdate();
                        player.setAir(resultSet.getInt("Air"));
                        player.setHealth(resultSet.getFloat("Health"));
                        player.getHungerManager().setExhaustion(resultSet.getFloat("exhaustion"));
                        player.getHungerManager().setFoodLevel(resultSet.getInt("foodLevel"));
                        player.getHungerManager().setSaturationLevel(resultSet.getFloat("saturationLevel"));
                        ((HungerManagerAccessor) player.getHungerManager()).setFoodTickTimer(resultSet.getInt("foodTickTimer"));
                        if (!"".equals(resultSet.getString("off")))
                            player.getInventory().offHand.set(0, ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(resultSet.getString("off"))).resultOrPartial(LOGGER::error).orElseThrow());
                        player.getInventory().selectedSlot = resultSet.getInt("selectedSlot");
                        player.experienceLevel = resultSet.getInt("experienceLevel");
                        player.experienceProgress = resultSet.getInt("experienceProgress");
                        if (!"".equals(resultSet.getString("enderChestInventory")))
                            List.of(resultSet.getString("enderChestInventory").split("&")).forEach(compound -> {
                                String[] compounds = compound.split("~");
                                player.getEnderChestInventory().setStack(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                            });
                        if (!"".equals(resultSet.getString("main")))
                            List.of(resultSet.getString("main").split("&")).forEach(compound -> {
                                String[] compounds = compound.split("~");
                                player.getInventory().main.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                            });
                        if (!"".equals(resultSet.getString("armor")))
                            List.of(resultSet.getString("armor").split("&")).forEach(compound -> {
                                String[] compounds = compound.split("~");
                                player.getInventory().armor.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                            });
                        if (!"".equals(resultSet.getString("effects")))
                            List.of(resultSet.getString("effects").split("&")).forEach(compound -> player.addStatusEffect(StatusEffectInstance.fromNbt(NbtCompound.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compound)).resultOrPartial(LOGGER::error).orElseThrow())));
                        if (!Boolean.parseBoolean(config.get("DJM"))) player.sendMessage(Text.translatable("success to load " + player.getName().getString() + "'s data!").formatted(Formatting.AQUA));
                        LOGGER.info("success to load {}'s data!", player.getName().getString());
                        player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_PLAYER_LEVELUP), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                        setserver.setString(1, config.get("SERVER"));
                        setserver.setString(2, player.getUuid().toString());
                        setserver.executeUpdate();
                        broken.remove(player.getUuid());
                    } else {
                        Thread.sleep(1000);
                        for (int i = 1; !onjoinstatement.executeQuery().next(); i++) {
                            if (i == 10) {
                                if (!Boolean.parseBoolean(config.get("DEM"))) player.sendMessage(Text.translatable("COULD NOT FIND " + player.getName().getString() + "'s DATA!\nMADE NEW ONE!").formatted(Formatting.RED));
                                LOGGER.warn("COULD NOT FIND {}'s DATA!\nMADE NEW ONE!", player.getName().getString());
                                player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_GLASS_BREAK), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                                broken.remove(player.getUuid());
                                return;
                            }
                            Thread.sleep(1000);
                        }
                    }
                    break;
                } catch (CommunicationsException ignored) {
                } catch (Exception e) {
                    player.getInventory().clear();
                    player.getEnderChestInventory().clear();
                    player.clearStatusEffects();
                    if (!Boolean.parseBoolean(config.get("DEM"))) player.sendMessage(Text.translatable("THERE WERE SOME ERRORS WHEN LOAD PLAYER DATA : \n" + e.getMessage()).formatted(Formatting.RED));
                    player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_ANVIL_DESTROY), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                    LOGGER.error("THERE WERE SOME ERRORS WHEN LOAD PLAYER DATA:");
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }

    private void ondisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        new Thread(() -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
            LOGGER.info("saving {}'s data...", player.getName().getString());

            while (true) {
                try {
                    checkskip.setString(1, player.getName().getString());
                    ResultSet checkskiprs = checkskip.executeQuery();
                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (!Boolean.parseBoolean(config.get("DSM"))) minecraftServer.getPlayerManager().broadcast(Text.translatable("skip saving because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW), false);
                        LOGGER.warn("skip saving because {}'s data includes skip list", player.getName().getString());
                        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 1f);
                        bea.setString(1, player.getUuidAsString());
                        bea.executeUpdate();
                        befalse.setString(1, player.getUuid().toString());
                        befalse.executeUpdate();
                        return;
                    }

                    if (broken.stream().anyMatch(bplayer -> bplayer.equals(player.getUuid()))) {
                        LOGGER.warn("skip saving because {}'s data was broken", player.getName().getString());
                        broken.remove(player.getUuid());
                        player.getInventory().clear();
                        player.getEnderChestInventory().clear();
                        player.clearStatusEffects();
                        ((PlayerManagerInvoker) minecraftServer.getPlayerManager()).invokesavePlayerData(player);
                        return;
                    }

                    ondisconnectstatement.setString(1, player.getName().getString());
                    ondisconnectstatement.setString(2, player.getUuidAsString());
                    ondisconnectstatement.setInt(3, player.getAir());
                    ondisconnectstatement.setFloat(4, player.getHealth());
                    ondisconnectstatement.setFloat(6, player.getHungerManager().getExhaustion());
                    ondisconnectstatement.setInt(7, player.getHungerManager().getFoodLevel());
                    ondisconnectstatement.setFloat(8, player.getHungerManager().getSaturationLevel());
                    ondisconnectstatement.setInt(9, ((HungerManagerAccessor) player.getHungerManager()).getFoodTickTimer());
                    ondisconnectstatement.setInt(13, player.getInventory().selectedSlot);
                    ondisconnectstatement.setInt(14, player.experienceLevel);
                    ondisconnectstatement.setFloat(15, player.experienceProgress);
                    ondisconnectstatement.setString(11, player.getInventory().offHand.get(0).isEmpty() ? "" : ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, player.getInventory().offHand.get(0)).resultOrPartial(LOGGER::error).orElseThrow().toString());
                    EnderChestInventory end = player.getEnderChestInventory();
                    StringBuilder endresults = new StringBuilder();
                    for (int i = 0; i < end.size(); i++) {
                        if (end.getStack(i).isEmpty()) continue;
                        endresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, end.getStack(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
                    }
                    ondisconnectstatement.setString(5, endresults.toString());
                    DefaultedList<ItemStack> main = player.getInventory().main;
                    StringBuilder mainresults = new StringBuilder();
                    for (int i = 0; i < main.size(); i++) {
                        if (main.get(i).isEmpty()) continue;
                        mainresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, main.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
                    }
                    ondisconnectstatement.setString(10, mainresults.toString());
                    DefaultedList<ItemStack> armor = player.getInventory().armor;
                    StringBuilder armorresults = new StringBuilder();
                    for (int i = 0; i < armor.size(); i++) {
                        if (armor.get(i).isEmpty()) continue;
                        armorresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, armor.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
                    }
                    ondisconnectstatement.setString(12, armorresults.toString());
                    StringBuilder effectresults = new StringBuilder();
                    player.getStatusEffects().forEach(effect -> effectresults.append(NbtCompound.CODEC.encodeStart(JsonOps.INSTANCE, effect.writeNbt(new NbtCompound())).resultOrPartial(LOGGER::error).orElseThrow()).append("&"));
                    ondisconnectstatement.setString(16, effectresults.toString());
                    ondisconnectstatement.executeUpdate();
                    player.getInventory().clear();
                    player.getEnderChestInventory().clear();
                    player.clearStatusEffects();
                    ((PlayerManagerInvoker) minecraftServer.getPlayerManager()).invokesavePlayerData(player);
                    LOGGER.info("success to save {}'s data", player.getName().getString());
                    break;
                } catch (CommunicationsException ignored) {
                } catch (Exception e) {
                    LOGGER.error("FAIL TO SAVE {}'s DATA:", player.getName().getString());
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }
}