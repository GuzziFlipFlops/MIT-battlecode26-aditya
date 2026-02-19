package Hermesv6.robot;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.comms.*;
import Hermesv6.strategy.*;
import Hermesv6.economy.*;
public class RatKingRunner {
    public static void run() {
        try {
            if (Globals.roundNum % 10 == 0) {
                System.out.println("[Hermesv6] Turn " + Globals.roundNum + ": RatKing at " + Globals.myLoc + ", Cheese: " + Globals.rc.getAllCheese());
            }
            int availableCheese = Globals.rc.getAllCheese();
            if (availableCheese < Constants.RATKING_CHEESE_CONSUMPTION) {
                try {
                    CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 2);
                } catch (GameActionException e) {
                }
            }
            try {
                CommArray.reportAlliedKing(Globals.myLoc);
            } catch (GameActionException e) {
                if (Globals.roundNum % 20 == 0) {
                    System.out.println("[DEBUG] King #" + Globals.myID + " ERROR reporting location: " + e.getMessage());
                }
            } catch (Exception e) {
                if (Globals.roundNum % 20 == 0) {
                    System.out.println("[DEBUG] King #" + Globals.myID + " EXCEPTION reporting location: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            try {
                RatKingState.updateAssessment();
            } catch (Exception e) {
            }
            try {
                StrategicDecisions.make();
            } catch (GameActionException e) {
            }
            try {
                switch (RatKingState.getState()) {
                    case MANAGING:
                        KingManager.manage();
                        break;
                    case DEFENDING:
                        KingDefender.defend();
                        break;
                    case RETREATING:
                        KingRetreater.retreat();
                        break;
                    case ATTACKING:
                        KingAttackCoordinator.coordinate();
                        break;
                }
            } catch (GameActionException e) {
            }
            try {
                KingSenseReporter.report();
            } catch (GameActionException e) {
            }
        } catch (Exception e) {
            System.out.println("[Hermesv6] ERROR in RatKingRunner: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static boolean tryFormationBomb() throws GameActionException {
        if (!Globals.rc.canBecomeRatKing()) return false;
        if (Globals.globalCheese < Constants.RAT_KING_UPGRADE_COST) return false;
        int allyCount = 0;
        int enemyCount = 0;
        boolean hasEnemyInZone = false;
        MapLocation enemyKingNearby = null;
        if (Globals.nearbyAllies != null) {
            for (int i = 0; i < Globals.nearbyAllies.length && i < 10; i++) {
                RobotInfo ally = Globals.nearbyAllies[i];
                if (ally != null && ally.getType() == UnitType.BABY_RAT) {
                    int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                    if (dist <= 2) {
                        allyCount++;
                        if (allyCount >= 7) break;
                    }
                }
            }
        }
        if (Globals.nearbyEnemies != null) {
            for (int i = 0; i < Globals.nearbyEnemies.length && i < 10; i++) {
                RobotInfo enemy = Globals.nearbyEnemies[i];
                if (enemy != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                    if (dist <= 2) {
                        enemyCount++;
                        hasEnemyInZone = true;
                        if (enemy.getType() == UnitType.RAT_KING) {
                            enemyKingNearby = enemy.getLocation();
                        }
                    }
                }
            }
        }
        if (hasEnemyInZone && allyCount >= 5) {
            if (enemyKingNearby != null) {
                Globals.rc.becomeRatKing();
                return true;
            }
            if (enemyCount >= 2) {
                Globals.rc.becomeRatKing();
                return true;
            }
            if (enemyCount >= 1 && allyCount >= 6) {
                Globals.rc.becomeRatKing();
                return true;
            }
        }
        MapLocation ourKing = CommArray.getNearestAlliedKing();
        if (ourKing != null && Globals.myLoc.distanceSquaredTo(ourKing) <= 9) {
            if (hasEnemyInZone && allyCount >= 5) {
                Globals.rc.becomeRatKing();
                return true;
            }
        }
        if (allyCount >= 7) {
            Globals.rc.becomeRatKing();
            return true;
        }
        return false;
    }
}