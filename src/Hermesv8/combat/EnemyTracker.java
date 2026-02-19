package Hermesv8.combat;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;

/**
 * Tracks enemy king locations and movement patterns
 */
public class EnemyTracker {
    private static final int MAX_ENEMY_KINGS = 10;

    private static MapLocation[] enemyKings = new MapLocation[MAX_ENEMY_KINGS];
    private static int[] lastSeenRound = new int[MAX_ENEMY_KINGS];
    private static int[] confidence = new int[MAX_ENEMY_KINGS]; // 100 = just seen, decreases over time
    private static int numKings = 0;

    /**
     * Update enemy king tracking from sensor data
     */
    public static void update() throws GameActionException {
        // Scan for enemy kings in vision
        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    updateKing(enemy.getLocation(), 100);

                    // Report to team
                    try {
                        CommArray.reportEnemyKing(enemy.getLocation());
                    } catch (Exception e) {}
                }
            }
        }

        // Update from shared array
        MapLocation sharedKing = CommArray.getNearestEnemyKing();
        if (sharedKing != null) {
            updateKing(sharedKing, 50); // Medium confidence from shared data
        }

        // Decay confidence over time
        for (int i = 0; i < numKings; i++) {
            int roundsSinceSeen = Globals.roundNum - lastSeenRound[i];
            confidence[i] = Math.max(0, 100 - (roundsSinceSeen * 2));
        }
    }

    /**
     * Update or add enemy king location
     */
    private static void updateKing(MapLocation loc, int conf) {
        if (loc == null) return;

        // Check if king already tracked (within 5 tiles)
        for (int i = 0; i < numKings; i++) {
            if (enemyKings[i] != null && enemyKings[i].distanceSquaredTo(loc) <= 25) {
                enemyKings[i] = loc;
                lastSeenRound[i] = Globals.roundNum;
                confidence[i] = Math.max(confidence[i], conf);
                return;
            }
        }

        // Add new king
        if (numKings < MAX_ENEMY_KINGS) {
            enemyKings[numKings] = loc;
            lastSeenRound[numKings] = Globals.roundNum;
            confidence[numKings] = conf;
            numKings++;
        }
    }

    /**
     * Get nearest enemy king location
     */
    public static MapLocation getNearestEnemyKing() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        int bestConfidence = 0;

        for (int i = 0; i < numKings; i++) {
            if (enemyKings[i] != null && confidence[i] > 20) { // Only return if reasonably confident
                int dist = Globals.myLoc.distanceSquaredTo(enemyKings[i]);
                // Prefer closer kings, but also consider confidence
                int score = dist * 100 / Math.max(1, confidence[i]);

                if (score < minDist || (score == minDist && confidence[i] > bestConfidence)) {
                    minDist = score;
                    nearest = enemyKings[i];
                    bestConfidence = confidence[i];
                }
            }
        }

        return nearest;
    }

    /**
     * Get best enemy king target for attack
     */
    public static MapLocation getBestAttackTarget() {
        MapLocation bestTarget = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < numKings; i++) {
            if (enemyKings[i] != null && confidence[i] > 30) {
                int dist = Globals.myLoc.distanceSquaredTo(enemyKings[i]);

                // Score: high confidence and close distance are better
                int score = confidence[i] * 10 - dist;

                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = enemyKings[i];
                }
            }
        }

        return bestTarget;
    }

    /**
     * Mark enemy king as destroyed
     */
    public static void markDestroyed(MapLocation loc) {
        if (loc == null) return;

        for (int i = 0; i < numKings; i++) {
            if (enemyKings[i] != null && enemyKings[i].distanceSquaredTo(loc) <= 4) {
                // Remove by swapping with last element
                enemyKings[i] = enemyKings[numKings - 1];
                lastSeenRound[i] = lastSeenRound[numKings - 1];
                confidence[i] = confidence[numKings - 1];
                numKings--;
                break;
            }
        }
    }

    /**
     * Get all known enemy king locations
     */
    public static MapLocation[] getAllEnemyKings() {
        MapLocation[] result = new MapLocation[numKings];
        System.arraycopy(enemyKings, 0, result, 0, numKings);
        return result;
    }

    /**
     * Get number of tracked enemy kings
     */
    public static int getNumKings() {
        return numKings;
    }

    /**
     * Get confidence for nearest enemy king
     */
    public static int getNearestKingConfidence() {
        MapLocation nearest = getNearestEnemyKing();
        if (nearest == null) return 0;

        for (int i = 0; i < numKings; i++) {
            if (enemyKings[i] != null && enemyKings[i].equals(nearest)) {
                return confidence[i];
            }
        }

        return 0;
    }
}
