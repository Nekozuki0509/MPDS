[![Modrinth Version](https://img.shields.io/modrinth/v/yJXF9ZSx?logo=modrinth&color=1bd768)![Modrinth Downloads](https://img.shields.io/modrinth/dt/yJXF9ZSx?logo=modrinth&color=1bd768)![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/yJXF9ZSx?logo=modrinth&color=1bd768)](https://modrinth.com/mod/mpds)
[![Discord](https://img.shields.io/discord/1241236305741090836?logo=discord&color=5765f2)](https://discord.gg/352Cdy8MjV)
[![Static Badge](https://img.shields.io/badge/litlink-Nekozuki0509-9594f9)](https://lit.link/nekozuki0509)
[![Static Badge](https://img.shields.io/badge/patreon-Nekozuki0509-red?logo=patreon)](https://patreon.com/Nekozuki0509)

# Minecraft Player Data Sync(MPDS)
## Description
it's a fabric mod to sync player data between fabric servers. this needs only server side. this mod can sync player's air, health, enderChest, exhaustion, foodLevel, saturationLevel, foodTickTimer, inventory, offhand, armor, selectedSlot, experienceLevel, and experienceProgress. 
## TODO
> [!CAUTION]
> - **you need to build mysql server with 8.0.36 or higher.**
> - **you need to link fabric servers with proxy server!(like velocity, bungeecord, etc...)**
1. add this mod to server mods folder.
1. restart server.
1. edit config file which is in config/mpdsconfig file.
1. let's play!
## config file
```
{
  "_Hcomment_" : "it's mysql host ip",
  "HOST" : "000.000.000.000",
  "_Dcomment_" : "it's mysql database name **YOU MUST CREATE THIS DB!!**",
  "DB_NAME" : "minecraft",
  "_Tcomment_" : "it's mysql table name(auto create)",
  "TABLE_NAME" : "player",
  "_Ucomment_" : "it's mysql user name",
  "USER" : "foo",
  "_Pcomment_" : "it's mysql user's password",
  "PASSWD" : "bar",
  "_Scomment_" : "it's this server name",
  "SERVER" : "s",
  "_DJMcomment_" : "disable join message",
  "DJM" : "false",
  "_DSMcomment_" : "disable skip message",
  "DSM" : "false",
  "_DEMcomment_" : "disable error message",
  "DEM" : "false"
}
```
