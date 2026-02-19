package Hermesv8.robot;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;

/**
 * Handles rat king expansion via baby rat promotion
 * Based on oblock's pattern-based validation system
 */
public class KingExpansion {
    private static int bestDirection = -1; // -1=none, 0=S, 1=N, 2=E, 3=W
    private static boolean kingMovedForward = false;
    private static int spawnPhase = 0; // 0=none, 1=left, 2=mid, 3=right, 4=done
    private static boolean expansionComplete = false;

    // Shared array indices (using high indices to avoid conflicts)
    private static final int EXPANSION_DIRECTION = 58;
    private static final int EXPANSION_PHASE = 59;
    private static final int PROMOTE_SIGNAL = 60;

    public static void init() {
        bestDirection = -1;
        kingMovedForward = false;
        spawnPhase = 0;
        expansionComplete = false;
    }

    /**
     * Check if we should attempt king expansion
     */
    public static boolean shouldExpand() throws GameActionException {
        if (expansionComplete) return false;
        if (Globals.myType != UnitType.RAT_KING) return false;

        // Only expand if we have LOTS of cheese and stable economy
        int cheese = Globals.rc.getAllCheese();
        int minCheese = Constants.RAT_KING_UPGRADE_COST + 300; // Lowered from 500

        // Check ally count - we need enough rats to support expansion
        int allyCount = (Globals.nearbyAllies != null) ? Globals.nearbyAllies.length : 0;

        boolean shouldExpandNow = cheese >= minCheese && allyCount >= 8 && Globals.roundNum >= 50; // Lowered thresholds

        if (shouldExpandNow) {
            System.out.println("[KingExpansion] Expansion conditions met: cheese=" + cheese + ", allies=" + allyCount + ", round=" + Globals.roundNum);
        }

        return shouldExpandNow;
    }

    /**
     * Validate 3x3 grid in a direction
     */
    private static boolean validateDirection(int cx, int cy, int dir) throws GameActionException {
        MapLocation[] grid = getGridForDirection(cx, cy, dir);

        for (MapLocation loc : grid) {
            if (!Globals.rc.onTheMap(loc)) return false;
            if (!Globals.rc.canSenseLocation(loc)) return false;
            if (!Globals.rc.sensePassability(loc)) return false;
        }
        return true;
    }

    /**
     * Get 3x3 grid locations for direction validation
     */
    private static MapLocation[] getGridForDirection(int cx, int cy, int dir) {
        MapLocation[] grid = new MapLocation[9];

        switch (dir) {
            case 1: // NORTH
                grid[0] = new MapLocation(cx - 1, cy + 2);
                grid[1] = new MapLocation(cx, cy + 2);
                grid[2] = new MapLocation(cx + 1, cy + 2);
                grid[3] = new MapLocation(cx - 1, cy + 3);
                grid[4] = new MapLocation(cx, cy + 3);
                grid[5] = new MapLocation(cx + 1, cy + 3);
                grid[6] = new MapLocation(cx - 1, cy + 4);
                grid[7] = new MapLocation(cx, cy + 4);
                grid[8] = new MapLocation(cx + 1, cy + 4);
                break;
            case 0: // SOUTH
                grid[0] = new MapLocation(cx - 1, cy - 2);
                grid[1] = new MapLocation(cx, cy - 2);
                grid[2] = new MapLocation(cx + 1, cy - 2);
                grid[3] = new MapLocation(cx - 1, cy - 3);
                grid[4] = new MapLocation(cx, cy - 3);
                grid[5] = new MapLocation(cx + 1, cy - 3);
                grid[6] = new MapLocation(cx - 1, cy - 4);
                grid[7] = new MapLocation(cx, cy - 4);
                grid[8] = new MapLocation(cx + 1, cy - 4);
                break;
            case 2: // EAST
                grid[0] = new MapLocation(cx + 2, cy - 1);
                grid[1] = new MapLocation(cx + 2, cy);
                grid[2] = new MapLocation(cx + 2, cy + 1);
                grid[3] = new MapLocation(cx + 3, cy - 1);
                grid[4] = new MapLocation(cx + 3, cy);
                grid[5] = new MapLocation(cx + 3, cy + 1);
                grid[6] = new MapLocation(cx + 4, cy - 1);
                grid[7] = new MapLocation(cx + 4, cy);
                grid[8] = new MapLocation(cx + 4, cy + 1);
                break;
            case 3: // WEST
                grid[0] = new MapLocation(cx - 2, cy - 1);
                grid[1] = new MapLocation(cx - 2, cy);
                grid[2] = new MapLocation(cx - 2, cy + 1);
                grid[3] = new MapLocation(cx - 3, cy - 1);
                grid[4] = new MapLocation(cx - 3, cy);
                grid[5] = new MapLocation(cx - 3, cy + 1);
                grid[6] = new MapLocation(cx - 4, cy - 1);
                grid[7] = new MapLocation(cx - 4, cy);
                grid[8] = new MapLocation(cx - 4, cy + 1);
                break;
        }
        return grid;
    }

