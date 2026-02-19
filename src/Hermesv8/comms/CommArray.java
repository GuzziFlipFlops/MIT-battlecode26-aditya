package Hermesv8.comms;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.util.DebugLogger;
public class CommArray {
    private static final int CAT_START = 0;
    private static final int CAT_END = 7;
    private static final int ENEMY_KING_START = 8;
    private static final int ENEMY_KING_END = 15;
    private static final int ALLIED_KING_START = 16;
    private static final int ALLIED_KING_END = 23;
    private static final int CHEESE_MINE_START = 24;
    private static final int CHEESE_MINE_END = 31;
    @SuppressWarnings("unused")
    private static final int DAMAGE_TRACKING_START = 32;
    @SuppressWarnings("unused")
    private static final int DAMAGE_TRACKING_END = 39;
    @SuppressWarnings("unused")
    private static final int CHEESE_TRANSFER_START = 40;
    @SuppressWarnings("unused")
    private static final int CHEESE_TRANSFER_END = 43;
    public static final int FLAG_BACKSTAB_READY = 48;
    public static final int FLAG_DEFEND_KING = 49;
    public static final int FLAG_ATTACK_CAT = 50;
    public static final int FLAG_GAME_PHASE = 52;
    public static final int MODE_INDEX = 53;
    public static final int ATTACK_TARGET_X = 54;
    public static final int ATTACK_TARGET_Y = 55;
    public static final int LAST_UPDATED_HOME_ROUND = 56;
    public static final int LAST_UPDATED_TARGET_ROUND = 57;
    private static final int TOTAL_DAMAGE_INDEX = 32;
    private static final int TOTAL_CHEESE_TRANSFER_INDEX = 40;
    public static void init() {
    }
    public static void update() {
        try {
            readEnemyKingPositions();
        } catch (Exception e) {
        }
        try {
            readAlliedKingPositions();
        } catch (Exception e) {
        }
        try {
            readCheeseMineLocations();
        } catch (Exception e) {
        }
        try {
            readCatPositions();
        } catch (Exception e) {
        }
    }
    public static void writeLocation(int index, MapLocation loc) throws GameActionException {
        if (loc == null) {
            DebugLogger.logComm("writeSharedArray", index, 0, true);
            Globals.rc.writeSharedArray(index, 0);
            DebugLogger.logComm("writeSharedArray", index + 1, 0, true);
            Globals.rc.writeSharedArray(index + 1, 0);
        } else {
            DebugLogger.logComm("writeSharedArray", index, loc.x, true);
            Globals.rc.writeSharedArray(index, loc.x);
            DebugLogger.logComm("writeSharedArray", index + 1, loc.y, true);
            Globals.rc.writeSharedArray(index + 1, loc.y);
        }
    }

    public static MapLocation readLocation(int index) throws GameActionException {
        int x = Globals.rc.readSharedArray(index);
        DebugLogger.logComm("readSharedArray", index, x, true);
        int y = Globals.rc.readSharedArray(index + 1);
        DebugLogger.logComm("readSharedArray", index + 1, y, true);
        if (x == 0 && y == 0) return null;
        if (x < 0 || x >= Globals.mapWidth || y < 0 || y >= Globals.mapHeight) return null;
        return new MapLocation(x, y);
    }

    @Deprecated
    public static int packLocation(MapLocation loc) {
        if (loc == null) return 0;
        return loc.x * 32 + loc.y;
    }

