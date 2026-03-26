package com.github.nekozuki0509.mpds.impl;

import com.github.nekozuki0509.common.minecraft.Colors;
import com.github.nekozuki0509.common.minecraft.MinecraftApi;
import com.github.nekozuki0509.common.minecraft.MinecraftPlayer;
import com.github.nekozuki0509.common.minecraft.Sounds;

import java.nio.file.Path;

public class MinecraftApiImpl implements MinecraftApi {
    @Override
    public Path getConfigDir() {
        return null;
    }

    @Override
    public void registerEvents() {

    }

    @Override
    public void broadcast(String msg, Colors color) {

    }

    @Override
    public void savePlayerData(MinecraftPlayer player) {

    }

    @Override
    public void playWorldSound(MinecraftPlayer player, Sounds sound) {

    }

    @Override
    public void sqlToPlayer(MinecraftPlayer player) {

    }

    @Override
    public void sendMessage(MinecraftPlayer player, String string, Colors color) {

    }

    @Override
    public void playPlayerSound(MinecraftPlayer player, Sounds sounds) {

    }

    @Override
    public void clearInventory(MinecraftPlayer player) {

    }

    @Override
    public void clearEnderChestInventory(MinecraftPlayer player) {
        
    }

    @Override
    public void clearStatusEffects(MinecraftPlayer player) {

    }
}
