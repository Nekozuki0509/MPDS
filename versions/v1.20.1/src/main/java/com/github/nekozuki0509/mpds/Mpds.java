package com.github.nekozuki0509.mpds;

import com.github.nekozuki0509.mpds.common.Common;
import com.github.nekozuki0509.mpds.impl.MinecraftApiImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class Mpds implements ModInitializer {

    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        Common.init(new MinecraftApiImpl());
    }
}
