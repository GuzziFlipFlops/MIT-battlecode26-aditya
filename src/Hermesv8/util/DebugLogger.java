package Hermesv8.util;
import battlecode.common.*;
import Hermesv8.*;
public class DebugLogger {
    private static final boolean ENABLE_DEBUG = false;
    private static final boolean DEBUG_STATE = true;
    private static final boolean DEBUG_CHEESE = true;
    private static final boolean DEBUG_NAV = true;
    private static final boolean DEBUG_COMBAT = true;
    private static final boolean DEBUG_SPAWN = true;
    private static final boolean DEBUG_MODE = true;
    private static final boolean DEBUG_GRADIENT = true;
    private static final boolean DEBUG_DELIVERY = true;
    private static final boolean DEBUG_COLLECTION = true;
    private static final boolean DEBUG_ACTION = true;
    private static final boolean DEBUG_EXCEPTION = true;
    private static final boolean DEBUG_VISION = true;
    private static final boolean DEBUG_COMM = true;

    public static void logState(String component, String oldState, String newState) {
        if (ENABLE_DEBUG && DEBUG_STATE) {
            System.out.println("[DEBUG STATE] " + component + " #" + Globals.myID + " @R" + Globals.roundNum + " | " + oldState + " -> " + newState + " | Loc:" + Globals.myLoc + " | Cheese:" + Globals.myCheese + " | Health:" + Globals.myHealth);
        }
    }

    public static void logCheese(String action, int amount, MapLocation loc) {
        if (ENABLE_DEBUG && DEBUG_CHEESE) {
            System.out.println("[DEBUG CHEESE] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Amount:" + amount + " | Loc:" + loc + " | MyCheese:" + Globals.myCheese + " | GlobalCheese:" + Globals.globalCheese);
        }
    }

    public static void logDelivery(String action, int amount, MapLocation kingLoc, boolean success) {
        if (ENABLE_DEBUG && DEBUG_DELIVERY) {
            System.out.println("[DEBUG DELIVERY] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Amount:" + amount + " | KingLoc:" + kingLoc + " | Success:" + success + " | MyLoc:" + Globals.myLoc + " | Dist:" + (kingLoc != null ? Globals.myLoc.distanceSquaredTo(kingLoc) : -1));
        }
    }

    public static void logCollection(String action, MapLocation cheeseLoc, int amount, boolean success) {
        if (ENABLE_DEBUG && DEBUG_COLLECTION) {
            System.out.println("[DEBUG COLLECTION] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | CheeseLoc:" + cheeseLoc + " | Amount:" + amount + " | Success:" + success + " | MyLoc:" + Globals.myLoc + " | MyCheese:" + Globals.myCheese + " | Cap:" + Constants.CARRY_CAP);
        }
    }

    public static void logNav(String action, MapLocation target, Direction dir, boolean success, String reason) {
        if (ENABLE_DEBUG && DEBUG_NAV) {
            System.out.println("[DEBUG NAV] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Target:" + target + " | Dir:" + dir + " | Success:" + success + " | Reason:" + reason + " | MyLoc:" + Globals.myLoc + " | CanMove:" + Globals.rc.isMovementReady() + " | CanTurn:" + Globals.rc.canTurn());
        }
    }

    public static void logCombat(String action, RobotInfo target, int damage, boolean success) {
        if (ENABLE_DEBUG && DEBUG_COMBAT) {
            System.out.println("[DEBUG COMBAT] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Target:" + (target != null ? target.getID() : "null") + " | Damage:" + damage + " | Success:" + success + " | MyLoc:" + Globals.myLoc);
        }
    }

    public static void logSpawn(String action, MapLocation loc, int cost, boolean success, String reason) {
        if (ENABLE_DEBUG && DEBUG_SPAWN) {
            System.out.println("[DEBUG SPAWN] King #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Loc:" + loc + " | Cost:" + cost + " | Success:" + success + " | Reason:" + reason + " | GlobalCheese:" + Globals.globalCheese);
        }
    }

    public static void logMode(int oldMode, int newMode, String reason) {
        if (ENABLE_DEBUG && DEBUG_MODE) {
            System.out.println("[DEBUG MODE] King #" + Globals.myID + " @R" + Globals.roundNum + " | Mode:" + oldMode + " -> " + newMode + " | Reason:" + reason + " | Cheese:" + Globals.globalCheese);
        }
    }

    public static void logGradient(String action, MapLocation start, boolean success, int bytecodes) {
        if (ENABLE_DEBUG && DEBUG_GRADIENT) {
            System.out.println("[DEBUG GRADIENT] King #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Start:" + start + " | Success:" + success + " | Bytecodes:" + bytecodes);
        }
    }

    public static void logAction(String action, boolean success, String reason) {
        if (ENABLE_DEBUG && DEBUG_ACTION) {
            System.out.println("[DEBUG ACTION] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Success:" + success + " | Reason:" + reason + " | ActionReady:" + Globals.rc.isActionReady() + " | MovementReady:" + Globals.rc.isMovementReady());
        }
    }

    public static void logException(String component, Exception e, String context) {
        if (ENABLE_DEBUG && DEBUG_EXCEPTION) {
            String exceptionType = e.toString().split(":")[0];
            System.out.println("[DEBUG EXCEPTION] " + component + " #" + Globals.myID + " @R" + Globals.roundNum + " | " + exceptionType + " | " + e.getMessage() + " | Context:" + context);
            e.printStackTrace();
        }
    }

    public static void logVision(String action, int robotsSensed, int mapInfosSensed) {
        if (ENABLE_DEBUG && DEBUG_VISION) {
            System.out.println("[DEBUG VISION] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Robots:" + robotsSensed + " | MapInfos:" + mapInfosSensed + " | MyLoc:" + Globals.myLoc + " | Facing:" + Globals.myDir);
        }
    }

    public static void logComm(String action, int index, int value, boolean success) {
        if (ENABLE_DEBUG && DEBUG_COMM) {
            System.out.println("[DEBUG COMM] " + (Globals.myType == UnitType.RAT_KING ? "King" : "Rat") + " #" + Globals.myID + " @R" + Globals.roundNum + " | " + action + " | Index:" + index + " | Value:" + value + " | Success:" + success);
        }
    }

    public static void logKingStatus(int cheese, int health, int roundsLeft) {
        if (ENABLE_DEBUG) {
            System.out.println("[DEBUG KING] King #" + Globals.myID + " @R" + Globals.roundNum + " | Cheese:" + cheese + " | Health:" + health + " | RoundsLeft:" + roundsLeft + " | Consumption:" + Constants.RATKING_CHEESE_CONSUMPTION + " | Need:" + (Constants.RATKING_CHEESE_CONSUMPTION * roundsLeft));
        }
    }

    public static void logRatStatus(int cheese, int health, String state) {
        if (ENABLE_DEBUG) {
            System.out.println("[DEBUG RAT] Rat #" + Globals.myID + " @R" + Globals.roundNum + " | Cheese:" + cheese + " | Health:" + health + " | State:" + state + " | Loc:" + Globals.myLoc);
        }
    }
}
