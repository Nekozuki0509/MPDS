package mpds.mpds.events;

import com.mojang.serialization.JsonOps;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mpds.mpds.mixin.HungerManagerAccessor;
import mpds.mpds.mixin.PlayerManagerInvoker;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;

import java.sql.ResultSet;

import static mpds.mpds.MPDS.*;

public class Disconnect {
    public static void ondisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        new Thread(() -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
            LOGGER.info("saving {}'s data...", player.getName().getString());

            while (true) {
                try {
                    checkskip.setString(1, player.getName().getString());
                    ResultSet checkskiprs = checkskip.executeQuery();

                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (ASM)
                            minecraftServer.getPlayerManager().broadcast(new TranslatableText("skip saving because " + player.getName().getString() + "'s data includes skip list").formatted(Formatting.YELLOW), MessageType.SYSTEM, Util.NIL_UUID);
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

                    if (SA) ondisconnectstatement.setInt(3, player.getAir());

                    if (SH) ondisconnectstatement.setFloat(4, player.getHealth());

                    if (SF) {
                        ondisconnectstatement.setFloat(6, player.getHungerManager().getExhaustion());
                        ondisconnectstatement.setInt(7, player.getHungerManager().getFoodLevel());
                        ondisconnectstatement.setFloat(8, player.getHungerManager().getSaturationLevel());
                        ondisconnectstatement.setInt(9, ((HungerManagerAccessor) player.getHungerManager()).getFoodTickTimer());
                    }

                    if (SL) {
                        ondisconnectstatement.setInt(14, player.experienceLevel);
                        ondisconnectstatement.setFloat(15, player.experienceProgress);
                    }

                    if (SEn) {
                        EnderChestInventory end = player.getEnderChestInventory();
                        StringBuilder endresults = new StringBuilder();
                        for (int i = 0; i < end.size(); i++) {
                            if (end.getStack(i).isEmpty()) continue;
                            endresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, end.getStack(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
                        }
                        ondisconnectstatement.setString(5, endresults.toString());

                        player.getEnderChestInventory().clear();
                    }

                    if (SI) {
                        ondisconnectstatement.setString(11, player.getInventory().offHand.get(0).isEmpty() ? "" : ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, player.getInventory().offHand.get(0)).resultOrPartial(LOGGER::error).orElseThrow().toString());
                        ondisconnectstatement.setInt(13, player.getInventory().selectedSlot);

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

                        player.getInventory().clear();
                    }

                    if (SEf) {
                        StringBuilder effectresults = new StringBuilder();
                        player.getStatusEffects().forEach(effect -> effectresults.append(NbtCompound.CODEC.encodeStart(JsonOps.INSTANCE, effect.writeNbt(new NbtCompound())).resultOrPartial(LOGGER::error).orElseThrow()).append("&"));
                        ondisconnectstatement.setString(16, effectresults.toString());
                        ondisconnectstatement.executeUpdate();

                        player.clearStatusEffects();
                    }

                    ((PlayerManagerInvoker) minecraftServer.getPlayerManager()).invokesavePlayerData(player);
                    LOGGER.info("success to save {}'s data", player.getName().getString());

                    return;
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
