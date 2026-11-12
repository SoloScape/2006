# Barrows authenticity matrix (revision 443, 2006 target)

This document distinguishes **implemented behavior**, **cache-verified identifiers**, **period-corroborated behavior with unverified numbers**, and **remaining dependencies**. Passing regression tests means the implementation follows its coded contract; it does not prove that every numerical choice matches the original 2006 Jagex server.

## Implemented and regression tested

| Feature | Implementation | Tests / evidence |
| --- | --- | --- |
| Six brothers, sarcophagi, hidden brother, puzzle doors, crypt and chest travel | `BarrowsManager` | `BarrowsBrotherSpecialCombatChecks`, `BarrowsHistoricalPuzzleChecks`, `BarrowsPostLootEscapeChecks` |
| Prayer drain with slain-only apparition selection | `BarrowsPrayerDrainTask` | `BarrowsPrayerDrainChecks`; face order visually unconfirmed |
| Chest rewards delivered into inventory and player-owned ground overflow rather than a later reward popup | `BarrowsManager` | `BarrowsChestRewardChecks` |
| All 24 Barrows equipment pieces degrade for active combat, fully break upon manual drop/death, repair for appropriate NPC charges | `BarrowsEquipmentDurability`, `BarrowsRepairHandler` | `BarrowsEquipmentDurabilityChecks`, `BarrowsItemLossChecks`, `BarrowsRepairChecks` |
| Puzzle choices reroute the centre-door access while preserving an escape route | `BarrowsManager` | `BarrowsPostLootEscapeChecks` |
| Post-loot chest collapse, shake and resetting state upon tunnel exit | `BarrowsChestDamageTask`, `Player`, `BarrowsManager` | `BarrowsPostLootEscapeChecks`, `Barrows443CameraShakePacketChecks` |
| Per-room cap and auto-expiry of temporary door-summoned enemies | `BarrowsTunnelPopulation`, `BarrowsManager` | `BarrowsTunnelEncounterChecks` |
| Void Knights' repair Squire native mapping | `BarrowsRepairHandler`, `data/npcs/extra_spawns.csv` | `BarrowsRepairChecks`; exact NPC spawn tile still provisional |
| Armour stand repair discounts when a native object exists and is reachable | `BarrowsRepairHandler`, `ItemOnObjectTask` | `BarrowsRepairChecks` |

## Native revision-443 cache identifiers

- **Armour repair stand:** `13715`, name confirmed in `ObjectDefinition.loadRevision443()`. Old 317/OSRS object `6802` is *not* that object in this cache.
- **Void Knight smith Squire:** `3797` is the native Squire with Talk-to and no Trade option, corresponding to the old ID `1766`. Old ID `1766` is a **Cow calf** in this cache; `3795` has no Talk-to option or world spawn. New extra NPC placement `(2652,2660,0)` is on a walkable tile near the 443 Outpost, **not yet established as the exact original spawn location**.
- **Squire repair offer:** test item-on-NPC and actual repair dialogue with `3797` in game. A cache NPC name and action alone cannot prove scene/dialogue identity.
- **Book:** original text preserved, but continuation pages on client group 49 are intentionally not necessarily 14 pages one-for-one.

## Numerical fidelity — provisional, not authenticated against Jagex's 2006 source

Do not adjust these to speculative values without contemporary primary evidence or repeatable 2006 capture data.

| System | Current value | Verification outstanding |
| --- | --- | --- |
| Temporary door NPC lifetime | 100 ticks (60 s) | Contemporary July 2006 users report disappearance, but no duration |
| Max live enemies per tunnel room | 11 | Contemporary July 2006 report confirms a cap, not its size; 11 is preserved later value |
| Door encounter weights | 12/128 brother, 52/128 skeleton, 32/128 bloodworm, 32/128 crypt rat | Exact 2006 distribution unknown; later OSRS reference |
| Chest item chance | `1/(450 - 58*k)`, `k = 1..6` | Exact 2006 unique odds unknown; tests validate formula, not history |
| Standard loot threshold table | Full 1012-potential thresholds | Exact 2006 per-item weights unknown; regression checks verify internal consistency |
| Dungeon locked-door layout distribution | Randomized connected spanning tree + one central door | Historical random layout distribution unverified |
| Prayer interval / base drain / apparition animation | 30 ticks / `8 + slain` / graphic animation 2085 | Confirm 2006 cadence and actual six face models in client |
| Collapse interval / maximum hit / graphic | 50 ticks / 5 damage / graphic 60 | Confirm 2006 values, rockfall sound and camera visually |
| Combat animations and projectile flight timing | Current revision-443 definitions | In-game reference capture still required |

### Contemporary evidence

- [Tip.It, 12 July 2006: Monsters leaving in Barrows](https://forum.tip.it/topic/52943-monsters-leaving-in-barrows/) — player reports disappearing tunnel enemies even in combat and a room-specific cap; does not publish numeric values.
- [Tip.It, 22 August 2006: Barrows with Ancient Magicks](https://forum.tip.it/topic/28758-barrows-wancient-magicks-ed-22-aug-2006-aow-discussion/) — escape via ladder while the dungeon collapses.
- [Tip.It, 15 June 2006: Where do I repair Barrows?](https://forum.tip.it/topic/47140-howwhere-do-i-go-to-repair-barrows-items/) — period repair NPC context.

### Later references — **not proof of the exact 2006 math**

- [OSRS wiki: Void Knights' smith](https://oldschool.runescape.wiki/w/Squire_%28Void_Knights_smith%29) — historic release 18 April 2006, older NPC ID 1766.
- [OSRS wiki: Armour stand](https://oldschool.runescape.wiki/w/Armour_stand) — Construction introduction 31 May 2006, 55 Construction, older object ID 6802.
- [OSRS wiki: Barrows armour](https://oldschool.runescape.wiki/w/Barrows_armour) — repair formula and later equipment behavior.

## Remaining cross-feature and audiovisual work

1. **POH Construction and Workshop furniture** are absent from the repository. The stand repair handler works for an existing `13715` object, but nobody can construct a house/workshop/stand normally. Building this properly requires a separate Construction/house instancing, hotspot, materials, save/load and visitor system. Do not put a random non-POH stand into the overworld and call that a 2006 recreation.
2. **Squire placement/interaction**: inspect an authentic 443 Outpost map or in-game capture for the exact smith tile and test item-on-NPC and NPC dialogue.
3. **2006 probability audit**: find period loot datasets or Jagex code for chest/door/route odds before claiming them 1:1; later OSRS values are placeholders.
4. **2006 client screenshot/packet capture**: compare six brother face models, low-detail Prayer graphics, animations, pathing, puzzle text, chest camera/audio, rockfall effects and the book's page breaks.
5. **End-to-end QA matrix**: verify full inventory, exit/re-enter before chest, all 0–6 brother kills, double chest clicks, death/teleport mid-run, disconnected player, room-population churn and NPC ownership.

## Testing

The push-triggered `.github/workflows/verify-barrows-morytania.yml` compiles the Java server with JDK 8 and runs Barrows and associated Morytania regression checks. Use the Actions run for the actual branch commit when reporting CI status.
