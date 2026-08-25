package com.github.nekozuki0509.mpds.common.minecraft;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface MinecraftApi {

    Path getConfigDir();

    void registerCommand(String name, CommandFunction consumer);

    void onJoin(Consumer<MinecraftPlayer> handler);

    void onDisconnect(Consumer<MinecraftPlayer> handler);

    void onServerStopped(Runnable handler);

    void broadcast(String msg, Colors color);

    void playWorldSound(MinecraftPlayer player, Sounds sound);

    void sqlToPlayer(MinecraftPlayer player);

    void sendMessage(MinecraftPlayer player, String string, Colors color);

    void playPlayerSound(MinecraftPlayer player, Sounds sound);

    void clearInventory(MinecraftPlayer player);

    void clearEnderChestInventory(MinecraftPlayer player);

    void clearStatusEffects(MinecraftPlayer player);
}
