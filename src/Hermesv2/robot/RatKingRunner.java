package Hermesv2.robot;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.comms.*;
import Hermesv2.strategy.*;
import Hermesv2.economy.*;

public class RatKingRunner {

    public static void run() {
        try {
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
        }
    }
    
    private static boolean tryFormationBomb() throws GameActionException {
        if (!Globals.rc.canBecomeRatKing()) return false;
        if (Globals.globalCheese < Constants.RAT_KING_UPGRADE_COST) return false;
        
        int allyCount = 0;
        boolean hasEnemyInZone = false;
        
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
        
        if (allyCount >= 7) {
            Globals.rc.becomeRatKing();
            return true;
        }
        
        if (!Globals.isCooperation && allyCount >= 5 && Globals.nearbyEnemies != null) {
            for (int i = 0; i < Globals.nearbyEnemies.length && i < 5; i++) {
                RobotInfo enemy = Globals.nearbyEnemies[i];
                if (enemy != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                    if (dist <= 2) {
                        hasEnemyInZone = true;
                        break;
                    }
                }
            }
            if (hasEnemyInZone) {
                Globals.rc.becomeRatKing();
                return true;
            }
        }
        return false;
    }
}
