package com.github.nekozuki0509.mpds.common;

import com.github.nekozuki0509.mpds.common.minecraft.ArgumentGetter;
import com.github.nekozuki0509.mpds.common.minecraft.Colors;
import com.github.nekozuki0509.mpds.common.minecraft.MinecraftPlayer;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.sql.ResultSet;

import static com.github.nekozuki0509.mpds.common.Common.LOGGER;
import static com.github.nekozuki0509.mpds.common.Common.api;

public class Commands {

    public static int updateSkip(ArgumentGetter getter, MinecraftPlayer player) {
        while (true) {
            try {
                Sql.updateSkip(getter.getString("player"), String.valueOf(getter.getBool("skip")));
                api.broadcast("set %s's data %s".formatted(getter.getString("player"), getter.getBool("skip")), Colors.YELLOW);

                return 1;
            } catch (CommunicationsException ignored) {
            } catch (Exception e) {
                api.broadcast("THERE WERE SOME ERRORS : \n%s".formatted(e.getMessage()), Colors.RED);
                LOGGER.error("THERE WERE SOME ERRORS :\n{}", ExceptionUtils.getStackTrace(e));

                return 1;
            }
        }
    }

    public static int showSkip(ArgumentGetter getter, MinecraftPlayer player) {
        ResultSet skiprs;
        StringBuilder skipp = new StringBuilder();

        while (true) {
            try {
                skiprs = Sql.showSkip();

                while (skiprs.next()) {
                    if ("false".equals(skiprs.getString("skip"))) continue;
                    skipp.append("・").append(skiprs.getString("Name")).append("\n");
                }

                if (player != null) {
                    api.sendMessage(player, skipp.toString(), Colors.WHITE);
                } else {
                    LOGGER.info(skipp.toString());
                }

                return 1;
            } catch (CommunicationsException ignored) {
            } catch (Exception e) {
                api.broadcast("THERE WERE SOME ERRORS : \n%s".formatted(e.getMessage()), Colors.RED);
                LOGGER.error("THERE WERE SOME ERRORS :\n{}", ExceptionUtils.getStackTrace(e));

                return 1;
            }
        }
    }
}
