package com.github.nekozuki0509.mpds.mixins;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.nekozuki0509.mpds.common.Common.prevent;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void damageMixin(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (prevent.contains(((ServerPlayerEntity) (Object) this).getUuidAsString())) cir.setReturnValue(false);
    }
}
