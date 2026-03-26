package com.github.nekozuki0509.common.events;

import com.github.nekozuki0509.common.minecraft.Colors;
import com.github.nekozuki0509.common.minecraft.MinecraftPlayer;
import com.github.nekozuki0509.common.Sql;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import java.sql.ResultSet;

import static com.github.nekozuki0509.common.Common.*;
import static com.github.nekozuki0509.common.minecraft.Sounds.BLOCK_GLASS_BREAK;

public class Disconnect {

    public static void ondisconnect(MinecraftPlayer player) {
        new Thread(() -> {
            String playerN = player.getName();
            LOGGER.info("saving {}'s data...", playerN);

            while (true) {
                try {
                    ResultSet checkskiprs = Sql.checkSkip(playerN);

                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (config.isASM())
                            api.broadcast("skip saving because %s's data includes skip list".formatted(playerN), Colors.YELLOW);
                        LOGGER.warn("skip saving because {}'s data includes skip list", playerN);

                        api.playWorldSound(player, BLOCK_GLASS_BREAK);
                        Sql.beA(player.getUuid());

                        return;
                    }

                    if (broken.stream().anyMatch(bplayer -> bplayer.equals(player.getUuid()))) {
                        LOGGER.warn("skip saving because {}'s data was broken", playerN);
                        broken.remove(player.getUuid());

                        api.clearInventory(player);
                        api.clearEnderChestInventory(player);
                        api.clearStatusEffects(player);
                        api.savePlayerData(player);

                        return;
                    }

                    Sql.disconnect(player);

                    api.savePlayerData(player);
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