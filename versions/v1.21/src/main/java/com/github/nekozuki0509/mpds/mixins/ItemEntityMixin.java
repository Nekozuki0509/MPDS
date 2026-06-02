package com.github.nekozuki0509.mpds.mixins;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.nekozuki0509.common.Common.prevent;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void onPlayerCollisionMixin(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity && prevent.contains(player.getUuidAsString())) ci.cancel();
    }
}
