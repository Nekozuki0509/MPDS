package com.github.nekozuki0509.common.minecraft;

import lombok.*;

import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
@Getter
@Setter(AccessLevel.PRIVATE)
public class MinecraftPlayer {

    private final String Uuid;

    private final String Name;

    private int Air;

    private float Health;

    private float Exhaustion;

    private int FoodLevel;

    private float SaturationLevel;

    private int FoodTickTimer;

    private int ExperienceLevel;

    private float ExperienceProgress;

    private String EnderChestInventory;

    private String Off;

    private int SelectedSlot;

    private String Main;

    private String Armor;

    private String Effects;

    public MinecraftPlayer(ResultSet resultSet) throws SQLException {
        this.Uuid = resultSet.getString("Uuid");
        this.Name = resultSet.getString("Name");
        this.Air = resultSet.getInt("Air");
        this.Health = resultSet.getFloat("Health");
        this.Exhaustion = resultSet.getFloat("exhaustion");
        this.FoodLevel = resultSet.getInt("foodLevel");
        this.SaturationLevel = resultSet.getFloat("saturationLevel");
        this.FoodTickTimer = resultSet.getInt("foodTickTimer");
        this.ExperienceLevel = resultSet.getInt("experienceLevel");
        this.ExperienceProgress = resultSet.getFloat("experienceProgress");
        this.EnderChestInventory = resultSet.getString("enderChestInventory");
        this.Off = resultSet.getString("off");
        this.SelectedSlot = resultSet.getInt("selectedSlot");
        this.Main = resultSet.getString("main");
        this.Armor = resultSet.getString("armor");
        this.Effects = resultSet.getString("effects");
    }

    public MinecraftPlayer(String uuid, String name, int air, float health, float exhaustion, int foodLevel, float saturationLevel, int foodTickTimer, int experienceLevel, float experienceProgress, String enderChestInventory, String off, int selectedSlot, String main, String armor, String effects) {
        this.Uuid = uuid;
        this.Name = name;
        this.Air = air;
        this.Health = health;
        this.Exhaustion = exhaustion;
        this.FoodLevel = foodLevel;
        this.SaturationLevel = saturationLevel;
        this.FoodTickTimer = foodTickTimer;
        this.ExperienceLevel = experienceLevel;
        this.ExperienceProgress = experienceProgress;
        this.EnderChestInventory = enderChestInventory;
        this.Off = off;
        this.SelectedSlot = selectedSlot;
        this.Main = main;
        this.Armor = armor;
        this.Effects = effects;
    }
}
