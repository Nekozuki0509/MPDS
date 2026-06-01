package com.github.nekozuki0509.mpds.mixins;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerManager.class)
public interface PlayerManagerInvoker {
    @Invoker("savePlayerData")
    void invokesavePlayerData(ServerPlayerEntity player);
}