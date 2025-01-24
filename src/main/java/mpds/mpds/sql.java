package mpds.mpds;

import java.sql.*;

import static mpds.mpds.MPDS.ServerName;
import static mpds.mpds.MPDS.config;

public class sql {

    static Connection connection = null;

    public static String TABLE_NAME;

    public static PreparedStatement disconnect;

    public static PreparedStatement bea;

    public static PreparedStatement befalse;

    public static PreparedStatement checkskip;

    public static PreparedStatement join;

    public static PreparedStatement setserver;

    public static PreparedStatement showskip;

    public static PreparedStatement updateskip;

    public static void init() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        connection = DriverManager.getConnection("jdbc:mysql://" + config.get("HOST") + "/" + config.get("DB_NAME") + "?autoReconnect=true", config.get("USER"), config.get("PASSWD"));

        showskip = connection.prepareStatement("SELECT * FROM skipplayer");

        updateskip = connection.prepareStatement("INSERT INTO skipplayer (Name, skip) VALUES (?, ?) AS new ON DUPLICATE KEY UPDATE Name=new.Name, skip=new.skip");

        checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

        bea = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET server=\"*\" WHERE uuid = ?");

        befalse = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET sync=\"false\" WHERE uuid = ?");

        join = connection.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE uuid = ?");

        checkskip = connection.prepareStatement("SELECT skip FROM skipplayer WHERE Name = ?");

        befalse = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET sync=\"false\" WHERE uuid = ?");

        setserver = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET server=\"" + ServerName + "\" WHERE uuid = ?");

        disconnect = connection.prepareStatement("INSERT INTO " + TABLE_NAME +
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
                ("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
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

    public static void disconnect(sqlPlayer player) throws SQLException {
        disconnect.setString(1, player.name);
        disconnect.setString(2, player.uuid);
        disconnect.setInt(3, player.air);
        disconnect.setFloat(4, player.health);
        disconnect.setString(5, player.enderChestInventory);
        disconnect.setFloat(6, player.exhaustion);
        disconnect.setInt(7, player.foodLevel);
        disconnect.setFloat(8, player.saturationLevel);
        disconnect.setInt(9, player.foodTickTimer);
        disconnect.setString(10, player.main);
        disconnect.setString(11, player.off);
        disconnect.setString(12, player.armor);
        disconnect.setInt(13, player.selectedSlot);
        disconnect.setInt(14, player.experienceLevel);
        disconnect.setFloat(15, player.experienceProgress);
        disconnect.setString(16, player.effects);
        disconnect.executeUpdate();
    }

    public static void close() throws SQLException {
        connection.close();
    }
}
