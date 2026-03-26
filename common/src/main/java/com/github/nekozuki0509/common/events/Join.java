package com.github.nekozuki0509.common.events;

import com.github.nekozuki0509.common.Sql;
import com.github.nekozuki0509.common.minecraft.Colors;
import com.github.nekozuki0509.common.minecraft.MinecraftPlayer;
import com.github.nekozuki0509.common.minecraft.Sounds;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import java.sql.ResultSet;

import static com.github.nekozuki0509.common.Common.*;

public class Join {

    public static void onjoin(MinecraftPlayer player) {
        new Thread(() -> {
            String playerN = player.getName();
            broken.add(player.getUuid());

            if (config.isAJM())
                api.sendMessage(player, "loading %s's data...".formatted(playerN), Colors.YELLOW);
            LOGGER.info("loading {}'s data...", playerN);

            while (true) {
                try {
                    ResultSet checkskiprs = Sql.checkSkip(playerN);

                    if (checkskiprs.next() && "true".equals(checkskiprs.getString("skip"))) {
                        if (config.isASM()) {
                            api.broadcast("skip loading because %s's data includes skip list".formatted(playerN), Colors.YELLOW);
                            api.sendMessage(player, "skip loading because %s's data includes skip list".formatted(playerN), Colors.YELLOW);
                        }
                        LOGGER.warn("skip loading because {}'s data includes skip list", playerN);

                        broken.remove(player.getUuid());

                        api.playPlayerSound(player, Sounds.BLOCK_GLASS_BREAK);
                        api.playWorldSound(player, Sounds.BLOCK_GLASS_BREAK);

                        return;
                    }

                    ResultSet resultSet;
                    if ((resultSet = Sql.join(player.getUuid())).next()) {
                        for (int i = 0; "false".equals(resultSet.getString("sync")); i++) {
                            if (i == 3) {
                                if (config.getServerName().equals(resultSet.getString("server")) || "*".equals(resultSet.getString("server"))) {
                                    if (config.isAJM())
                                        api.sendMessage(player, "saved %s's correct data".formatted(playerN), Colors.AQUA);
                                    LOGGER.info("saved {}'s correct data", playerN);

                                    broken.remove(player.getUuid());
                                    api.playPlayerSound(player, Sounds.ENTITY_PLAYER_LEVELUP);

                                    return;
                                }
                                if (config.isAEM())
                                    api.sendMessage(player, "IT LOOKS %s's DATA WAS BROKEN!\nPLEASE CONNECT TO %s!".formatted(playerN, resultSet.getString("server")), Colors.RED);
                                LOGGER.error("IT LOOKS {}'s DATA WAS BROKEN!\nPLEASE CONNECT TO {}!", playerN, resultSet.getString("server"));
                                api.playPlayerSound(player, Sounds.BLOCK_ANVIL_DESTROY);

                                return;
                            }
                            Thread.sleep(1000);
                            (resultSet = Sql.join(player.getUuid())).next();
                        }

                        Sql.beFalse(player.getUuid());
                        api.sqlToPlayer(new MinecraftPlayer(resultSet));

                        if (config.isAJM())
                            api.sendMessage(player, "success to load %s's data!".formatted(playerN), Colors.AQUA);
                        LOGGER.info("success to load {}'s data!", playerN);

                        api.playPlayerSound(player, Sounds.ENTITY_PLAYER_LEVELUP);
                        Sql.setServer(player.getUuid());
                        broken.remove(player.getUuid());

                    } else {
                        Thread.sleep(1000);

                        for (int i = 1; !Sql.join(player.getUuid()).next(); i++) {
                            if (i == 3) {
                                if (config.isAEM())
                                    api.sendMessage(player, "COULD NOT FIND %s's DATA!\nMADE NEW ONE!".formatted(playerN), Colors.RED);
                                LOGGER.warn("COULD NOT FIND {}'s DATA!\nMADE NEW ONE!", playerN);

                                api.playPlayerSound(player, Sounds.BLOCK_GLASS_BREAK);
                                broken.remove(player.getUuid());
                                Sql.setServer(player.getUuid());

                                return;
                            }
                            Thread.sleep(1000);
                        }
                    }

                    return;
                } catch (CommunicationsException ignored) {
                } catch (Exception e) {
                    api.clearInventory(player);
                    api.clearEnderChestInventory(player);
                    api.clearStatusEffects(player);

                    if (config.isAEM())
                        api.sendMessage(player, "THERE WERE SOME ERRORS WHEN LOAD PLAYER DATA : \n%s".formatted(e.getMessage()), Colors.RED);
                    LOGGER.error("THERE WERE SOME ERRORS WHEN LOAD {}'s DATA!:", playerN);

                    api.playPlayerSound(player, Sounds.BLOCK_ANVIL_DESTROY);
                    e.printStackTrace();

                    return;
                }
            }
        }).start();
    }
}