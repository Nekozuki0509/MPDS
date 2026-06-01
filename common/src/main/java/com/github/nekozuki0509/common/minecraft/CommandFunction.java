package com.github.nekozuki0509.common.minecraft;

@FunctionalInterface
public interface CommandFunction {
    int execute(ArgumentGetter getter, MinecraftPlayer player);
}