    @Deprecated
    public static MapLocation unpackLocation(int packed) {
        if (packed == 0) return null;
        int x = packed / 32;
        int y = packed % 32;
        return new MapLocation(x, y);
    }
    public static void reportCat(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        for (int i = CAT_START; i <= CAT_END; i += 2) {
            MapLocation existing = readLocation(i);
            if (existing == null) {
                writeLocation(i, loc);
                return;
            }
            if (existing.equals(loc)) {
                return;
            }
        }
        writeLocation(CAT_START, loc);
    }
    public static void reportEnemyKing(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) {
            System.out.println("[DEBUG CommArray] Rat #" + Globals.myID + " (baby rat) found enemy king at " + loc + " but can't write to array");
            return;
        }
        for (int i = ENEMY_KING_START; i <= ENEMY_KING_END; i += 2) {
            MapLocation existing = readLocation(i);
            if (existing == null) {
                writeLocation(i, loc);
                System.out.println("[DEBUG CommArray] Rat King #" + Globals.myID + " reported enemy king at " + loc);
                return;
            }
            if (existing.equals(loc)) {
                return;
            }
        }
        writeLocation(ENEMY_KING_START, loc);
        System.out.println("[DEBUG CommArray] Rat King #" + Globals.myID + " reported enemy king at " + loc + " (overwrite)");
    }
    public static void reportAlliedKing(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) {
            if (Globals.roundNum % 50 == 0) {
                System.out.println("[DEBUG] reportAlliedKing called but myType=" + Globals.myType + ", not RAT_KING");
            }
            return;
        }
        writeLocation(ALLIED_KING_START, loc);
        for (int i = ALLIED_KING_START + 2; i <= ALLIED_KING_END; i += 2) {
            MapLocation existing = readLocation(i);
            if (existing != null && existing.equals(loc)) {
                writeLocation(i, loc);
                break;
            }
        }
    }
    public static void reportCheeseMine(MapLocation loc) throws GameActionException {
        if (loc == null) return;
        for (int i = CHEESE_MINE_START; i <= CHEESE_MINE_END; i += 2) {
            MapLocation existing = readLocation(i);
            if (existing == null) {
                writeLocation(i, loc);
                return;
            }
            if (existing.equals(loc)) {
                return;
            }
        }
    }
    private static void readEnemyKingPositions() throws GameActionException {
        Globals.numKnownEnemyKings = 0;
        for (int i = ENEMY_KING_START; i <= ENEMY_KING_END && Globals.numKnownEnemyKings < 5; i += 2) {
            MapLocation loc = readLocation(i);
            if (loc != null) {
                Globals.knownEnemyKings[Globals.numKnownEnemyKings++] = loc;
            }
        }
    }
    private static void readAlliedKingPositions() throws GameActionException {
        Globals.numKnownAlliedKings = 0;
        for (int i = ALLIED_KING_START; i <= ALLIED_KING_END && Globals.numKnownAlliedKings < 5; i += 2) {
            MapLocation loc = readLocation(i);
            if (loc != null) {
                if (loc.x >= 0 && loc.x < Globals.mapWidth && loc.y >= 0 && loc.y < Globals.mapHeight) {
                    Globals.knownAlliedKings[Globals.numKnownAlliedKings++] = loc;
                }
            }
        }
    }
    private static void readCheeseMineLocations() throws GameActionException {
        Globals.numKnownCheeseMines = 0;
        for (int i = CHEESE_MINE_START; i <= CHEESE_MINE_END && Globals.numKnownCheeseMines < 10; i += 2) {
            MapLocation loc = readLocation(i);
            if (loc != null) {
                Globals.knownCheeseMines[Globals.numKnownCheeseMines++] = loc;
            }
        }
    }
    public static void readCatPositions() throws GameActionException {
        Globals.numKnownCats = 0;
        for (int i = CAT_START; i <= CAT_END && Globals.numKnownCats < 10; i += 2) {
            MapLocation loc = readLocation(i);
            if (loc != null) {
                Globals.knownCats[Globals.numKnownCats++] = loc;
            }
        }
    }
    public static MapLocation getNearestCat() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = cat.getLocation();
                    }
                }
            }
        }
        for (int i = 0; i < Globals.numKnownCats; i++) {
            MapLocation catLoc = Globals.knownCats[i];
            if (catLoc != null) {
                int dist = Globals.myLoc.distanceSquaredTo(catLoc);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = catLoc;
                }
            }
        }
        return nearest;
    }
    public static void setFlag(int flagIndex, int value) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        DebugLogger.logComm("writeSharedArray", flagIndex, value, true);
            Globals.rc.writeSharedArray(flagIndex, value);
    }
    public static int getFlag(int flagIndex) throws GameActionException {
        int value = Globals.rc.readSharedArray(flagIndex);
        DebugLogger.logComm("readSharedArray", flagIndex, value, true);
        return value;
    }
    public static void signalBackstab() throws GameActionException {
        setFlag(FLAG_BACKSTAB_READY, 1);
    }
    public static boolean isBackstabSignaled() throws GameActionException {
        return getFlag(FLAG_BACKSTAB_READY) == 1;
    }
    public static MapLocation getNearestAlliedKing() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < Globals.numKnownAlliedKings; i++) {
            MapLocation kingLoc = Globals.knownAlliedKings[i];
            if (kingLoc != null) {
                int dist = Globals.myLoc.distanceSquaredTo(kingLoc);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = kingLoc;
                }
            }
        }
        return nearest;
    }
    public static MapLocation getNearestCheeseMine() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < Globals.numKnownCheeseMines; i++) {
            MapLocation mineLoc = Globals.knownCheeseMines[i];
            if (mineLoc != null) {
                int dist = Globals.myLoc.distanceSquaredTo(mineLoc);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = mineLoc;
                }
            }
        }
        return nearest;
    }
    public static MapLocation getNearestEnemyKing() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < Globals.numKnownEnemyKings; i++) {
            MapLocation kingLoc = Globals.knownEnemyKings[i];
            if (kingLoc != null) {
                int dist = Globals.myLoc.distanceSquaredTo(kingLoc);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = kingLoc;
                }
            }
        }
        return nearest;
    }
    public static void addCatDamage(int damage) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        int value = Globals.rc.readSharedArray(TOTAL_DAMAGE_INDEX);
        DebugLogger.logComm("readSharedArray", TOTAL_DAMAGE_INDEX, value, true);
        int current = value;
        int newTotal = Math.min(current + damage, GameConstants.COMM_ARRAY_MAX_VALUE);
        DebugLogger.logComm("writeSharedArray", TOTAL_DAMAGE_INDEX, newTotal, true);
            Globals.rc.writeSharedArray(TOTAL_DAMAGE_INDEX, newTotal);
    }
    public static int getTotalCatDamage() throws GameActionException {
        int value = Globals.rc.readSharedArray(TOTAL_DAMAGE_INDEX);
        DebugLogger.logComm("readSharedArray", TOTAL_DAMAGE_INDEX, value, true);
        return value;
    }
    public static void addCheeseTransfer(int amount) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        int value = Globals.rc.readSharedArray(TOTAL_CHEESE_TRANSFER_INDEX);
        DebugLogger.logComm("readSharedArray", TOTAL_CHEESE_TRANSFER_INDEX, value, true);
        int current = value;
        int newTotal = Math.min(current + amount, GameConstants.COMM_ARRAY_MAX_VALUE);
        DebugLogger.logComm("writeSharedArray", TOTAL_CHEESE_TRANSFER_INDEX, newTotal, true);
            Globals.rc.writeSharedArray(TOTAL_CHEESE_TRANSFER_INDEX, newTotal);
    }
    public static int getTotalCheeseTransfer() throws GameActionException {
        int value = Globals.rc.readSharedArray(TOTAL_CHEESE_TRANSFER_INDEX);
        DebugLogger.logComm("readSharedArray", TOTAL_CHEESE_TRANSFER_INDEX, value, true);
        return value;
    }

    public static void setMode(int mode) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        DebugLogger.logComm("writeSharedArray", MODE_INDEX, mode, true);
            Globals.rc.writeSharedArray(MODE_INDEX, mode);
    }

    public static int getMode() throws GameActionException {
        int value = Globals.rc.readSharedArray(MODE_INDEX);
        DebugLogger.logComm("readSharedArray", MODE_INDEX, value, true);
        return value;
    }

    public static void setAttackTarget(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        if (loc == null) {
            DebugLogger.logComm("writeSharedArray", ATTACK_TARGET_X, 0, true);
            Globals.rc.writeSharedArray(ATTACK_TARGET_X, 0);
            DebugLogger.logComm("writeSharedArray", ATTACK_TARGET_Y, 0, true);
            Globals.rc.writeSharedArray(ATTACK_TARGET_Y, 0);
        } else {
            DebugLogger.logComm("writeSharedArray", ATTACK_TARGET_X, loc.x, true);
            Globals.rc.writeSharedArray(ATTACK_TARGET_X, loc.x);
            DebugLogger.logComm("writeSharedArray", ATTACK_TARGET_Y, loc.y, true);
            Globals.rc.writeSharedArray(ATTACK_TARGET_Y, loc.y);
        }
    }

    public static MapLocation getAttackTarget() throws GameActionException {
        int valueX = Globals.rc.readSharedArray(ATTACK_TARGET_X);
        DebugLogger.logComm("readSharedArray", ATTACK_TARGET_X, valueX, true);
        int x = valueX;
        int valueY = Globals.rc.readSharedArray(ATTACK_TARGET_Y);
        DebugLogger.logComm("readSharedArray", ATTACK_TARGET_Y, valueY, true);
        int y = valueY;
        if (x == 0 && y == 0) return null;
        return new MapLocation(x, y);
    }

    public static void setLastUpdatedHomeRound(int round) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        DebugLogger.logComm("writeSharedArray", LAST_UPDATED_HOME_ROUND, round, true);
            Globals.rc.writeSharedArray(LAST_UPDATED_HOME_ROUND, round);
    }

    public static int getLastUpdatedHomeRound() throws GameActionException {
        int value = Globals.rc.readSharedArray(LAST_UPDATED_HOME_ROUND);
        DebugLogger.logComm("readSharedArray", LAST_UPDATED_HOME_ROUND, value, true);
        return value;
    }

    public static void setLastUpdatedTargetRound(int round) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        DebugLogger.logComm("writeSharedArray", LAST_UPDATED_TARGET_ROUND, round, true);
            Globals.rc.writeSharedArray(LAST_UPDATED_TARGET_ROUND, round);
    }

    public static int getLastUpdatedTargetRound() throws GameActionException {
        int value = Globals.rc.readSharedArray(LAST_UPDATED_TARGET_ROUND);
        DebugLogger.logComm("readSharedArray", LAST_UPDATED_TARGET_ROUND, value, true);
        return value;
    }
}