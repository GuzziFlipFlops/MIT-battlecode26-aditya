package Hermes.robot;

import battlecode.common.*;
import Hermes.*;
import Hermes.comms.*;
import Hermes.nav.Navigator;
import Hermes.economy.TrapPlacer;

public class KingAttackCoordinator {

    public static void coordinate() throws GameActionException {
        CommArray.setFlag(CommArray.FLAG_ATTACK_CAT, 1);
        
        if (Globals.rc.isActionReady()) {
            RobotInfo target = selectTarget();
            
            if (target != null) {
                if (target.getType() == UnitType.CAT) {
                    CommArray.reportCat(target.getLocation());
                } else {
                    CommArray.reportEnemyKing(target.getLocation());
                }
                
                if (Globals.myLoc.isAdjacentTo(target.getLocation())) {
                    if (Globals.rc.canAttack(target.getLocation())) {
                        Globals.rc.attack(target.getLocation());
                        return;
                    }
                }
            }
            
            KingManager.trySpawnRats();
            
            if (Globals.isCooperation && Globals.globalCheese >= 10) {
                TrapPlacer.tryPlaceCatTrap();
            }
        }
        
        RobotInfo target = selectTarget();
        if (target != null && Globals.rc.isMovementReady()) {
            Navigator.navigateTo(target.getLocation());
        } else if (Globals.rc.isMovementReady()) {
            KingManager.moveTowardBetterPosition();
        }
    }

    private static RobotInfo selectTarget() {
        RobotInfo bestCat = null;
        int minCatHealth = Integer.MAX_VALUE;
        
        if (Globals.nearbyCats != null) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null && cat.getHealth() < minCatHealth) {
                    minCatHealth = cat.getHealth();
                    bestCat = cat;
                }
            }
        }
        
        if (bestCat != null) return bestCat;
        
        if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    return enemy;
                }
            }
        }
        
        return null;
    }
}
