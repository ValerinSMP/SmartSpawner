<div align="center">
  
![SmartSpawner banner](docs/public/banner.png)

[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/smartspawner)
[![Spigot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/spigot_vector.svg)](https://www.spigotmc.org/resources/120743/)
[![Hangar](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg)](https://hangar.papermc.io/Nighter/SmartSpawner)

[![Documentation](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/ghpages_vector.svg)](https://docs.smartspawner.site/)
[![discord-plural](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_46h.png)](http://discord.com/invite/FJN7hJKPyb)

</div>

## Requirements

- **Minecraft Version:** 1.21.6 - 26.2
- **Server Software:** Paper, Folia or compatible forks
- **Java Version:** 25+

## Localization

| Language           | Locale Code | Contributor                                     | Status |
|--------------------|-------------|-------------------------------------------------|--------|
| 🇨🇳 Chinese Simplified | `zh_CN`     | [SnowCutieOwO](https://github.com/SnowCutieOwO) | v1.2.3 (Outdated) |
| 🇩🇪 German             | `de_DE`     | [jannispkz](https://github.com/jannispkz)       | v1.6.7 (Outdated) |
| 🇺🇸 English            | `en_US`     | [Nighter](https://github.com/n1ght3r)           | Latest |
| 🇮🇹 Italian            | `it_IT`     | [RV_SkeLe](https://github.com/RVSkeLe)          | v1.3.5 (Outdated) |
| 🇹🇷 Turkish            | `tr_TR`     | berkkorkmaz, [onurrrk](https://github.com/onurrrk) | Latest |
| 🇻🇳 Vietnamese         | `vi_VN`     | [Nighter](https://github.com/n1ght3r)           | Latest |

## API

For developers interested in integrating with SmartSpawner, visit the [Developer API Documentation](https://docs.smartspawner.site/developer-api/).

## Building

```bash
git clone https://github.com/OpenVdra/SmartSpawner.git
cd SmartSpawner
./gradlew :core:shadowJar
```

On Windows, use `gradlew.bat :core:shadowJar`. The compiled plugin JAR will be available in `core/build/libs/` as `SmartSpawner-<version>.jar`.

## Support

- **Issues & Bug Reports:** [GitHub Issues](https://github.com/OpenVdra/smartspawner/issues)
- **Discord Community:** [Join our Discord](https://dsc.gg/openvdra)

## Statistics

[![bStats](https://bstats.org/signatures/bukkit/SmartSpawner.svg)](https://bstats.org/plugin/bukkit/SmartSpawner)

## License

This project is licensed under the GPLv3 License - see the [LICENSE](LICENSE) file for details.

## Valerin development contract

[Production ownership and unit API draft](docs/valerin/PRODUCTION-OWNERSHIP-API-V1.md).
D1 contains offline simulation and a draft interface; no recycling provider or production limits are active.

[Valerin D3 Test delivery, migration and rollback](docs/valerin/DELIVERY-D3-PRODUCTION-20261011.md).

[Automatic migration and player /ss mine delivery](docs/valerin/DELIVERY-D4-AUTOMIGRATION-MINE-20261011.md).

Current Valerin delivery: production.8 is staged on Custom for the owner's daily restart.
Fresh blocks belong to their placer; adding to someone else's stack is rejected. Overlapping
protection roots share one durable farm budget. Existing confirmed owners remain unchanged.
[Overlap/placer migration, validation and rollback](docs/valerin/DELIVERY-D6-OVERLAP-PLACER-20261011.md).

Production caps now apply independently to each mob/item type: 100 per farm/type and per33x33window/type.
[Per-type correction and delivery](docs/valerin/DELIVERY-D7-PER-TYPE-20261011.md).

/ss mine now shows placed/producing counts and private status/type filters, sorting and pagination.
[Spanish menu, notices and rollback](docs/valerin/DELIVERY-D8-MINE-SPANISH-20261011.md).

Held/retired spawner items have no production owner: old owner/farm tags are ignored and new drops
remove them. Placed owners remain durable; held units can be added to your own stack.
[Unplaced item ownership and stored-stack correction](docs/valerin/DELIVERY-D9-UNPLACED-NO-OWNER-20261011.md).

Admins can inspect /ss mine <player-or-UUID> [page] with smartspawner.command.mine.others
(default op), including registered offline users, without management actions.
[Admin inspection, permissions and rollback](docs/valerin/DELIVERY-D10-MINE-ADMIN-20261011.md).