    /**
     * Find best direction for expansion
     */
    public static void findExpansionDirection() throws GameActionException {
        if (bestDirection != -1) return; // Already found

        MapLocation myLoc = Globals.myLoc;
        int cx = myLoc.x;
        int cy = myLoc.y;

        // Try directions: NORTH, EAST, SOUTH, WEST
        int[] dirs = {1, 2, 0, 3};
        for (int dir : dirs) {
            if (validateDirection(cx, cy, dir)) {
                bestDirection = dir;
                try {
                    CommArray.setFlag(EXPANSION_DIRECTION, dir + 1); // +1 to distinguish from 0
                } catch (Exception e) {}
                System.out.println("[KingExpansion] Found valid direction: " + dirToString(dir));
                break;
            }
        }
    }

    /**
     * Execute expansion pattern
     */
    public static boolean executeExpansion() throws GameActionException {
        if (bestDirection == -1 || expansionComplete) return false;
        if (Globals.myType != UnitType.RAT_KING) return false;

        // Phase 1: Move king forward
        if (!kingMovedForward) {
            Direction moveDir = getDirectionFromInt(bestDirection);
            if (Globals.rc.canMove(moveDir)) {
                Globals.rc.move(moveDir);
                kingMovedForward = true;
                spawnPhase = 1;
                System.out.println("[KingExpansion] King moved forward");
                return true;
            }
            return false;
        }

        // Phase 2-4: Spawn 3 rats in formation
        if (spawnPhase >= 1 && spawnPhase <= 3) {
            MapLocation spawnLoc = getSpawnLocation(spawnPhase);
            if (spawnLoc != null && Globals.rc.canBuildRat(spawnLoc)) {
                Globals.rc.buildRat(spawnLoc);
                System.out.println("[KingExpansion] Spawned expansion rat " + spawnPhase + " at " + spawnLoc);
                spawnPhase++;

                if (spawnPhase > 3) {
                    // Signal promotion
                    try {
                        CommArray.setFlag(PROMOTE_SIGNAL, 1);
                        System.out.println("[KingExpansion] ✨ Signaling rat promotion!");
                    } catch (Exception e) {}
                    expansionComplete = true;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Get spawn location for current phase
     */
    private static MapLocation getSpawnLocation(int phase) {
        MapLocation myLoc = Globals.myLoc;
        int x = myLoc.x;
        int y = myLoc.y;

        switch (bestDirection) {
            case 1: // NORTH (spawn at y-2)
                switch (phase) {
                    case 1: return new MapLocation(x - 1, y - 2);
                    case 2: return new MapLocation(x, y - 2);
                    case 3: return new MapLocation(x + 1, y - 2);
                }
                break;
            case 0: // SOUTH (spawn at y+2)
                switch (phase) {
                    case 1: return new MapLocation(x - 1, y + 2);
                    case 2: return new MapLocation(x, y + 2);
                    case 3: return new MapLocation(x + 1, y + 2);
                }
                break;
            case 2: // EAST (spawn at x-2)
                switch (phase) {
                    case 1: return new MapLocation(x - 2, y - 1);
                    case 2: return new MapLocation(x - 2, y);
                    case 3: return new MapLocation(x - 2, y + 1);
                }
                break;
            case 3: // WEST (spawn at x+2)
                switch (phase) {
                    case 1: return new MapLocation(x + 2, y - 1);
                    case 2: return new MapLocation(x + 2, y);
                    case 3: return new MapLocation(x + 2, y + 1);
                }
                break;
        }
        return null;
    }

    /**
     * Baby rat checks for promotion signal
     */
    public static boolean checkForPromotion() throws GameActionException {
        if (Globals.myType != UnitType.BABY_RAT) return false;

        try {
            int signal = CommArray.getFlag(PROMOTE_SIGNAL);
            if (signal == 1 && Globals.rc.canBecomeRatKing()) {
                Globals.rc.becomeRatKing();
                CommArray.setFlag(PROMOTE_SIGNAL, 2); // Mark as promoted
                System.out.println("[KingExpansion] ✨ Rat #" + Globals.myID + " became RAT KING!");
                return true;
            }
        } catch (Exception e) {}

        return false;
    }

    private static Direction getDirectionFromInt(int dir) {
        switch (dir) {
            case 0: return Direction.SOUTH;
            case 1: return Direction.NORTH;
            case 2: return Direction.EAST;
            case 3: return Direction.WEST;
            default: return Direction.CENTER;
        }
    }

    private static String dirToString(int dir) {
        switch (dir) {
            case 0: return "SOUTH";
            case 1: return "NORTH";
            case 2: return "EAST";
            case 3: return "WEST";
            default: return "UNKNOWN";
        }
    }
}
