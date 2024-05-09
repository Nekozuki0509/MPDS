package mpds.mpds;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.JsonOps;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
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

	static final List<UUID> skip = new ArrayList<>();
	
	static Path configjson;
	
	static HashMap<String, String> config;
	
	public static Gson gson = new Gson();

	PreparedStatement onjoinstatement = connection.prepareStatement("SELECT * FROM " + config.get("TABLE_NAME") + " WHERE uuid = ?");

	PreparedStatement checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

	PreparedStatement showskip = connection.prepareStatement("SELECT * FROM skipplayer");

	PreparedStatement updateskip = connection.prepareStatement("INSERT INTO skip (Name, skip) VALUES (?, ?) AS new ON DUPLICATE KEY UPDATE Name=new.Name, skip=new.skip");

	PreparedStatement befalse = connection.prepareStatement("UPDATE " + config.get("TABLE_NAME") + " SET sync=\"false\" WHERE uuid = ?");

	PreparedStatement setserver = connection.prepareStatement("UPDATE " + config.get("TABLE_NAME") + " SET server=? WHERE uuid = ?");

	PreparedStatement ondisconnectstatement = connection.prepareStatement("INSERT INTO " + config.get("TABLE_NAME") +
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
			"effects=new,effects," +
			"sync=new.sync");

	public MPDS() throws SQLException {
	}

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
			config = gson.fromJson(reader, new TypeToken<HashMap<String, String>>() {}.getType());
		} catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
			connection = DriverManager.getConnection("jdbc:mysql://" + config.get("HOST") + "/" + config.get("DB_NAME") + "?autoReconnect=true", config.get("USER"), config.get("PASSWD"));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

        try {
			connection.prepareStatement
					("CREATE TABLE IF NOT EXISTS " + config.get("TABLE_NAME") +"(" +
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
		} catch (SQLException e){
			LOGGER.error("FAIL TO CONNECT MYSQL");
			LOGGER.error("DID YOU CHANGE MPDS CONFIG?");
			e.printStackTrace();
		}

		ServerPlayConnectionEvents.JOIN.register(this::onjoin);
		ServerPlayConnectionEvents.DISCONNECT.register(this::ondisconnect);

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
				dispatcher.register(literal("updateskip").then(argument("player", StringArgumentType.word()).then(argument("skip", BoolArgumentType.bool())
						.executes(ctx -> {
							try {
								updateskip.setString(1, StringArgumentType.getString(ctx, "player"));
								updateskip.setString(2, String.valueOf(BoolArgumentType.getBool(ctx, "skip")));
								updateskip.executeUpdate();
							} catch (SQLException e) {
								throw new RuntimeException(e);
							}
							ctx.getSource().getServer().sendSystemMessage(new TranslatableText("set " + StringArgumentType.getString(ctx, "player") + "'s data " + BoolArgumentType.getBool(ctx, "skip")).formatted(Formatting.YELLOW), Util.NIL_UUID);
							return 1;
						})
				))));

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
				dispatcher.register(literal("showskip")
						.executes(ctx -> {
							ResultSet skiprs;
							StringBuilder skip = new StringBuilder();
							try {
								while ((skiprs = showskip.executeQuery()).next()) {
									if ("false".equals(skiprs.getString("skip"))) continue;
									skip.append("・").append(skiprs.getString("Name")).append("\n");
								}
								ctx.getSource().getServer().sendSystemMessage(Text.of(skip.toString()), Util.NIL_UUID);
							} catch (SQLException e) {
								throw new RuntimeException(e);
							}
							return 1;
						})
				));

		LOGGER.info("MPDS loaded");
	}

	private void onjoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
		new Thread(() -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
			broken.add(player.getUuid());
			player.sendSystemMessage(new TranslatableText("loading " + player.getName().getString() + "'s data...").formatted(Formatting.YELLOW), Util.NIL_UUID);
            LOGGER.info("loading " + player.getName().getString() + "'s data...");
			while (true) {
				try {
					checkskip.setString(1, player.getName().getString());
					ResultSet checkskiprs = checkskip.executeQuery();
					if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
						minecraftServer.sendSystemMessage(new TranslatableText("skip loading because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW), Util.NIL_UUID);
						LOGGER.warn("skip loading because " + player.getName().getString() + "'s data includes skip list");
						skip.add(player.getUuid());
						broken.remove(player.getUuid());
						player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1f, 1f);
						return;
					}
					onjoinstatement.setString(1, player.getUuid().toString());
					ResultSet resultSet;
					if ((resultSet = onjoinstatement.executeQuery()).next()) {
						for (int i = 0; "false".equals(resultSet.getString("sync")); i++) {
							if (i == 10) {
								if (config.get("SERVER").equals(resultSet.getString("server"))) {
									player.sendSystemMessage(new TranslatableText("saved " + player.getName().getString() + "'s correct data").formatted(Formatting.AQUA), Util.NIL_UUID);
									LOGGER.info("saved " + player.getName().getString() + "'s correct data");
									player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f));
									return;
								}
								player.sendSystemMessage(new TranslatableText("IT LOOKS " + player.getName().getString() + "'s DATA WAS BROKEN!").formatted(Formatting.RED), Util.NIL_UUID);
								player.sendSystemMessage(new TranslatableText("PLEASE CONNECT TO " + resultSet.getString("server") + "!").formatted(Formatting.RED), Util.NIL_UUID);
								LOGGER.error("IT LOOKS " + player.getName().getString() + "'s DATA WAS BROKEN!");
								LOGGER.error("PLEASE CONNECT TO " + resultSet.getString("server") + "!");
								player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f));
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
						if (!"".equals(resultSet.getString("off"))) player.getInventory().offHand.set(0, ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(resultSet.getString("off"))).resultOrPartial(LOGGER::error).orElseThrow());
						player.getInventory().selectedSlot = resultSet.getInt("selectedSlot");
						player.experienceLevel = resultSet.getInt("experienceLevel");
						player.experienceProgress = resultSet.getInt("experienceProgress");
						List.of(resultSet.getString("enderChestInventory").split("&")).forEach(compound -> {
							String[] compounds = compound.split("~");
							player.getEnderChestInventory().setStack(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
						});
						List.of(resultSet.getString("main").split("&")).forEach(compound -> {
							String[] compounds = compound.split("~");
							player.getInventory().main.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
						});
						List.of(resultSet.getString("armor").split("&")).forEach(compound -> {
							String[] compounds = compound.split("~");
							player.getInventory().armor.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
						});
						List.of(resultSet.getString("effects").split("&")).forEach(compound -> player.addStatusEffect(StatusEffectInstance.fromNbt(NbtCompound.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compound)).resultOrPartial(LOGGER::error).orElseThrow())));
						player.sendSystemMessage(new TranslatableText("success to load " + player.getName().getString() + "'s data!").formatted(Formatting.AQUA), Util.NIL_UUID);
						LOGGER.info("success to load " + player.getName().getString() + "'s data!");
						player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f));
					} else {
						player.sendSystemMessage(new TranslatableText("COULD NOT FIND " + player.getName().getString() + "'s DATA!").formatted(Formatting.RED), Util.NIL_UUID);
						player.sendSystemMessage(new TranslatableText("MADE NEW ONE!").formatted(Formatting.RED), Util.NIL_UUID);
						LOGGER.warn("COULD NOT FIND " + player.getName().getString() + "'s DATA!");
						LOGGER.warn("MADE NEW ONE!");
						player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f));
					}
					setserver.setString(1, config.get("SERVER"));
					setserver.setString(2, player.getUuid().toString());
					setserver.executeUpdate();
					broken.remove(player.getUuid());
					return;
				} catch (CommunicationsException ignored) {
				} catch (Exception e) {
					player.sendSystemMessage(new TranslatableText("THERE WERE SOME ERRORS WHEN LOAD PLAYER DATA : \n" + e.getMessage()).formatted(Formatting.RED), Util.NIL_UUID);
					player.networkHandler.sendPacket(new PlaySoundS2CPacket(SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f));
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
			LOGGER.info("saving " + player.getName().getString() + "'s data...");

			if (skip.stream().anyMatch(splayer -> splayer.equals(player.getUuid()))) {
				minecraftServer.sendSystemMessage(new TranslatableText("skip saving because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW), Util.NIL_UUID);
				LOGGER.warn("skip saving because " + player.getName().getString() + "'s data includes skip list");
				skip.remove(player.getUuid());
				return;
			}

			if (broken.stream().anyMatch(bplayer -> bplayer.equals(player.getUuid()))) {
				LOGGER.warn("skip saving because " + player.getName().getString() + "'s data was broken");
				broken.remove(player.getUuid());
				return;
			}

			while (true) {
				try {
					ondisconnectstatement.setString(1, player.getName().getString());
					ondisconnectstatement.setString(2, player.getUuidAsString());
					ondisconnectstatement.setInt(3, player.getAir());
					ondisconnectstatement.setFloat(4, player.getHealth());
					ondisconnectstatement.setFloat(6, player.getHungerManager().getExhaustion());
					ondisconnectstatement.setInt(7, player.getHungerManager().getFoodLevel());
					ondisconnectstatement.setFloat(8, player.getHungerManager().getSaturationLevel());
					ondisconnectstatement.setInt(9, ((HungerManagerAccessor) player.getHungerManager()).getFoodTickTimer());
					ondisconnectstatement.setString(11, player.getInventory().offHand.get(0).isEmpty()?"":ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, player.getInventory().offHand.get(0)).resultOrPartial(LOGGER::error).orElseThrow().toString());
					ondisconnectstatement.setInt(13, player.getInventory().selectedSlot);
					ondisconnectstatement.setInt(14, player.experienceLevel);
					ondisconnectstatement.setFloat(15, player.experienceProgress);
					EnderChestInventory end = player.getEnderChestInventory();
					StringBuilder endresults = new StringBuilder();
					for (int i = 0; i < end.size() && !end.getStack(i).isEmpty(); i++) {
						endresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, end.getStack(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
					}
					ondisconnectstatement.setString(5, endresults.toString());
					DefaultedList<ItemStack> main = player.getInventory().main;
					StringBuilder mainresults = new StringBuilder();
					for (int i = 0; i < main.size() && !main.get(i).isEmpty(); i++) {
						mainresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, main.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
					}
					ondisconnectstatement.setString(10, mainresults.toString());
					DefaultedList<ItemStack> armor = player.getInventory().armor;
					StringBuilder armorresults = new StringBuilder();
					for (int i = 0; i < armor.size() && !armor.get(i).isEmpty(); i++) {
						armorresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, armor.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
					}
					ondisconnectstatement.setString(12, armorresults.toString());
					StringBuilder effectresults = new StringBuilder();
					player.getStatusEffects().forEach(effect -> effectresults.append(NbtCompound.CODEC.encodeStart(JsonOps.INSTANCE, effect.writeNbt(new NbtCompound())).resultOrPartial(LOGGER::error).orElseThrow()).append("&"));
					ondisconnectstatement.setString(16, effectresults.toString());
					ondisconnectstatement.executeUpdate();
					LOGGER.info("success to save " + player.getName().getString() + "'s data");
					player.getInventory().clear();
					player.getEnderChestInventory().clear();
					player.clearStatusEffects();
					return;
				} catch (CommunicationsException ignored) {
				} catch (Exception e) {
					broken.remove(player.getUuid());
					LOGGER.error("FAIL TO SAVE " + player.getName().getString() + "'s DATA:");
					e.printStackTrace();
					return;
				}
			}
		}).start();
	}
}