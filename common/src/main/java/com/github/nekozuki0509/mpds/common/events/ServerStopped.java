package com.github.nekozuki0509.mpds.common.events;

import com.github.nekozuki0509.mpds.common.Sql;

import java.sql.SQLException;

public class ServerStopped {
    public static void onServerStopped() {
        try {
            Sql.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
