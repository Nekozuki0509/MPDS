#### Minecraft Player Data Sync(MPDS)
## Description
it's a fabric mod to sync player data between fabric servers. this needs only server side. this mod can sync player's air, health, enderChest, exhaustion, foodLevel, saturationLevel, foodTickTimer, inventory, offhand, armor, selectedSlot, experienceLevel, experienceProgress, and effect. 
## TODO
> [!CAUTION]
> - **you need to build mysql server.**
> - **you need to link fabric servers with proxy server!(like velocity, bungeecord, etc...)**
1. add this mod to server mods folder.
1. restart server.
1. edit config file which is in config/mpdsconfig file.
1. let's play!
## config file
```
HOST=000.000.000.000 #String | default: 000.000.000.000 | comment: it's mysql host ip
DB_NAME=test #String | default: test | comment: it's mysql database name **YOU MUST CREATE THIS DB!!**
TABLE_NAME=test #String | default: test | comment: it's mysql table name(auto create)
USER=test #String | default: test | comment: it's mysql user name
PASSWD=test #String | default: test | comment: it's mysql user's password
```
## Released on modrinth
https://modrinth.com/mod/mpds/
