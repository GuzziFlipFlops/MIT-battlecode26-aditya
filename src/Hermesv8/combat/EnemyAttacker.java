package Hermesv8.combat;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.nav.*;
import Hermesv8.comms.*;
public class EnemyAttacker {
    public static void attack() throws GameActionException {
        RobotInfo target = null;
        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    target = enemy;
                    try {
                        CommArray.reportEnemyKing(enemy.getLocation());
                        Squeaker.squeakAttack(enemy.getLocation());
                    } catch (GameActionException e) {}
                    break;
                }
            }
        }
        if (target == null && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            target = TargetSelector.selectEnemyTarget();
        }
        MapLocation enemyKingLoc = CommArray.getNearestEnemyKing();
        if (target == null && enemyKingLoc != null) {
            if (Globals.myLoc.isAdjacentTo(enemyKingLoc)) {
                if (Globals.rc.isActionReady() && Globals.rc.canAttack(enemyKingLoc)) {
                    int cheeseToSpend = Math.min(Globals.myCheese, 21);
                    Globals.rc.attack(enemyKingLoc, cheeseToSpend);
                    return;
                }
            }
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(enemyKingLoc);
                return;
            }
        }
        if (target == null) {
            if (Globals.rc.isMovementReady()) {
                MapLocation myLoc = Globals.myLoc;
                int oppositeX = Globals.mapWidth - 1 - myLoc.x;
                int oppositeY = Globals.mapHeight - 1 - myLoc.y;
                MapLocation oppositeSide = new MapLocation(oppositeX, oppositeY);
                Navigator.navigateTo(oppositeSide);
                return;
            }
            return;
        }
        MapLocation targetLoc = target.getLocation();
        if (Globals.myLoc.isAdjacentTo(targetLoc)) {
            if (Globals.rc.isActionReady() && Globals.rc.canAttack(targetLoc)) {
                int cheeseToSpend = DamageCalculator.getCheeseForEnemy(target);
                Globals.rc.attack(targetLoc, cheeseToSpend);
                return;
            }
        }
        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(targetLoc);
        }
    }
}