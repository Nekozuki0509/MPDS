package com.github.nekozuki0509.mpds.common.minecraft;

@FunctionalInterface
public interface CommandFunction {
    int execute(ArgumentGetter getter, MinecraftPlayer player);
}
