package Hermesv2.robot;

import battlecode.common.*;
import Hermesv2.*;

public class RatKingState {

    private static RatKingStateType state = RatKingStateType.MANAGING;
    private static int consecutiveDangerTurns = 0;
    private static int lastSpawnRound = 0;
    private static int totalSpawned = 0;

    public static void updateAssessment() throws GameActionException {
        boolean inDanger = isInDanger();
        
        if (inDanger) {
            consecutiveDangerTurns++;
            if (consecutiveDangerTurns > 3) {
                state = RatKingStateType.RETREATING;
            } else {
                state = RatKingStateType.DEFENDING;
            }
        } else {
            consecutiveDangerTurns = 0;
            if (shouldCoordinateAttack()) {
                state = RatKingStateType.ATTACKING;
            } else {
                state = RatKingStateType.MANAGING;
            }
        }
    }

    private static boolean isInDanger() {
        if (Globals.nearbyCats != null) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                    if (dist <= 16) {
                        return true;
                    }
                }
            }
        }
        
        if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                    if (dist <= 25) {
                        return true;
                    }
                }
            }
        }
        
        if (Globals.myHealth < 100) {
            return true;
        }
        
        return false;
    }

    private static boolean shouldCoordinateAttack() {
        int allyCount = (Globals.nearbyAllies != null) ? Globals.nearbyAllies.length : 0;
        int catCount = (Globals.nearbyCats != null) ? Globals.nearbyCats.length : 0;
        
        if (catCount > 0 && allyCount >= catCount * 5) {
            return true;
        }
        
        if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING && enemy.getHealth() < 200) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public static RatKingStateType getState() {
        return state;
    }

    public static int getLastSpawnRound() {
        return lastSpawnRound;
    }

    public static void setLastSpawnRound(int round) {
        lastSpawnRound = round;
    }

    public static int getTotalSpawned() {
        return totalSpawned;
    }

    public static void incrementSpawned() {
        totalSpawned++;
    }
}
