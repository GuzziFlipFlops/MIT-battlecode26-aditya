package Hermesv3.robot;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.comms.*;
import Hermesv3.strategy.*;
import Hermesv3.economy.*;

public class RatKingRunner {

    public static void run() {
        try {
            
            if (Globals.roundNum % 10 == 0) {
                System.out.println("[Hermesv3] Turn " + Globals.roundNum + ": RatKing at " + Globals.myLoc + ", Cheese: " + Globals.rc.getAllCheese());
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
            
            while (Clock.getBytecodesLeft() > 1000) {
                boolean actionTaken = false;
                
                if (Globals.rc.isActionReady()) {
                    try {
                        if (KingManager.trySpawnRats()) {
                            actionTaken = true;
                            continue;
                        }
                    } catch (GameActionException e) {
                    }
                    
                    try {
                        if (tryFormationBomb()) {
                            actionTaken = true;
                            continue;
                        }
                    } catch (GameActionException e) {
                    }
                    
                    try {
                        int phase = GamePhase.calculate();
                        if (Globals.isCooperation && Globals.roundNum >= 50 && Globals.roundNum <= 300 && phase <= GamePhase.PHASE_MID) {
                            if (TrapPlacer.tryPlaceCatTrap()) {
                                actionTaken = true;
                                continue;
                            }
                        } else if (!Globals.isCooperation || Globals.roundNum > 300 || phase >= GamePhase.PHASE_LATE) {
                            if (TrapPlacer.tryPlaceRatTrap()) {
                                actionTaken = true;
                                continue;
                            }
                        }
                    } catch (GameActionException e) {
                    }
                }
                
                if (!actionTaken) break;
            }
            
            try {
                KingSenseReporter.report();
            } catch (GameActionException e) {
            }
        } catch (Exception e) {
            
            System.out.println("[Hermesv3] ERROR in RatKingRunner: " + e.getMessage());
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
