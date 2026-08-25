package com.github.nekozuki0509.mpds.common;

import com.github.nekozuki0509.mpds.common.minecraft.MinecraftPlayer;

import java.sql.*;

import static com.github.nekozuki0509.mpds.common.Common.config;

public class Sql {

    private static Connection connection = null;

    private static PreparedStatement disconnect;

    private static PreparedStatement bea;

    private static PreparedStatement befalse;

    private static PreparedStatement checkskip;

    private static PreparedStatement join;

    private static PreparedStatement setserver;

    private static PreparedStatement showskip;

    private static PreparedStatement updateskip;

    public static void init() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        connection = DriverManager.getConnection("jdbc:mysql://%s/%s?autoReconnect=true".formatted(config.getHost(), config.getDBName()), config.getUser(), config.getPassword());

        showskip = connection.prepareStatement("SELECT * FROM skipplayer");

        updateskip = connection.prepareStatement("INSERT INTO skipplayer (Name, skip) VALUES (?, ?) AS new ON DUPLICATE KEY UPDATE Name=new.Name, skip=new.skip");

        checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

        bea = connection.prepareStatement("UPDATE %s SET server=\"*\" WHERE uuid = ?".formatted(config.getTableName()));

        befalse = connection.prepareStatement("UPDATE %s SET sync=\"false\" WHERE uuid = ?".formatted(config.getTableName()));

        join = connection.prepareStatement("SELECT * FROM %s WHERE uuid = ?".formatted(config.getTableName()));

        checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

        befalse = connection.prepareStatement("UPDATE %s SET sync=\"false\" WHERE uuid = ?".formatted(config.getTableName()));

        setserver = connection.prepareStatement("UPDATE %s SET server=\"%s\" WHERE uuid = ?".formatted(config.getTableName(), config.getServerName()));

        disconnect = connection.prepareStatement("INSERT INTO %s".formatted(config.getTableName()) +
                " (Name, uuid, Air, Health, enderChestInventory, exhaustion, foodLevel, saturationLevel, foodTickTimer, main, off, armor, selectedSlot, experienceLevel, experienceProgress, effects, sync) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \"true\") AS new " +
                "ON DUPLICATE KEY UPDATE " +
                "Air=new.Air," +
                "Health=new.Health," +
                "enderChestInventory=new.enderChestInventory," +
                "exhaustion=new.exhaustion," +
                "foodLevel=new.foodLevel," +
                "saturationLevel=new.saturationLevel," +
                "foodTickTimer=new.foodTickTimer," +
                "main=new.main," +
                "off=new.off," +
                "armor=new.armor," +
                "selectedSlot=new.selectedSlot," +
                "experienceLevel=new.experienceLevel," +
                "experienceProgress=new.experienceProgress," +
                "effects=new.effects," +
                "sync=new.sync");

        Statement statement = connection.createStatement();

        statement.execute
                ("CREATE TABLE IF NOT EXISTS %s(".formatted(config.getTableName()) +
                        "id int AUTO_INCREMENT PRIMARY KEY," +
                        "Name char(16)," +
                        "uuid char(36) UNIQUE," +
                        "Air int," +
                        "Health float," +
                        "enderChestInventory longtext," +
                        "exhaustion float," +
                        "foodLevel int," +
                        "saturationLevel float," +
                        "foodTickTimer int," +
                        "main longtext," +
                        "off longtext," +
                        "armor longtext," +
                        "selectedSlot int," +
                        "experienceLevel int," +
                        "experienceProgress float," +
                        "effects longtext," +
                        "sync char(5)," +
                        "server text" +
                        ")");

        statement.execute
                ("CREATE TABLE IF NOT EXISTS skipplayer(" +
                        "id int auto_increment PRIMARY KEY," +
                        "Name char(16) UNIQUE," +
                        "skip char(5)" +
                        ")");
    }

    public static ResultSet showSkip() throws SQLException {
        return showskip.executeQuery();
    }

    public static ResultSet checkSkip(String player) throws SQLException {
        checkskip.setString(1, player);
        return checkskip.executeQuery();
    }

    public static void updateSkip(String player, String skip) throws SQLException {
        updateskip.setString(1, player);
        updateskip.setString(2, skip);
        updateskip.executeUpdate();
    }

    public static void beFalse(String uuid) throws SQLException {
        befalse.setString(1, uuid);
        befalse.executeUpdate();
    }

    public static void beA(String uuid) throws SQLException {
        bea.setString(1, uuid);
        bea.executeUpdate();
        beFalse(uuid);
    }

    public static void setServer(String uuid) throws SQLException {
        setserver.setString(1, uuid);
        setserver.executeUpdate();
    }

    public static ResultSet join(String uuid) throws SQLException {
        join.setString(1, uuid);
        return join.executeQuery();
    }

    public static void disconnect(MinecraftPlayer player) throws SQLException {
        disconnect.setString(1, player.getName());
        disconnect.setString(2, player.getUuid());
        disconnect.setInt(3, player.getAir());
        disconnect.setFloat(4, player.getHealth());
        disconnect.setString(5, player.getEnderChestInventory());
        disconnect.setFloat(6, player.getExhaustion());
        disconnect.setInt(7, player.getFoodLevel());
        disconnect.setFloat(8, player.getSaturationLevel());
        disconnect.setInt(9, player.getFoodTickTimer());
        disconnect.setString(10, player.getMain());
        disconnect.setString(11, player.getOff());
        disconnect.setString(12, player.getArmor());
        disconnect.setInt(13, player.getSelectedSlot());
        disconnect.setInt(14, player.getExperienceLevel());
        disconnect.setFloat(15, player.getExperienceProgress());
        disconnect.setString(16, player.getEffects());
        disconnect.executeUpdate();
    }

    public static void close() throws SQLException {
        connection.close();
    }
}