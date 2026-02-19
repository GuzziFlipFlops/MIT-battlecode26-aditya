package Hermesv8.economy;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;

/**
 * Tracks cheese mine locations, respawn times, and optimal collection routes
 */
public class CheeseTracker {
    private static final int MAX_CHEESE_MINES = 20;
    private static final int CHEESE_RESPAWN_TIME = 40; // Cheese respawns every ~40 rounds

    private static MapLocation[] cheeseMines = new MapLocation[MAX_CHEESE_MINES];
    private static int[] lastCollectedRound = new int[MAX_CHEESE_MINES];
    private static int[] cheeseAmounts = new int[MAX_CHEESE_MINES];
    private static int numMines = 0;

    /**
     * Update cheese mine tracking from sensor data
     */
    public static void update() throws GameActionException {
        if (Globals.nearbyMapInfos == null) return;

        for (MapInfo info : Globals.nearbyMapInfos) {
            if (info == null) continue;

            MapLocation loc = info.getMapLocation();
            if (loc == null) continue;

            if (info.hasCheeseMine()) {
                addOrUpdateMine(loc, info.getCheeseAmount());

                // Report to team
                try {
                    CommArray.reportCheeseMine(loc);
                } catch (Exception e) {}
            }
        }

        // Update from shared array
        MapLocation sharedMine = CommArray.getNearestCheeseMine();
        if (sharedMine != null) {
            addOrUpdateMine(sharedMine, 0); // Unknown amount from shared data
        }
    }

    /**
     * Add or update a cheese mine in our tracking
     */
    private static void addOrUpdateMine(MapLocation loc, int amount) {
        // Check if mine already exists
        for (int i = 0; i < numMines; i++) {
            if (cheeseMines[i] != null && cheeseMines[i].equals(loc)) {
                if (amount == 0 && cheeseAmounts[i] > 0) {
                    // Cheese was collected
                    lastCollectedRound[i] = Globals.roundNum;
                }
                cheeseAmounts[i] = amount;
                return;
            }
        }

        // Add new mine
        if (numMines < MAX_CHEESE_MINES) {
            cheeseMines[numMines] = loc;
            cheeseAmounts[numMines] = amount;
            lastCollectedRound[numMines] = (amount == 0) ? Globals.roundNum : 0;
            numMines++;
        }
    }

    /**
     * Get best cheese target considering distance and respawn prediction
     */
    public static MapLocation getBestCheeseTarget() {
        MapLocation bestTarget = null;
        int bestScore = Integer.MIN_VALUE;

        // First priority: visible cheese on ground
        if (Globals.nearbyMapInfos != null) {
            for (MapInfo info : Globals.nearbyMapInfos) {
                if (info != null && info.getCheeseAmount() > 0) {
                    MapLocation loc = info.getMapLocation();
                    if (loc != null) {
                        int dist = Globals.myLoc.distanceSquaredTo(loc);
                        int score = (info.getCheeseAmount() * 100) - dist;

                        if (score > bestScore) {
                            bestScore = score;
                            bestTarget = loc;
                        }
                    }
                }
            }
        }

        if (bestTarget != null) return bestTarget;

        // Second priority: cheese mines likely to have respawned
        for (int i = 0; i < numMines; i++) {
            if (cheeseMines[i] == null) continue;

            int roundsSinceCollected = Globals.roundNum - lastCollectedRound[i];
            boolean likelyRespawned = (roundsSinceCollected >= CHEESE_RESPAWN_TIME) || (cheeseAmounts[i] > 0);

            if (likelyRespawned) {
                int dist = Globals.myLoc.distanceSquaredTo(cheeseMines[i]);
                // Score: closer is better, recently respawned is better
                int score = 1000 - dist + (roundsSinceCollected - CHEESE_RESPAWN_TIME);

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = cheeseMines[i];
                }
            }
        }

        if (bestTarget != null) return bestTarget;

        // Third priority: any known cheese mine
        for (int i = 0; i < numMines; i++) {
            if (cheeseMines[i] != null) {
                int dist = Globals.myLoc.distanceSquaredTo(cheeseMines[i]);
                int score = -dist;

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = cheeseMines[i];
                }
            }
        }

        return bestTarget;
    }

    /**
     * Find nearest cheese location (visible or from mines)
     */
    public static MapLocation getNearestCheese() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;

        // Check visible cheese
        if (Globals.nearbyMapInfos != null) {
            for (MapInfo info : Globals.nearbyMapInfos) {
                if (info != null && info.getCheeseAmount() > 0) {
                    MapLocation loc = info.getMapLocation();
                    if (loc != null) {
                        int dist = Globals.myLoc.distanceSquaredTo(loc);
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = loc;
                        }
                    }
                }
            }
        }

        if (nearest != null) return nearest;

        // Check known mines with cheese
        for (int i = 0; i < numMines; i++) {
            if (cheeseMines[i] != null && cheeseAmounts[i] > 0) {
                int dist = Globals.myLoc.distanceSquaredTo(cheeseMines[i]);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = cheeseMines[i];
                }
            }
        }

        return nearest;
    }

    /**
     * Get all known cheese mine locations
     */
    public static MapLocation[] getAllCheeseMines() {
        MapLocation[] result = new MapLocation[numMines];
        System.arraycopy(cheeseMines, 0, result, 0, numMines);
        return result;
    }

    /**
     * Get number of known cheese mines
     */
    public static int getNumMines() {
        return numMines;
    }
}
