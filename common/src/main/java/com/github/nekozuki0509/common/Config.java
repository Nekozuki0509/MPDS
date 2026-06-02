package com.github.nekozuki0509.common;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.github.nekozuki0509.common.Common.api;

@Getter
@NoArgsConstructor
public class Config {

    private String Host;

    private String DBName;

    private String TableName;

    private String User;

    private String Password;

    private String ServerName;

    private boolean AJM;

    private boolean ASM;

    private boolean AEM;

    private boolean SA;

    private boolean SH;

    private boolean SF;

    private boolean SL;

    private boolean SEn;

    private boolean SI;

    private boolean SEf;

    public static Config init() {
        Path configDir = api.getConfigDir().resolve("MPDS");
        Path configjson = configDir.resolve("mpdsConfig.json");

        if (Files.notExists(configDir)) {
            try {
                Files.createDirectory(configDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        if (Files.notExists(configjson)) {
            try {
                Files.copy(Objects.requireNonNull(Common.class.getResourceAsStream("/mpdsConfig.json")), configjson);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("made MPDS config file.\nPlease set!");
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(String.valueOf(configjson)), StandardCharsets.UTF_8))) {
            return (new Gson()).fromJson(reader, Config.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
