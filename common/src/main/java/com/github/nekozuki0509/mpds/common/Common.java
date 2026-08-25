package com.github.nekozuki0509.mpds.common;

import com.github.nekozuki0509.mpds.common.events.Disconnect;
import com.github.nekozuki0509.mpds.common.events.Join;
import com.github.nekozuki0509.mpds.common.events.ServerStopped;
import com.github.nekozuki0509.mpds.common.minecraft.MinecraftApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Common {

    public static final Logger LOGGER = LoggerFactory.getLogger("mpds");

    public static MinecraftApi api;

    public static final List<String> broken = new ArrayList<>();

    public static final List<String> prevent = new ArrayList<>();

    public static Config config;

    public static void init(MinecraftApi impledApi) {
        api = impledApi;
        config = Config.init();

        try {
            Sql.init();
        } catch (SQLException e) {
            LOGGER.error("FAIL TO CONNECT MYSQL");
            LOGGER.error("DID YOU CHANGE MPDS CONFIG?");
            e.printStackTrace();
        }

        api.onJoin(Join::onjoin);
        api.onDisconnect(Disconnect::ondisconnect);
        api.onServerStopped(ServerStopped::onServerStopped);

        api.registerCommand("updateSkip", Commands::updateSkip);
        api.registerCommand("showSkip", Commands::showSkip);

        LOGGER.info("MPDS loaded");
    }
}