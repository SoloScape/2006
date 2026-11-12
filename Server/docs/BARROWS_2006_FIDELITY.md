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
| Fresh-account hidden brother randomisation with unfinished-run save retention | `Player`, `CharacterFileManager` | `Barrows2006RestorationChecks` real JSON save/reload |
| Two-player private owned-NPC combat and kill credit while fixed tunnel spawns stay public | `CombatCycleEvent`, `BarrowsManager`, `Npc` | `Barrows2006RestorationChecks` |
| Private chest config and clickable client-morphed appearance, never global chest/collision | `BarrowsManager`, `FirstObjectActionTask` | `Barrows2006RestorationChecks`; native 443 reconnect varps |
| Members-world Barrows entry, spell-brother Weaken, and broken-0% direct drop | `BarrowsManager`, `AhrimCombatDefinition`, `ItemActionPacketHandler` | Native 443 build; `Barrows2006RestorationChecks` |
| Original Strange Old Man, Squire Talk-to with their native NPC IDs; no later diary/quest rewards | `BarrowsNpcDialogues`, `DialogueManager`, `ExtraNpcSpawns` | `Barrows2006RestorationChecks`; repeatable scream, secret and knock-knock dialogue paths tested; period wording not completely verified |
| 9 January 2014 spade spawns removed, original crumbling-tome spawn retained | `data/world/Item spawn.dat` | `Barrows2006RestorationChecks`; 1261 → 1259 records |
| All 14 original tome pages preserved as words and distinct boundaries on the native 443 book | `BarrowsTomeHandler` | `BarrowsTomeAuthenticityChecks`; rendering not pixel-identical |

## Native revision-443 cache identifiers

