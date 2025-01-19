package mpds.mpds.events;

import com.google.gson.JsonParser;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mpds.mpds.mixin.HungerManagerAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.sql.ResultSet;
import java.util.List;

import static mpds.mpds.MPDS.*;

public class Join {
    public static void onjoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        new Thread(() -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
            broken.add(player.getUuid());

            if (AJM)
                player.sendMessage(Text.translatable("loading " + player.getName().getString() + "'s data...").formatted(Formatting.YELLOW));
            LOGGER.info("loading {}'s data...", player.getName().getString());

            while (true) {
                try {
                    checkskip.setString(1, player.getName().getString());
                    ResultSet checkskiprs = checkskip.executeQuery();

                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (ASM) {
                            minecraftServer.getPlayerManager().broadcast(Text.translatable("skip loading because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW), false);
                            player.sendMessage(Text.translatable("skip loading because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW));
                        }
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
                            if (i == 3) {
                                if (ServerName.equals(resultSet.getString("server")) || "*".equals(resultSet.getString("server"))) {
                                    if (AJM)
                                        player.sendMessage(Text.translatable("saved " + player.getName().getString() + "'s correct data").formatted(Formatting.AQUA));
                                    LOGGER.info("saved {}'s correct data", player.getName().getString());

                                    broken.remove(player.getUuid());
                                    player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_PLAYER_LEVELUP), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));

                                    return;
                                }
                                if (AEM)
                                    player.sendMessage(Text.translatable("IT LOOKS " + player.getName().getString() + "'s DATA WAS BROKEN!\nPLEASE CONNECT TO " + resultSet.getString("server") + "!").formatted(Formatting.RED));
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

                        if (SA) player.setAir(resultSet.getInt("Air"));

                        if (SH) player.setHealth(resultSet.getFloat("Health"));

                        if (SF) {
                            player.getHungerManager().setExhaustion(resultSet.getFloat("exhaustion"));
                            player.getHungerManager().setFoodLevel(resultSet.getInt("foodLevel"));
                            player.getHungerManager().setSaturationLevel(resultSet.getFloat("saturationLevel"));
                            ((HungerManagerAccessor) player.getHungerManager()).setFoodTickTimer(resultSet.getInt("foodTickTimer"));
                        }

                        if (SL) {
                            player.experienceLevel = resultSet.getInt("experienceLevel");
                            player.experienceProgress = resultSet.getInt("experienceProgress");
                        }

                        if (SEn && !"".equals(resultSet.getString("enderChestInventory")))
                            List.of(resultSet.getString("enderChestInventory").split("&")).forEach(compound -> {
                                String[] compounds = compound.split("~");
                                player.getEnderChestInventory().setStack(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                            });

                        if (SI) {
                            if (!"".equals(resultSet.getString("off")))
                                player.getInventory().offHand.set(0, ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(resultSet.getString("off"))).resultOrPartial(LOGGER::error).orElseThrow());
                            player.getInventory().selectedSlot = resultSet.getInt("selectedSlot");
                            if (!"".equals(resultSet.getString("main"))) {
                                List.of(resultSet.getString("main").split("&")).forEach(compound -> {
                                    String[] compounds = compound.split("~");
                                    player.getInventory().main.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                                });
                            }
                            if (!"".equals(resultSet.getString("armor"))) {
                                List.of(resultSet.getString("armor").split("&")).forEach(compound -> {
                                    String[] compounds = compound.split("~");
                                    player.getInventory().armor.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                                });
                            }
                        }

                        if (SEf && !"".equals(resultSet.getString("effects")))
                            List.of(resultSet.getString("effects").split("&")).forEach(compound -> player.addStatusEffect(StatusEffectInstance.CODEC.parse(wrappedOps, JsonParser.parseString(compound)).resultOrPartial(LOGGER::error).orElseThrow()));

                        if (AJM)
                            player.sendMessage(Text.translatable("success to load " + player.getName().getString() + "'s data!").formatted(Formatting.AQUA));
                        LOGGER.info("success to load {}'s data!", player.getName().getString());

                        player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_PLAYER_LEVELUP), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));

                        setserver.setString(1, ServerName);
                        setserver.setString(2, player.getUuid().toString());
                        setserver.executeUpdate();

                        broken.remove(player.getUuid());
                    } else {
                        Thread.sleep(1000);

                        for (int i = 1; !onjoinstatement.executeQuery().next(); i++) {
                            if (i == 10) {
                                if (AEM)
                                    player.sendMessage(Text.translatable("COULD NOT FIND " + player.getName().getString() + "'s DATA!\nMADE NEW ONE!").formatted(Formatting.RED));
                                LOGGER.warn("COULD NOT FIND {}'s DATA!\nMADE NEW ONE!", player.getName().getString());

                                player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_GLASS_BREAK), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                                broken.remove(player.getUuid());

                                return;
                            }
                            Thread.sleep(1000);
                        }
                    }

                    return;
                } catch (CommunicationsException ignored) {
                } catch (Exception e) {
                    player.getInventory().clear();
                    player.getEnderChestInventory().clear();
                    player.clearStatusEffects();

                    if (AEM)
                        player.sendMessage(Text.translatable("THERE WERE SOME ERRORS WHEN LOAD " + player.getName().getString() + "'s DATA! : \n" + e.getMessage()).formatted(Formatting.RED));
                    LOGGER.error("THERE WERE SOME ERRORS WHEN LOAD {}'s DATA!:", player.getName().getString());

                    player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_ANVIL_DESTROY), SoundCategory.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
                    e.printStackTrace();

                    return;
                }
            }
        }).start();
    }
}
