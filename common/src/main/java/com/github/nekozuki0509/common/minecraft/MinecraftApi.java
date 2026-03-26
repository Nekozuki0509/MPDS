package com.github.nekozuki0509.common.minecraft;

import java.nio.file.Path;

public interface MinecraftApi {

    Path getConfigDir();

    void registerEvents();

    void broadcast(String msg, Colors color);

    void savePlayerData(MinecraftPlayer player);

    void playWorldSound(MinecraftPlayer player, Sounds sound);

    void sqlToPlayer(MinecraftPlayer player);

    void sendMessage(MinecraftPlayer player, String string, Colors color);

    void playPlayerSound(MinecraftPlayer player, Sounds sounds);

    void clearInventory(MinecraftPlayer player);

    void clearEnderChestInventory(MinecraftPlayer player);

    void clearStatusEffects(MinecraftPlayer player);
}
