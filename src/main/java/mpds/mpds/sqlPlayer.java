package mpds.mpds;

import com.google.gson.JsonParser;
import mpds.mpds.mixin.HungerManagerAccessor;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

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
            ((HungerManagerAccessor) player.getHungerManager()).setExhaustion(resultSet.getFloat("exhaustion"));
            player.getHungerManager().setFoodLevel(resultSet.getInt("foodLevel"));
            player.getHungerManager().setSaturationLevel(resultSet.getFloat("saturationLevel"));
            ((HungerManagerAccessor) player.getHungerManager()).setFoodTickTimer(resultSet.getInt("foodTickTimer"));
        }

        if (SL) {
            player.setExperienceLevel(resultSet.getInt("experienceLevel"));
            player.experienceProgress = resultSet.getFloat("experienceProgress");
        }

        if (SEn && !"".equals(resultSet.getString("enderChestInventory")))
            List.of(resultSet.getString("enderChestInventory").split("(?<=~\\d{1,5})&")).forEach(compound -> {
                int lastTilde = compound.lastIndexOf("~");
                if (lastTilde == -1) {
                    LOGGER.error("Malformed enderChestInventory data: {}", compound);
                    return;
                }
                String json = compound.substring(0, lastTilde);
                int index = Integer.parseInt(compound.substring(lastTilde + 1));
                player.getEnderChestInventory().setStack(index, ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(json)).resultOrPartial(LOGGER::error).orElseThrow());
            });

        if (SI) {
            if (!"".equals(resultSet.getString("off")))
                player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(resultSet.getString("off"))).resultOrPartial(LOGGER::error).orElseThrow());
            player.getInventory().setSelectedSlot(resultSet.getInt("selectedSlot"));
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(resultSet.getInt("selectedSlot")));
            if (!"".equals(resultSet.getString("main"))) {
                List.of(resultSet.getString("main").split("(?<=~\\d{1,5})&")).forEach(compound -> {
                    int lastTilde = compound.lastIndexOf("~");
                    if (lastTilde == -1) {
                        LOGGER.error("Malformed main inventory data: {}", compound);
                        return;
                    }
                    String json = compound.substring(0, lastTilde);
                    int index = Integer.parseInt(compound.substring(lastTilde + 1));
                    player.getInventory().setStack(index, ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(json)).resultOrPartial(LOGGER::error).orElseThrow());
                });
            }
            if (!"".equals(resultSet.getString("armor"))) {
                List.of(resultSet.getString("armor").split("(?<=~\\d{1,5})&")).forEach(compound -> {
                    int lastTilde = compound.lastIndexOf("~");
                    if (lastTilde == -1) {
                        LOGGER.error("Malformed armor data: {}", compound);
                        return;
                    }
                    String json = compound.substring(0, lastTilde);
                    int idx = Integer.parseInt(compound.substring(lastTilde + 1));
                    net.minecraft.entity.EquipmentSlot slot = switch (idx) {
                        case 0 -> net.minecraft.entity.EquipmentSlot.FEET;
                        case 1 -> net.minecraft.entity.EquipmentSlot.LEGS;
                        case 2 -> net.minecraft.entity.EquipmentSlot.CHEST;
                        case 3 -> net.minecraft.entity.EquipmentSlot.HEAD;
                        default -> null;
                    };
                    if (slot != null) {
                        player.equipStack(slot, ItemStack.CODEC.parse(wrappedOps, JsonParser.parseString(json)).resultOrPartial(LOGGER::error).orElseThrow());
                    }
                });
            }
        }

        if (SEf && !"".equals(resultSet.getString("effects")))
            List.of(resultSet.getString("effects").split("&")).forEach(compound -> player.addStatusEffect(StatusEffectInstance.CODEC.parse(wrappedOps, JsonParser.parseString(compound)).resultOrPartial(LOGGER::error).orElseThrow()));

    }

    public sqlPlayer(ServerPlayerEntity player, boolean clearData) {
        this.name = player.getName().getString();
        this.uuid = player.getUuidAsString();
        this.air = player.getAir();
        this.health = player.getHealth();
        this.exhaustion = ((HungerManagerAccessor) player.getHungerManager()).getExhaustion();
        this.foodLevel = player.getHungerManager().getFoodLevel();
        this.saturationLevel = player.getHungerManager().getSaturationLevel();
        this.foodTickTimer = ((HungerManagerAccessor) player.getHungerManager()).getFoodTickTimer();
        this.experienceLevel = player.experienceLevel;
        this.experienceProgress = player.experienceProgress;

        EnderChestInventory end = player.getEnderChestInventory();
        StringBuilder endresults = new StringBuilder();
        for (int i = 0; i < end.size(); i++) {
            if (end.getStack(i).isEmpty()) continue;
            endresults.append(ItemStack.CODEC.encodeStart(wrappedOps, end.getStack(i)).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        this.enderChestInventory = endresults.toString();
        if (clearData && SEn) player.getEnderChestInventory().clear();

        this.off = player.getStackInHand(net.minecraft.util.Hand.OFF_HAND).isEmpty() ? "" : ItemStack.CODEC.encodeStart(wrappedOps, player.getStackInHand(net.minecraft.util.Hand.OFF_HAND)).resultOrPartial(LOGGER::error).orElseThrow().toString();
        this.selectedSlot = player.getInventory().getSelectedSlot();

	    // unused; iterate via set/getStack indices 0-35
	    StringBuilder mainresults = new StringBuilder();
        for (int i = 0; i < 36; i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (st.isEmpty()) continue;
            mainresults.append(ItemStack.CODEC.encodeStart(wrappedOps, st).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        this.main = mainresults.toString();

	    // unused; access via get/setArmorStack
	    StringBuilder armorresults = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            net.minecraft.entity.EquipmentSlot slot = switch (i) {
                case 0 -> net.minecraft.entity.EquipmentSlot.FEET;
                case 1 -> net.minecraft.entity.EquipmentSlot.LEGS;
                case 2 -> net.minecraft.entity.EquipmentSlot.CHEST;
                case 3 -> net.minecraft.entity.EquipmentSlot.HEAD;
                default -> null;
            };
            ItemStack ast = slot == null ? ItemStack.EMPTY : player.getEquippedStack(slot);
            if (ast.isEmpty()) continue;
            armorresults.append(ItemStack.CODEC.encodeStart(wrappedOps, ast).resultOrPartial(LOGGER::error).orElseThrow()).append("~").append(i).append("&");
        }
        this.armor = armorresults.toString();

        if (clearData && SI) player.getInventory().clear();

        StringBuilder effectresults = new StringBuilder();
        player.getStatusEffects().forEach(effect -> effectresults.append(StatusEffectInstance.CODEC.encodeStart(wrappedOps, effect).resultOrPartial(LOGGER::error).orElseThrow()).append("&"));
        this.effects = effectresults.toString();
        if (clearData && SEf) player.clearStatusEffects();
    }
}
