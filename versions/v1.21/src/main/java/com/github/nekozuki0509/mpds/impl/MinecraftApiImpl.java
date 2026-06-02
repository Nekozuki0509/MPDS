package com.github.nekozuki0509.mpds.impl;

import com.github.nekozuki0509.common.minecraft.*;
import com.github.nekozuki0509.mpds.mixins.HungerManagerAccessor;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static com.github.nekozuki0509.common.Common.LOGGER;
import static com.github.nekozuki0509.common.Common.config;
import static com.github.nekozuki0509.mpds.Mpds.server;
import static com.github.nekozuki0509.mpds.Mpds.wrappedOps;
import static net.minecraft.server.command.CommandManager.literal;

public class MinecraftApiImpl implements MinecraftApi {
    private static final HashMap<String, ServerPlayerEntity> players = new HashMap<>();

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void registerCommand(String name, CommandFunction consumer) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal(name)
                        .executes(ctx -> consumer.execute(new ArgumentGetterImpl(ctx), transformPlayer(ctx.getSource().getPlayer())))
                )
        );
    }

    @Override
    public void onJoin(Consumer<MinecraftPlayer> handler) {
        ServerPlayConnectionEvents.INIT.register((h, s) -> handler.accept(transformPlayer(h.player)));
    }

    @Override
    public void onDisconnect(Consumer<MinecraftPlayer> handler) {
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> handler.accept(transformPlayer(h.player)));
    }

    @Override
    public void onServerStopped(Runnable handler) {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> handler.run());
    }

    @Override
    public void broadcast(String msg, Colors color) {
        server.getPlayerManager().broadcast(Text.translatable(msg).formatted(transformColor(color)), false);
    }

    @Override
    public void playWorldSound(MinecraftPlayer player, Sounds sound) {
        ServerPlayerEntity playerEntity = players.get(player.getUuid());
        playerEntity.getWorld().playSound(null, playerEntity.getBlockPos(), transformSound(sound), SoundCategory.PLAYERS, 1f, 1f);
    }

    @Override
    public void sqlToPlayer(MinecraftPlayer player) {
        ServerPlayerEntity playerEntity = players.get(player.getUuid());

        if (config.isSA()) playerEntity.setAir(player.getAir());
        if (config.isSH()) playerEntity.setHealth(player.getHealth());

        if (config.isSF()) {
            playerEntity.getHungerManager().setExhaustion(player.getExhaustion());
            playerEntity.getHungerManager().setFoodLevel(player.getFoodLevel());
            playerEntity.getHungerManager().setSaturationLevel(player.getSaturationLevel());
            ((HungerManagerAccessor) playerEntity.getHungerManager()).setFoodTickTimer(player.getFoodTickTimer());
        }

        if (config.isSL()) {
            playerEntity.setExperienceLevel(player.getExperienceLevel());
            playerEntity.experienceProgress = player.getExperienceProgress();
        }

        if (config.isSEn() && !player.getEnderChestInventory().isEmpty()) {
            List.of(player.getEnderChestInventory().split("&")).forEach(compound -> {
                String[] compounds = compound.split("~");
                playerEntity.getEnderChestInventory().setStack(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
            });
        }

        if (config.isSI()) {
            if (!player.getOff().isEmpty()) {
                playerEntity.getInventory().offHand.set(0, ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(player.getOff())).resultOrPartial(LOGGER::error).orElseThrow());
            }

            playerEntity.getInventory().selectedSlot = player.getSelectedSlot();
            playerEntity.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getSelectedSlot()));

            if (!player.getMain().isEmpty()) {
                List.of(player.getMain().split("&")).forEach(compound -> {
                    String[] compounds = compound.split("~");
                    playerEntity.getInventory().main.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                });
            }

            if (!player.getArmor().isEmpty()) {
                List.of(player.getArmor().split("&")).forEach(compound -> {
                    String[] compounds = compound.split("~");
                    playerEntity.getInventory().armor.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                });
            }
        }

        if (config.isSEf() && !player.getEffects().isEmpty()) {
            List.of(player.getEffects().split("&")).forEach(compound -> playerEntity.addStatusEffect(StatusEffectInstance.fromNbt(NbtCompound.CODEC.parse(wrappedOps, JsonParser.parseString(compound)).resultOrPartial(LOGGER::error).orElseThrow())));
        }
    }

    @Override
    public void sendMessage(MinecraftPlayer player, String string, Colors color) {
        players.get(player.getUuid()).sendMessage(Text.translatable(string).formatted(transformColor(color)));
    }

    @Override
    public void playPlayerSound(MinecraftPlayer player, Sounds sound) {
        ServerPlayerEntity playerEntity = players.get(player.getUuid());
        playerEntity.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(transformSound(sound)), SoundCategory.PLAYERS, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), 1f, 1f, Random.createThreadSafe().nextLong()));
    }

    @Override
    public void clearInventory(MinecraftPlayer player) {
        players.get(player.getUuid()).getInventory().clear();
    }

    @Override
    public void clearEnderChestInventory(MinecraftPlayer player) {
        players.get(player.getUuid()).getEnderChestInventory().clear();
    }

    @Override
    public void clearStatusEffects(MinecraftPlayer player) {
        players.get(player.getUuid()).clearStatusEffects();
    }

    private MinecraftPlayer transformPlayer(ServerPlayerEntity player) {
        if (player == null) return null;

        String uuid = player.getUuidAsString();
        String name = player.getName().getString();
        int air = player.getAir();
        float health = player.getHealth();
        float exhaustion = player.getHungerManager().getExhaustion();
        int foodLevel = player.getHungerManager().getFoodLevel();
        float saturationLevel = player.getHungerManager().getSaturationLevel();
        int foodTickTimer = ((HungerManagerAccessor) player.getHungerManager()).getFoodTickTimer();
        int experienceLevel = player.experienceLevel;
        float experienceProgress = player.experienceProgress;

        EnderChestInventory end = player.getEnderChestInventory();
        StringBuilder endResults = new StringBuilder();
        for (int i = 0; i < end.size(); i++) {
            if (end.getStack(i).isEmpty()) continue;
            endResults.append(ItemStack.CODEC.encodeStart(wrappedOps, end.getStack(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        String enderChestInventory = endResults.toString();

        String off = player.getInventory().offHand.get(0).isEmpty() ? "" : ItemStack.CODEC.encodeStart(wrappedOps, player.getInventory().offHand.get(0)).resultOrPartial(LOGGER::error).orElseThrow().toString();
        int selectedSlot = player.getInventory().selectedSlot;

        DefaultedList<ItemStack> mainInv = player.getInventory().main;
        StringBuilder mainResults = new StringBuilder();
        for (int i = 0; i < mainInv.size(); i++) {
            if (mainInv.get(i).isEmpty()) continue;
            mainResults.append(ItemStack.CODEC.encodeStart(wrappedOps, mainInv.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        String main = mainResults.toString();

        DefaultedList<ItemStack> armorInv = player.getInventory().armor;
        StringBuilder armorResults = new StringBuilder();
        for (int i = 0; i < armorInv.size(); i++) {
            if (armorInv.get(i).isEmpty()) continue;
            armorResults.append(ItemStack.CODEC.encodeStart(wrappedOps, armorInv.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        String armor = armorResults.toString();

        StringBuilder effectResults = new StringBuilder();
        player.getStatusEffects().forEach(effect -> effectResults.append(StatusEffectInstance.CODEC.encodeStart(wrappedOps, effect).resultOrPartial(LOGGER::error).orElseThrow()).append("&"));
        String effects = effectResults.toString();

        players.put(uuid, player);

        return new MinecraftPlayer(uuid, name, air, health, exhaustion, foodLevel, saturationLevel, foodTickTimer, experienceLevel, experienceProgress, enderChestInventory, off, selectedSlot, main, armor, effects);
    }

    private Formatting transformColor(Colors color) {
        return switch (color) {
            case WHITE -> Formatting.WHITE;
            case YELLOW -> Formatting.YELLOW;
            case RED -> Formatting.RED;
            case AQUA -> Formatting.AQUA;
        };
    }

    private SoundEvent transformSound(Sounds sound) {
        return switch (sound) {
            case BLOCK_GLASS_BREAK -> SoundEvents.BLOCK_GLASS_BREAK;
            case ENTITY_PLAYER_LEVELUP -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case BLOCK_ANVIL_DESTROY -> SoundEvents.BLOCK_ANVIL_DESTROY;
        };
    }
}
