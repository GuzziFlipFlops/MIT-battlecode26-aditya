package Hermes.comms;

import battlecode.common.*;
import Hermes.*;

public class CommArray {

    private static final int CAT_START = 0;
    private static final int CAT_END = 7;
    private static final int ENEMY_KING_START = 8;
    private static final int ENEMY_KING_END = 15;
    private static final int ALLIED_KING_START = 16;
    private static final int ALLIED_KING_END = 23;
    private static final int CHEESE_MINE_START = 24;
    private static final int CHEESE_MINE_END = 31;
    private static final int DAMAGE_TRACKING_START = 32;
    private static final int DAMAGE_TRACKING_END = 39;
    private static final int CHEESE_TRANSFER_START = 40;
    private static final int CHEESE_TRANSFER_END = 43;

    public static final int FLAG_BACKSTAB_READY = 48;
    public static final int FLAG_DEFEND_KING = 49;
    public static final int FLAG_ATTACK_CAT = 50;
    public static final int FLAG_GAME_PHASE = 52;
    
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
    }

    public static int packLocation(MapLocation loc) {
        if (loc == null) return 0;
        return loc.x * 100 + loc.y;
    }

    public static MapLocation unpackLocation(int packed) {
        if (packed == 0) return null;
        int x = packed / 100;
        int y = packed % 100;
        return new MapLocation(x, y);
    }

    public static void reportCat(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        
        int packed = packLocation(loc);
        
        for (int i = CAT_START; i <= CAT_END; i++) {
            int current = Globals.rc.readSharedArray(i);
            if (current == 0) {
                Globals.rc.writeSharedArray(i, packed);
                return;
            }
            MapLocation existing = unpackLocation(current);
            if (existing != null && existing.equals(loc)) {
                return;
            }
        }
        
        Globals.rc.writeSharedArray(CAT_START, packed);
    }

    public static void reportEnemyKing(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        
        int packed = packLocation(loc);
        
        for (int i = ENEMY_KING_START; i <= ENEMY_KING_END; i++) {
            int current = Globals.rc.readSharedArray(i);
            if (current == 0) {
                Globals.rc.writeSharedArray(i, packed);
                return;
            }
            MapLocation existing = unpackLocation(current);
            if (existing != null && existing.equals(loc)) {
                return;
            }
        }
        
        Globals.rc.writeSharedArray(ENEMY_KING_START, packed);
    }

    public static void reportAlliedKing(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        
        int packed = packLocation(loc);
        
        for (int i = ALLIED_KING_START; i <= ALLIED_KING_END; i++) {
            int current = Globals.rc.readSharedArray(i);
            if (current == 0) {
                Globals.rc.writeSharedArray(i, packed);
                return;
            }
            MapLocation existing = unpackLocation(current);
            if (existing != null && existing.equals(loc)) {
                Globals.rc.writeSharedArray(i, packed);
                return;
            }
        }
    }

    public static void reportCheeseMine(MapLocation loc) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        
        int packed = packLocation(loc);
        
        for (int i = CHEESE_MINE_START; i <= CHEESE_MINE_END; i++) {
            int current = Globals.rc.readSharedArray(i);
            if (current == 0) {
                Globals.rc.writeSharedArray(i, packed);
                return;
            }
            MapLocation existing = unpackLocation(current);
            if (existing != null && existing.equals(loc)) {
                return;
            }
        }
    }

    private static void readEnemyKingPositions() throws GameActionException {
        Globals.numKnownEnemyKings = 0;
        for (int i = ENEMY_KING_START; i <= ENEMY_KING_END && Globals.numKnownEnemyKings < 5; i++) {
            MapLocation loc = unpackLocation(Globals.rc.readSharedArray(i));
            if (loc != null) {
                Globals.knownEnemyKings[Globals.numKnownEnemyKings++] = loc;
            }
        }
    }

    private static void readAlliedKingPositions() throws GameActionException {
        Globals.numKnownAlliedKings = 0;
        for (int i = ALLIED_KING_START; i <= ALLIED_KING_END && Globals.numKnownAlliedKings < 5; i++) {
            MapLocation loc = unpackLocation(Globals.rc.readSharedArray(i));
            if (loc != null) {
                Globals.knownAlliedKings[Globals.numKnownAlliedKings++] = loc;
            }
        }
    }

    private static void readCheeseMineLocations() throws GameActionException {
        Globals.numKnownCheeseMines = 0;
        for (int i = CHEESE_MINE_START; i <= CHEESE_MINE_END && Globals.numKnownCheeseMines < 10; i++) {
            MapLocation loc = unpackLocation(Globals.rc.readSharedArray(i));
            if (loc != null) {
                Globals.knownCheeseMines[Globals.numKnownCheeseMines++] = loc;
            }
        }
    }

    public static void setFlag(int flagIndex, int value) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        Globals.rc.writeSharedArray(flagIndex, value);
    }

    public static int getFlag(int flagIndex) throws GameActionException {
        return Globals.rc.readSharedArray(flagIndex);
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
        int current = Globals.rc.readSharedArray(TOTAL_DAMAGE_INDEX);
        int newTotal = Math.min(current + damage, GameConstants.COMM_ARRAY_MAX_VALUE);
        Globals.rc.writeSharedArray(TOTAL_DAMAGE_INDEX, newTotal);
    }

    public static int getTotalCatDamage() throws GameActionException {
        return Globals.rc.readSharedArray(TOTAL_DAMAGE_INDEX);
    }

    public static void addCheeseTransfer(int amount) throws GameActionException {
        if (Globals.myType != UnitType.RAT_KING) return;
        int current = Globals.rc.readSharedArray(TOTAL_CHEESE_TRANSFER_INDEX);
        int newTotal = Math.min(current + amount, GameConstants.COMM_ARRAY_MAX_VALUE);
        Globals.rc.writeSharedArray(TOTAL_CHEESE_TRANSFER_INDEX, newTotal);
    }

    public static int getTotalCheeseTransfer() throws GameActionException {
        return Globals.rc.readSharedArray(TOTAL_CHEESE_TRANSFER_INDEX);
    }
}
