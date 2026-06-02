package com.github.nekozuki0509.mpds.impl;

import com.github.nekozuki0509.common.minecraft.ArgumentGetter;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

public class ArgumentGetterImpl implements ArgumentGetter {
    private final CommandContext<?> ctx;

    public ArgumentGetterImpl(CommandContext<?> ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getString(String argument) {
        return StringArgumentType.getString(ctx, argument);
    }

    @Override
    public boolean getBool(String argument) {
        return BoolArgumentType.getBool(ctx, argument);
    }
}
