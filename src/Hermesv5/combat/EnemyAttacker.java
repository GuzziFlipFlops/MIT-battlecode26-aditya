package Hermesv5.combat;
import battlecode.common.*;
import Hermesv5.*;
import Hermesv5.robot.*;
import Hermesv5.nav.*;
import Hermesv5.nav.AStarPathfinder;
import Hermesv5.nav.Mover;
import Hermesv5.strategy.*;
import Hermesv5.comms.*;
public class EnemyAttacker {
    public static void attack() throws GameActionException {
        RobotInfo target = null;
        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    target = enemy;
                    break;
                }
            }
        }
        MapLocation enemyKingLoc = CommArray.getNearestEnemyKing();
        if (target == null && enemyKingLoc != null && Globals.rc.isMovementReady()) {
            Navigator.navigateTo(enemyKingLoc);
            return;
        }
        if (target == null && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            target = TargetSelector.selectEnemyTarget();
        }
        if (target == null) {
            if (Globals.rc.isMovementReady()) {
                if (enemyKingLoc != null) {
                    Navigator.navigateTo(enemyKingLoc);
                    return;
                }
                MapLocation myLoc = Globals.myLoc;
                int oppositeX = Globals.mapWidth - 1 - myLoc.x;
                int oppositeY = Globals.mapHeight - 1 - myLoc.y;
                MapLocation oppositeSide = new MapLocation(oppositeX, oppositeY);
                if (Clock.getBytecodesLeft() > 2000) {
                    Direction dir = AStarPathfinder.findBestDirection(oppositeSide);
                    if (dir != null && dir != Direction.CENTER) {
                        if (Mover.tryMove(dir)) {
                            return;
                        }
                    }
                }
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