- **Armour repair stand:** `13715`, name confirmed in `ObjectDefinition.loadRevision443()`. Old 317/OSRS object `6802` is *not* that object in this cache.
- **Void Knight smith Squire:** `3797` is the native Squire with Talk-to and no Trade option, corresponding to the old ID `1766`. Old ID `1766` is a **Cow calf** in this cache; `3795` has no Talk-to option or world spawn. New extra NPC placement `(2652,2660,0)` is on a walkable tile near the 443 Outpost, **not yet established as the exact original spawn location**.
- **Squire repair offer:** item-on-NPC and Talk-to routes are implemented for `3797` and covered in regressions. The exact dialogue wording and precise NPC tile remain **not verified from a 2006 capture**. This is not yet identical to original text.
- **Strange Old Man:** native `2024` is cache-verified as “Strange Old Man” with Talk-to; `1671` is undefined in revision 443. Additional spawn `(3566,3290,0)` has been collision-tested, but its exact historical tile is provisional. Secret, knock-knock, book-giving and repeatable scream branches now have distinct, regression-tested dialogue continuations. Without a spade, the NPC still gives generic advice, but **does not** falsely claim the post-2006 spade spawns exist at each mound. The preserved conversation branches are from a later transcript ([Darkan, last edited November 2012](https://wiki.darkan.org/Transcript:Strange_Old_Man)), not a verified verbatim 2006 recording. Branch selection odds and precise tome-giving frequency remain provisional.
- **World item spawns:** original crumbling tome `4707` at `(3571,3312,0)` preserved. Two spades at `(3571,3310,0)` and `(3572,3312,0)`, introduced on 9 January 2014, were removed without affecting other item IDs/amounts.
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
2. **Historical NPC precision**: the smith Squire and Strange Old Man spawn on collision-safe 443 tiles, and their interactions function, but exact original 2006 coordinates and word-for-word dialogue require a dated period map or transcript/recording. Do not label reconstructed dialogue proven verbatim.
3. **2006 probability audit**: the current chest rolls (1/392 → 1/102 per equipment roll), 1012-point threshold table, encounter weights and dungeon route generation are exhaustively code-tested, but 2006-specific original source probabilities are still absent. Current OSRS data alone is **not** adequate provenance for exact 2006 values; retain the tested preserved behaviour rather than changing constants speculatively.
4. **Client audiovisual fidelity**: the exact original 443 game effects for six face models, low-detail Prayer handling, emergence animation, rockfall sounds and hit timing, book page-layout pixels, and camera-shake magnitude still require **dated 2006 client captures**. Packet-level camera-shake tests and a new `BarrowsTomeAuthenticityChecks` test prove native 443 compatibility and complete text—not exact historical pixels/audio. The native book group 49 has 11 text rows per side and may need a client widget patch for exact fourteen-page layout.
5. **End-to-end live-client QA**: the `Barrows2006RestorationChecks` now exercises two separate players' ownership, chest open/reopen, access, fresh characters, real JSON pre-loot/post-loot reconnect, native NPC spawns, and chest morph lookup. However, genuine concurrent-network multiplayer combat and an original 2006 client playtest remain needed for animation, pathing, door collision, packet sequencing and graphics. All 0–6 brother kills and death/teleport disconnect combinations should additionally be played manually.

## 12 November 2026 audit remediation

- The first-run hidden brother no longer defaults to Verac: a new `Player` randomly chooses one of the six. Loading a saved unfinished run overrides it with the saved choice.
- Barrows-owned NPCs use owner-aware combat validation and progress attribution while the pre-existing fixed monsters remain public. Their visible right-click options are not globally hidden; historical 2006 visibility rules are distinct from kill ownership.
- Chest opening uses private varp 453 instead of global `DynamicObject`; the player-specific client ID `6775` now resolves to a real unmodified chest `6774` or `10284` and the open state restores before the 443 login varps are sent.
- The old Squire (native `3797`) and Strange Old Man (native `2024`) have dialogue; the latter can provide a crumbling tome. The two later 2014 spades were removed from the binary spawn table. Exact period speech is explicitly marked reconstructed.
- Ahrim can use `SpellDefinition.WEAKEN` in addition to Fire Wave, Confuse and Curse. Surface mounds and crypt objects enforce members-world access.
- Post-combat equipment wear requires a live attacker/target on the same plane, and recent incoming threats must remain nearby. Already-broken 0% pieces avoid the misleading drop-degradation warning.
- The regression suite tests both players, real saved/reloaded characters, the new NPCs and their 443 IDs, dated item spawns, private chest morph lookup, and all original tome words and page starts.
- **Period evidence boundary:** later OSRS drop tables, 2013 cache snapshots or reconstructed dialogue are not equivalent to recovered original 2006 server constants. Do not silently mark unknowns as “verified”.

### Source classes and evidence-quality caveat

- *Contemporary 2006*: Tip.It July report on door-monster despawning/room caps and August guide on tunnel collapse; contemporary sources document behaviours but rarely their internal RNG, sound IDs or tick counts.
- *Revision-443 cache*: native NPC, item, object and interface definitions establish names/IDs and what the **specific 443 client is capable of**, not the chronological origin of every mechanic.
- *Later preserved OSRS*: [Barrows chest reward mechanics](https://oldschool.runescape.wiki/w/Chest_%28Barrows%29), [Squire smith](https://oldschool.runescape.wiki/w/Squire_%28Void_Knights_smith%29), and the Old Man's surviving regular-dialogue variants provide implementation context, **not proof those exact constants/text existed unchanged in 2006**.
- *Dated feature addition*: [Barrows spade additions on 9 January 2014](https://oldschool.runescape.wiki/w/Barrows) distinguish later access convenience from original content.

## 12 November 2026 Old Man / repair-Squire dialogue follow-up

- **Implemented:** The Strange Old Man's secret now allows an accept/decline choice and subsequent reply; the knock-knock joke includes the player's reaction; asking about the scream returns to another scream and the same leave/stay choices. `Barrows2006RestorationChecks` exercises those branch continuations and termination paths.
- **Kept:** The existing spade-dependent first response and no-spade advice, with no promise of later-added spades at every mound. The original 2006 spade condition is not independently proven by the preserved 2012 transcript, so no stronger historical claim is made.
- **Unchanged:** Both collision-safe, cache-native NPC spawn tiles; the Squire's working Barrows repair flow; repair rates; NPC IDs; reward/drop RNG; tunnel encounter weights; and maze generation. The old Squire's exact speech and any distinct axe/pickaxe repair feature lack sufficient 2006 evidence for a speculative implementation.
- **Historical sources:** [Preserved Strange Old Man transcript](https://wiki.darkan.org/Transcript:Strange_Old_Man) (2012 snapshot; some wording postdates 2006) and [Void Knights' smith](https://oldschool.runescape.wiki/w/Squire_%28Void_Knights_smith%29) (released 18 April 2006). The original NPC dialogue and exact tiles remain unconfirmed for 2006.

## Testing

The push-triggered `.github/workflows/verify-barrows-morytania.yml` compiles the Java server with JDK 8 and runs Barrows and associated Morytania regression checks. Use the Actions run for the actual branch commit when reporting CI status.
