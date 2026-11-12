import com.rs2.ConfigFile;
import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.gameplay.castlewars.CastleWarsManager;
import com.rs2.model.gameplay.castlewars.CastleWarsManager.Team;
import com.rs2.model.player.Player;
import com.rs2.model.player.CharacterFileRecord;
import com.rs2.model.item.ItemStack;
import com.rs2.util.CharacterJsonCodec;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Map;

/**
 * Historical pre-2007 Castle Wars rules and the actual overlapping save slots.
 * All mutations to static game state are restored before exiting.
 */
public final class CastleWarsPreMergeRulesChecks {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean oldF2p = ServerSettings.freeToPlayWorld;
        int oldMembership = ServerSettings.membershipRequirementMode;
        int oldMinimum = ServerSettings.castleWarsMinPlayersPerTeam;
        Field scoresSara = field(CastleWarsManager.class, "saradominScore");
        Field scoresZam = field(CastleWarsManager.class, "zamorakScore");
        int oldSara = scoresSara.getInt(null), oldZam = scoresZam.getInt(null);
        Map<Player,Team> waiting =
                (Map<Player,Team>)field(CastleWarsManager.class, "waitingPlayers").get(null);
        Map<Player,Team> games =
                (Map<Player,Team>)field(CastleWarsManager.class, "gamePlayers").get(null);
        Player person = new Player(null);
        Player rival = new Player(null);
        Player[] online = World.getPlayers();
        Player previous75 = online[75], previous76 = online[76];
        person.setIndex(75); rival.setIndex(76);
        person.setEncodedIndex(32768 + 75); rival.setEncodedIndex(32768 + 76);
        online[75] = person; online[76] = rival;
        person.setPosition(new Position(2400, 3100, 0));
        rival.setPosition(new Position(2400, 3108, 0));
        try {
            // A paying account cannot enter Castle Wars on an F2P world.
            ServerSettings.membershipRequirementMode = 1;
            ServerSettings.freeToPlayWorld = true;
            require(!CastleWarsManager.canEnterMembersCastleWars(person),
                    "Members account entered Castle Wars on a free world");
            require(CastleWarsManager.handleLobbyPortal(person,
                            CastleWarsManager.SARADOMIN_PORTAL_ID),
                    "F2P portal click was not handled");
            require(CastleWarsManager.getWaitingTeam(person) == null,
                    "Denied F2P portal click queued the player");

            // Non-members cannot enter even on a P2P world.
            ServerSettings.freeToPlayWorld = false;
            ServerSettings.membershipRequirementMode = 0;
            require(!CastleWarsManager.canEnterMembersCastleWars(person),
                    "Unsubscribed player can access Castle Wars");
            require(CastleWarsManager.handleLobbyPortal(person,
                            CastleWarsManager.ZAMORAK_PORTAL_ID),
                    "Non-member portal click was not handled");
            require(CastleWarsManager.getWaitingTeam(person) == null,
                    "Denied non-member portal click queued the player");

            ServerSettings.membershipRequirementMode = 1;
            require(CastleWarsManager.canEnterMembersCastleWars(person),
                    "Eligible P2P member cannot enter Castle Wars");
            person.isBot = true;
            require(CastleWarsManager.canEnterMembersCastleWars(person),
                    "Members-world bot is not eligible for Castle Wars");

            // Spellbook and item teleports share the same restricted-area guard.
            require(!person.isInTeleportRestrictedArea(),
                    "Tower should be unrestricted outside a match");
            games.put(person, Team.SARADOMIN);
            games.put(rival, Team.ZAMORAK);
            require(person.isInTeleportRestrictedArea(),
                    "An arena participant can cast spells to teleport away");
            require(!person.getTeleportManager().castSpellbookTeleport(new Position(3222,3218,0)),
                    "Spellbook teleport escaped Castle Wars");
            require(!person.getTeleportManager().castItemTeleport(new Position(3222,3218,0)),
                    "Jewellery teleport escaped Castle Wars");
            games.remove(person); games.remove(rival);
            require(!person.isInTeleportRestrictedArea(),
                    "Castle Wars teleports remain disabled after leaving the game");

            // Default five-player era rule and explicit configurable dev mode.
            ServerSettings.castleWarsMinPlayersPerTeam = 5;
            person.setPosition(new Position(2377, 9485, 0));
            rival.setPosition(new Position(2421, 9524, 0));
            waiting.put(person, Team.SARADOMIN);
            waiting.put(rival, Team.ZAMORAK);
            require(!CastleWarsManager.hasMinimumPlayersToStart(),
                    "A 1v1 match bypassed the historical five-per-team minimum");
            ConfigFile.applyConfigEntry("CASTLE_WARS_MIN_PLAYERS_PER_TEAM",
                    new String[]{"1"});
            require(ServerSettings.castleWarsMinPlayersPerTeam == 1
                            && CastleWarsManager.hasMinimumPlayersToStart(),
                    "Explicit development 1v1 config not respected");
            ConfigFile.applyConfigEntry("CASTLE_WARS_MIN_PLAYERS_PER_TEAM",
                    new String[]{"99"});
            require(ServerSettings.castleWarsMinPlayersPerTeam == 5
                            && !CastleWarsManager.hasMinimumPlayersToStart(),
                    "Server config did not cap the five-player historical minimum");
            waiting.remove(person); waiting.remove(rival);

            // A loss must not consume, inflate or reset pending ticket claims.
            person.reservedSaveInt1 = 0;
            person.reservedSaveInt2 = 0;
            person.reservedSaveInt3 = 7; // Existing pending rewards.
            person.castleWarsLosses = 3;
            scoresSara.setInt(null, 0); scoresZam.setInt(null, 1);
            Method result = CastleWarsManager.class.getDeclaredMethod(
                    "recordPlayerScoreboardResult", Player.class, Team.class);
            result.setAccessible(true);
            result.invoke(null, person, Team.SARADOMIN);
            require(person.reservedSaveInt1 == 1 && person.reservedSaveInt2 == 0
                            && person.castleWarsLosses == 4
                            && person.reservedSaveInt3 == 7,
                    "Game loss changed unclaimed tickets or failed to update loss stats");
            scoresSara.setInt(null, 2); scoresZam.setInt(null, 0);
            result.invoke(null, person, Team.SARADOMIN);
            require(person.reservedSaveInt1 == 2 && person.reservedSaveInt2 == 1
                            && person.castleWarsLosses == 4
                            && person.reservedSaveInt3 == 7,
                    "Game win corrupted pending tickets or loss stats");

            File test = Files.createTempFile("castlewars-split-save-", ".json").toFile();
            try {
                CharacterFileRecord record = new CharacterFileRecord();
                record.username = "CastleWarsPreMergeTest";
                record.reservedSaveInt3 = person.reservedSaveInt3;
                record.castleWarsLosses = person.castleWarsLosses;
                CharacterJsonCodec.save(test, record);
                CharacterFileRecord loaded = CharacterJsonCodec.read(test, record.username);
                require(loaded.reservedSaveInt3 == 7 && loaded.castleWarsLosses == 4,
                        "Separate character JSON fields were not persisted independently");
            } finally {
                Files.deleteIfExists(test.toPath());
                Files.deleteIfExists(new File(test.getPath() + ".bak").toPath());
            }
        } finally {
            waiting.remove(person); waiting.remove(rival);
            games.remove(person); games.remove(rival);
            scoresSara.setInt(null, oldSara); scoresZam.setInt(null, oldZam);
            ServerSettings.freeToPlayWorld = oldF2p;
            ServerSettings.membershipRequirementMode = oldMembership;
            ServerSettings.castleWarsMinPlayersPerTeam = oldMinimum;
            online[75] = previous75; online[76] = previous76;
            person.setIndex(-1); rival.setIndex(-1);
        }
        System.out.println("Castle Wars 2006 membership, 5v5, teleport, losses and JSON checks passed");
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
