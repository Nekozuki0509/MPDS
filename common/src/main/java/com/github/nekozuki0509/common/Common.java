package com.github.nekozuki0509.common;

import com.github.nekozuki0509.common.minecraft.MinecraftApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

public class Common {

    public static final Logger LOGGER = LoggerFactory.getLogger("mpds");

    public static MinecraftApi api;

    public static final List<String> broken = new ArrayList<>();

    public static Config config;

    public static void init() {
        config = Config.init();

        try {
            Sql.init();
        } catch (SQLException e) {
            LOGGER.error("FAIL TO CONNECT MYSQL");
            LOGGER.error("DID YOU CHANGE MPDS CONFIG?");
            e.printStackTrace();
        }

        api.registerEvents();

        LOGGER.info("MPDS loaded");
    }
}