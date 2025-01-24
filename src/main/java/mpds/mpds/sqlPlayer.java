package mpds.mpds;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import mpds.mpds.mixin.HungerManagerAccessor;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static mpds.mpds.MPDS.*;

public class sqlPlayer {

    public String uuid;

    public String name;

    public int air;

    public float health;

    public float exhaustion;

    public int foodLevel;

    public float saturationLevel;

    public int foodTickTimer;

    public int experienceLevel;

    public float experienceProgress;

    public String enderChestInventory;

    public String off;

    public int selectedSlot;

    public String main;

    public String armor;

    public String effects;

    public static void sqlToPlayer(ServerPlayerEntity player, ResultSet resultSet) throws SQLException {
        if (SA) player.setAir(resultSet.getInt("Air"));

        if (SH) player.setHealth(resultSet.getFloat("Health"));

        if (SF) {
            player.getHungerManager().setExhaustion(resultSet.getFloat("exhaustion"));
            player.getHungerManager().setFoodLevel(resultSet.getInt("foodLevel"));
            player.getHungerManager().setSaturationLevel(resultSet.getFloat("saturationLevel"));
            ((HungerManagerAccessor) player.getHungerManager()).setFoodTickTimer(resultSet.getInt("foodTickTimer"));
        }

        if (SL) {
            player.setExperienceLevel(resultSet.getInt("experienceLevel"));
            player.experienceProgress = resultSet.getFloat("experienceProgress");
        }

        if (SEn && !"".equals(resultSet.getString("enderChestInventory")))
            List.of(resultSet.getString("enderChestInventory").split("&")).forEach(compound -> {
                String[] compounds = compound.split("~");
                player.getEnderChestInventory().setStack(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
            });

        if (SI) {
            if (!"".equals(resultSet.getString("off")))
                player.getInventory().offHand.set(0, ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(resultSet.getString("off"))).resultOrPartial(LOGGER::error).orElseThrow());
            player.getInventory().selectedSlot = resultSet.getInt("selectedSlot");
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(resultSet.getInt("selectedSlot")));
            if (!"".equals(resultSet.getString("main"))) {
                List.of(resultSet.getString("main").split("&")).forEach(compound -> {
                    String[] compounds = compound.split("~");
                    player.getInventory().main.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                });
            }
            if (!"".equals(resultSet.getString("armor"))) {
                List.of(resultSet.getString("armor").split("&")).forEach(compound -> {
                    String[] compounds = compound.split("~");
                    player.getInventory().armor.set(Integer.parseInt(compounds[1]), ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compounds[0])).resultOrPartial(LOGGER::error).orElseThrow());
                });
            }
        }

        if (SEf && !"".equals(resultSet.getString("effects")))
            List.of(resultSet.getString("effects").split("&")).forEach(compound -> player.addStatusEffect(StatusEffectInstance.fromNbt(NbtCompound.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(compound)).resultOrPartial(LOGGER::error).orElseThrow())));

    }

    public sqlPlayer(ServerPlayerEntity player) {
        this.name = player.getName().getString();
        this.uuid = player.getUuidAsString();
        this.air = player.getAir();
        this.health = player.getHealth();
        this.exhaustion = player.getHungerManager().getExhaustion();
        this.foodLevel = player.getHungerManager().getFoodLevel();
        this.saturationLevel = player.getHungerManager().getSaturationLevel();
        this.foodTickTimer = ((HungerManagerAccessor) player.getHungerManager()).getFoodTickTimer();
        this.experienceLevel = player.experienceLevel;
        this.experienceProgress = player.experienceProgress;

        EnderChestInventory end = player.getEnderChestInventory();
        StringBuilder endresults = new StringBuilder();
        for (int i = 0; i < end.size(); i++) {
            if (end.getStack(i).isEmpty()) continue;
            endresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, end.getStack(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        this.enderChestInventory = endresults.toString();
        if (SEn) player.getEnderChestInventory().clear();

        this.off = player.getInventory().offHand.get(0).isEmpty() ? "" : ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, player.getInventory().offHand.get(0)).resultOrPartial(LOGGER::error).orElseThrow().toString();
        this.selectedSlot = player.getInventory().selectedSlot;

        DefaultedList<ItemStack> main = player.getInventory().main;
        StringBuilder mainresults = new StringBuilder();
        for (int i = 0; i < main.size(); i++) {
            if (main.get(i).isEmpty()) continue;
            mainresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, main.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        this.main = mainresults.toString();

        DefaultedList<ItemStack> armor = player.getInventory().armor;
        StringBuilder armorresults = new StringBuilder();
        for (int i = 0; i < armor.size(); i++) {
            if (armor.get(i).isEmpty()) continue;
            armorresults.append(ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, armor.get(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        this.armor = armorresults.toString();

        if (SI) player.getInventory().clear();

        StringBuilder effectresults = new StringBuilder();
        player.getStatusEffects().forEach(effect -> effectresults.append(NbtCompound.CODEC.encodeStart(JsonOps.INSTANCE, effect.writeNbt(new NbtCompound())).resultOrPartial(LOGGER::error).orElseThrow()).append("&"));
        this.effects = effectresults.toString();
        if (SEf) player.clearStatusEffects();
    }
}
