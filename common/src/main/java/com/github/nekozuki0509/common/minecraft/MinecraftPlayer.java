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
}
