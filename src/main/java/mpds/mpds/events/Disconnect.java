package mpds.mpds.events;

import com.mojang.serialization.JsonOps;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import mpds.mpds.mixin.HungerManagerAccessor;
import mpds.mpds.mixin.PlayerManagerInvoker;
import mpds.mpds.sql;
import mpds.mpds.sqlPlayer;
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
import static net.minecraft.sound.SoundEvents.BLOCK_GLASS_BREAK;

public class Disconnect {

    public static void ondisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        new Thread(() -> {
            ServerPlayerEntity player = serverPlayNetworkHandler.getPlayer();
            String playerN = player.getName().getString();
            LOGGER.info("saving {}'s data...", playerN);

            while (true) {
                try {
                    ResultSet checkskiprs = sql.checkSkip(playerN);

                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (ASM)
                            minecraftServer.getPlayerManager().broadcast(new TranslatableText("skip saving because " + playerN + "'s data includes skip list").formatted(Formatting.YELLOW), MessageType.SYSTEM, Util.NIL_UUID);
                        LOGGER.warn("skip saving because {}'s data includes skip list", playerN);

                        player.getWorld().playSound(null, player.getBlockPos(), BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 1f);
                        sql.beA(player.getUuidAsString());

                        return;
                    }

                    if (broken.stream().anyMatch(bplayer -> bplayer.equals(player.getUuid()))) {
                        LOGGER.warn("skip saving because {}'s data was broken", playerN);
                        broken.remove(player.getUuid());

                        player.getInventory().clear();
                        player.getEnderChestInventory().clear();
                        player.clearStatusEffects();
                        ((PlayerManagerInvoker) minecraftServer.getPlayerManager()).invokesavePlayerData(player);

                        return;
                    }

                    sql.disconnect(new sqlPlayer(player));

                    ((PlayerManagerInvoker) minecraftServer.getPlayerManager()).invokesavePlayerData(player);
                    LOGGER.info("success to save {}'s data", playerN);

                    return;
                } catch (CommunicationsException ignored) {
                } catch (Exception e) {
                    LOGGER.error("FAIL TO SAVE {}'s DATA:", playerN);
                    e.printStackTrace();

                    return;
                }
            }
        }).start();
    }
}
