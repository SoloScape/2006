# SoloScape 2006

**Play a self-hosted, single-player RuneScape world inspired by 18 December 2006.**

SoloScape combines a Java game client, a local game server and a revision **443** cache. Explore the world, develop a character and try the systems already implemented — all on your own computer.

> **Work in progress:** This is not a fully complete or historically verified recreation of RuneScape in 2006. Some quests, skills and interactions are partial or untested. See the [roadmap](#roadmap) for the tracking list.

[Quick start](#quick-start) · [Features](#current-working-features) · [Roadmap](#roadmap) · [Browser & phone](#browser--phone-play) · [Development](#development) · [Help](#troubleshooting)

---

## Quick start

Download the [latest source ZIP](https://github.com/SoloScape/SoloScape-2006/archive/refs/heads/main.zip) and extract it, or clone the repository. **Keep the `Client/`, `Server/` and `tools/` folders together.**

### Windows

1. Open the extracted SoloScape folder.
2. Double-click **`Start-Here-SoloScape.bat`**.
3. Choose **`4 — Play SoloScape`** to build and start the server and client.
4. Log in to the local client with the credentials for your character.

**For later launches:** Option `1` builds the game and generates `Start-Server.bat` and `Play-Client.bat` in the repository root. You can use those separately: start the server from its control panel, then open the client.

### Linux

From the repository root, in a graphical desktop session:

```bash
bash Start-Here-SoloScape.sh run
```

This builds and starts the server and client. To use the interactive menu instead, run `bash Start-Here-SoloScape.sh`. See the [Linux guide](docs/LINUX.md) for supported architectures, requirements and separate launcher commands.

### Requirements

- **Windows or Linux** with a desktop environment for the Java game.
- **Java 8 JDK**, not just a JRE or a newer JDK. The launchers can set up Eclipse Temurin Java 8 for you if needed (an internet connection is required for the first download).
- **Linux automatic Java setup** additionally needs Python 3 and `curl` or `wget`.
- Enough available memory to run both the server and client.

Keep the server running while playing. The game connects to the local server on port `43594` by default.

## Current working features

The repository contains implementations and tooling for the following areas. **Presence in the code does not guarantee that every action works or matches December 2006.**

| Area | Included in the project |
| :--- | :--- |
| **Client & world** | Java game client, revision 443 cache integration, movement, interfaces, NPCs and world interaction systems |
| **Gameplay** | Skill, combat, quest and activity frameworks, with varying degrees of coverage |
| **World systems** | Shops, trading, NPC behaviour and configurable simulated players |
| **Characters** | JSON character saves, legacy-save migration, backups and optional SQLite support |
| **Extras** | Browser/phone streaming, developer inspection tools, regression checks and 2006 Christmas event code |

The [server guide](Server/README.md) describes the implementation in more detail. The [roadmap](#roadmap) is a content checklist, not a guarantee of playability.

## Roadmap

<!-- roadmap-progress:start -->
**Overall completion: 8.3%** · **16 / 193** counted roadmap items marked ✅

![Roadmap completion: 8.3% — 16 of 193 items](docs/roadmap-progress.svg)
<!-- roadmap-progress:end -->

Progress counts ✅ entries across all roadmap categories, including optional plugins; ➖ entries are excluded. The percentage and bar refresh automatically after roadmap changes on `main` via GitHub Actions. This tracks README statuses, not independently verified gameplay.

The goal is the **18 December 2006 / revision 443** experience. Open a category below to see its items and their current tracker statuses.

**Status key:** 
✅ Complete (Accurate as possible)
⛔ Partially Working
📜 Needs Audit

<details>
<summary><strong>Skills & combat</strong></summary>

| Name | Status |
| :--- | :---: |
| Woodcutting | 📜 |
| Mining | 📜 |
| Agility | 📜 |
| Smithing | 📜 |
| Herblore | 📜 |
| Fishing | 📜 |
| Thieving | 📜 |
| Cooking | 📜 |
| Crafting | 📜 |
| Firemaking | 📜 |
| Fletching | 📜 |
| Runecrafting | 📜 |
| Slayer | 📜 |
| Farming | 📜 |
| Construction | 📜 |
| Hunter | 📜 |
| Prayer | 📜 |
| Magic | 📜 |
| Ranged | 📜 |
| Attack | 📜 |
| Strength | 📜 |
| Defence | 📜 |
| Hitpoints | 📜 |

</details>

<details>
<summary><strong>Bosses & major encounters</strong></summary>

| Name | Status |
| :--- | :---: |
| King Black Dragon | 📜 |
| Kalphite Queen | 📜 |
| Barrows brothers | 📜 |
| TzTok-Jad (Fight Caves) | 📜 |
| Dagannoth Kings | 📜 |
| Chaos Elemental | 📜 |
| Giant Mole / Wyson rewards | 📜 |

</details>

<details>
<summary><strong>Free-to-play quests</strong></summary>

| Name | Status |
| :--- | :---: |
| Black Knights' Fortress | 📜 |
| Cook's Assistant | 📜 |
| Demon Slayer | 📜 |
| Doric's Quest | 📜 |
| Dragon Slayer | 📜 |
| Ernest the Chicken | 📜 |
| Goblin Diplomacy | 📜 |
| Imp Catcher | 📜 |
| The Knight's Sword | 📜 |
| Pirate's Treasure | 📜 |
| Prince Ali Rescue | 📜 |
| The Restless Ghost | 📜 |
| Romeo & Juliet | 📜 |
| Rune Mysteries | 📜 |
| Sheep Shearer | 📜 |
| Shield of Arrav | 📜 |
| Vampire Slayer | 📜 |
| Witch's Potion | 📜 |

</details>

<details>
<summary><strong>Members' quests</strong></summary>

| Name | Status |
| :--- | :---: |
| Animal Magnetism | 📜 |
| Between a Rock... | 📜 |
| Big Chompy Bird Hunting | 📜 |
| Biohazard | 📜 |
| Cabin Fever | 📜 |
| Clock Tower | ✅ |
| Creature of Fenkenstrain | 📜 |
| Darkness of Hallowvale | 📜 |
| Death Plateau | 📜 |
| Death to the Dorgeshuun | 📜 |
| Desert Treasure | 📜 |
| Devious Minds | 📜 |
| The Dig Site | 📜 |
| Druidic Ritual | 📜 |
| Dwarf Cannon | 📜 |
| Eadgar's Ruse | 📜 |
| Eagles' Peak | 📜 |
| Elemental Workshop | 📜 |
| Elemental Workshop II | 📜 |
| Enakhra's Lament | 📜 |
| Enlightened Journey | 📜 |
| The Eyes of Glouphrie | 📜 |
| Fairy Tale I — Growing Pains | 📜 |
| Fairy Tale II — Cure a Queen | 📜 |
| Family Crest | 📜 |
| The Feud | 📜 |
| Fight Arena | 📜 |
| Fishing Contest | 📜 |
| Forgettable Tale of a Drunken Dwarf | 📜 |
| The Fremennik Trials | 📜 |
| Garden of Tranquillity | 📜 |
| Gertrude's Cat | 📜 |
| Ghosts Ahoy | 📜 |
| The Giant Dwarf | 📜 |
| The Golem | 📜 |
| The Grand Tree | 📜 |
| The Hand in the Sand | 📜 |
| Haunted Mine | 📜 |
| Hazeel Cult | 📜 |
| Heroes' Quest | 📜 |
| Holy Grail | 📜 |
| Horror from the Deep | 📜 |
| Icthlarin's Little Helper | 📜 |
| In Aid of the Myreque | 📜 |
| In Search of the Myreque | 📜 |
| Jungle Potion | 📜 |
| Legends' Quest | 📜 |
| Lost City | 📜 |
| The Lost Tribe | 📜 |
| Lunar Diplomacy | 📜 |
| Making History | 📜 |
| Merlin's Crystal | 📜 |
| Monk's Friend | ✅ |
| Monkey Madness | 📜 |
| Mountain Daughter | 📜 |
| Mourning's End Part I | 📜 |
| Mourning's End Part II | 📜 |
| Murder Mystery | 📜 |
| My Arm's Big Adventure | 📜 |
| Nature Spirit | 📜 |
| Observatory Quest | 📜 |
| One Small Favour | 📜 |
| Plague City | ✅ |
| Priest in Peril | ✅ |
| Rag and Bone Man | 📜 |
| Ratcatchers | 📜 |
| Recipe for Disaster | 📜 |
| Recruitment Drive | 📜 |
| Regicide | 📜 |
| Roving Elves | 📜 |
| Royal Trouble | 📜 |
| Rum Deal | 📜 |
| Scorpion Catcher | 📜 |
| Sea Slug | 📜 |
| Shades of Mort'ton | 📜 |
| Shadow of the Storm | 📜 |
| Sheep Herder | ✅ |
| Shilo Village | 📜 |
| The Slug Menace | 📜 |
| A Soul's Bane | 📜 |
| Spirits of the Elid | 📜 |
| Swan Song | 📜 |
| Tai Bwo Wannai Trio | 📜 |
| A Tail of Two Cats | 📜 |
| Tears of Guthix | 📜 |
| Temple of Ikov | 📜 |
| Throne of Miscellania | 📜 |
| The Tourist Trap | 📜 |
| Tree Gnome Village | 📜 |
| Tribal Totem | 📜 |
| Troll Romance | 📜 |
| Troll Stronghold | 📜 |
| Underground Pass | 📜 |
| Wanted! | 📜 |
| Watchtower | 📜 |
| Waterfall Quest | 📜 |
| Witch's House | 📜 |
| Zogre Flesh Eaters | 📜 |

</details>

<details>
<summary><strong>Minigames & activities</strong></summary>

| Name | Status |
| :--- | :---: |
| Barrows | ✅ |
| Castle Wars | ✅ |
| Duel Arena | 📜 |
| TzHaar Fight Caves | 📜 |
| Mage Training Arena | 📜 |
| Tai Bwo Wannai Cleanup | 📜 |
| Falador Party Room | 📜 |
| Pest Control | 📜 |
| TzHaar Fight Pits | 📜 |
| Pyramid Plunder | 📜 |
| Rogues' Den | 📜 |
| Trouble Brewing | 📜 |
| Temple Trekking | 📜 |
| Blast Furnace | 📜 |
| Fishing Trawler | 📜 |
| Rat Pits | 📜 |
| Stronghold of Security | 📜 |
| Warriors' Guild | 📜 |
| Brimhaven Agility Arena | 📜 |
| Gnome Restaurant | 📜 |

</details>

<details>
<summary><strong>Combat, world & economy</strong></summary>

| Name | Status |
| :--- | :---: |
| Melee / Ranged / Magic combat | 📜 |
| PvP / Wilderness combat | 📜 |
| Prayer & special attacks | 📜 |
| NPC combat and drops | 📜 |
| Player trading | 📜 |
| Banking & equipment | 📜 |
| Shops & prices | 📜 |
| Treasure Trails / clue scrolls | 📜 |
| Random events | 📜 |
| Slayer masters & tasks | 📜 |
| NPC dialogue & world interactions | 📜 |
| Quest progression & rewards | 📜 |
| Movement, teleports & travel | 📜 |
| 2006 Christmas rescue | 📜 |
| Fairy rings | 📜 |
| Player-owned houses (POH) | 📜 |
| 2006 skill tutors | 📜 |
| Capes of Accomplishment | 📜 |
| Modern bank tabs (released 2008) | ➖ |
| God Wars Dungeon (released 2007) | ➖ |
| Modern Ironman modes | ➖ |

</details>

<details>
<summary><strong>Plugins</strong></summary>

| Name | Status |
| :--- | :---: |
| Grand Exchange (released 2007) | ✅ |

</details>

<details>
<summary><strong>Client, tools & supporting systems</strong></summary>

| Name | Status |
| :--- | :---: |
| Java 8 setup and Windows launchers | ✅ |
| Client / server revision 443 support | ✅ |
| Character JSON saves & migration | ✅ |
| Optional SQLite save backend | ✅ |
| World simulation & bot AI | ✅ |
| Web / mobile client streaming | ✅ |
| Client developer tools / MCP | ✅ |
| Automated content audits & checks | ✅ |

</details>

## Browser & phone play

SoloScape can stream the **actual Java client** into a browser. This is not a standalone JavaScript version: the host computer still renders and runs the game.

1. Start the game server.
2. On **Windows**, run `Client/WebClient/Play-In-Browser-Or-Phone.bat`; on **Linux**, run `bash Client/WebClient/Play-In-Browser-Or-Phone.sh`.
3. Open [http://127.0.0.1:8081/](http://127.0.0.1:8081/) on the host, or use the LAN address printed by the launcher for a phone on the same network.

Keep the host and browser bridge running. **Do not expose port `8081` directly to the public internet**; use a private network or secure tunnel for remote access.

[Browser client instructions](Client/WebClient/README.md)

## Development

Builds use **Java 8** and the project's bundled dependencies rather than a Gradle or Maven setup.

| Task | Windows | Linux |
| :--- | :--- | :--- |
| Build both | `Start-Here-SoloScape.bat build` | `bash Start-Here-SoloScape.sh build` |
| Build client | `Start-Here-SoloScape.bat build client` | `bash Start-Here-SoloScape.sh build client` |
| Build server | `Start-Here-SoloScape.bat build server` | `bash Start-Here-SoloScape.sh build server` |
| Run game | `Start-Here-SoloScape.bat run` | `bash Start-Here-SoloScape.sh run` |
| Developer mode | `Start-Here-SoloScape.bat dev` | `bash Start-Here-SoloScape.sh dev` |
| Clean build files | `Start-Here-SoloScape.bat clean` | `bash Start-Here-SoloScape.sh clean` |

On Windows, `tools/scape.ps1` also accepts the same build, run, dev, clean and update commands. Build outputs are placed in `Server/dist/` and `Client/build/`.

Developer mode provides source watching and client inspection. The local developer dashboard is at [http://127.0.0.1:7780/](http://127.0.0.1:7780/) while the developer tools are running; it is **not** a public web service.

For detailed commands and diagnostics, see [client developer tools](docs/Client/DEVELOPER-TOOLS.md), [server documentation](Server/README.md) and [Linux setup](docs/LINUX.md).

## Character saves

Default character files are stored under `Server/data/characters/`. The server can migrate older `.dat` characters to JSON, and an optional SQLite backend is available.

**Back up your saves before upgrading or editing them, and stop the server before manual edits.** Character files may contain private account information; do not commit or share them publicly.

[Character save and recovery guide](docs/Server/CHARACTER-SAVES.md)

## Troubleshooting

<details>
<summary><strong>I can't find the Windows play launchers</strong></summary>

Run `Start-Here-SoloScape.bat` and choose option `1` to build the game. `Start-Server.bat` and `Play-Client.bat` are generated by the Windows build. On Linux, use the checked-in `.sh` launchers instead.

</details>

<details>
<summary><strong>Java or javac can't be found</strong></summary>

The project requires a **Java 8 JDK**. Re-run the setup with an internet connection, or set `JAVA_HOME` to an existing Java 8 JDK. For Linux dependency details, see the [Linux guide](docs/LINUX.md).

</details>

<details>
<summary><strong>The client can't connect or load the world</strong></summary>

Make sure the server is running and online, and keep the original folder layout. The client needs the cache under `Server/cache/`. The default server port is `43594`.

</details>

<details>
<summary><strong>My code changes aren't showing up</strong></summary>

Rebuild the changed component or use developer mode. Some Java changes require a restart instead of a live reload.

</details>

<details>
<summary><strong>The browser version won't open on my phone</strong></summary>

Keep the browser bridge running and use the **LAN URL printed in its terminal**, not the host's `127.0.0.1` address. Both devices must be able to reach each other on the same private network.

</details>

## Documentation

| Guide | What it covers |
| :--- | :--- |
| [Server](Server/README.md) | World setup, server configuration and content systems |
| [Linux](docs/LINUX.md) | Linux launcher usage and platform-specific requirements |
| [Browser client](Client/WebClient/README.md) | Playing through a browser or phone |
| [Developer tools](docs/Client/DEVELOPER-TOOLS.md) | Local client inspection and debugging |
| [Character saves](docs/Server/CHARACTER-SAVES.md) | Save migration, backup and recovery |
| [Historical audit](docs/AUDIT_2006-12-18.md) | Earlier revision 443 findings; **not** a live report of current completion |

## Contributing

Bug reports and pull requests are welcome. Include reproduction steps, expected behaviour and any testing performed. Contributions toward the December 2006 target are especially useful.

## Credits & legal

SoloScape is an **unofficial fan project**. It is not affiliated with or endorsed by Jagex. RuneScape names, artwork, assets and trademarks belong to their respective owners.

The server includes third-party dependencies and code adapted in part from the RS Mod pathfinder; see the [third-party notices](Server/README.md#third-party-notices).

**Licensing:** The repository currently has no repository-wide `LICENSE` file. Public source availability does not automatically grant redistribution rights for the code, libraries or game assets.
