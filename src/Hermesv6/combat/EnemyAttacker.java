package Hermesv6.combat;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.robot.*;
import Hermesv6.nav.*;
import Hermesv6.comms.*;
public class EnemyAttacker {
    public static void attack() throws GameActionException {
        RobotInfo target = null;
        boolean squeakedForKing = false;
        
        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    target = enemy;
                    try {
                        CommArray.reportEnemyKing(enemy.getLocation());
                        Squeaker.squeakAttack(enemy.getLocation());
                        squeakedForKing = true;
                        System.out.println("[DEBUG EnemyAttacker] Rat #" + Globals.myID + " squeaked for VISIBLE enemy king at " + enemy.getLocation());
                    } catch (GameActionException e) {
                        System.out.println("[DEBUG EnemyAttacker] ERROR squeaking for king: " + e.getMessage());
                    }
                    break;
                }
            }
        }
        
        MapLocation enemyKingLoc = CommArray.getNearestEnemyKing();
        if (!squeakedForKing && enemyKingLoc != null && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            boolean hasVisibleEnemy = false;
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null) {
                    hasVisibleEnemy = true;
                    break;
                }
            }
            if (hasVisibleEnemy) {
                try {
                    Squeaker.squeakAttack(enemyKingLoc);
                    squeakedForKing = true;
                    System.out.println("[DEBUG EnemyAttacker] Rat #" + Globals.myID + " squeaked for KNOWN enemy king at " + enemyKingLoc);
                } catch (GameActionException e) {
                    System.out.println("[DEBUG EnemyAttacker] ERROR squeaking for known king: " + e.getMessage());
                }
            }
        }
        
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
        
        if (target == null && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.BABY_RAT) {
                    target = enemy;
                    if (!squeakedForKing && enemyKingLoc != null) {
                        try {
                            Squeaker.squeakAttack(enemy.getLocation());
                            if (Globals.roundNum % 20 == 0) {
                                System.out.println("[DEBUG EnemyAttacker] Rat #" + Globals.myID + " squeaked for baby rat at " + enemy.getLocation() + " (enemy king known)");
                            }
                        } catch (GameActionException e) {
                            if (Globals.roundNum % 20 == 0) {
                                System.out.println("[DEBUG EnemyAttacker] ERROR squeaking for baby rat: " + e.getMessage());
                            }
                        }
                    }
                    break;
                }
            }
        }
        
        if (target == null && enemyKingLoc != null) {
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
                int cheeseToSpend = target.getType() == UnitType.RAT_KING ? 
                    Math.min(Globals.myCheese, 21) : DamageCalculator.getCheeseForEnemy(target);
                Globals.rc.attack(targetLoc, cheeseToSpend);
                if (target.getType() == UnitType.RAT_KING && !squeakedForKing) {
                    try {
                        Squeaker.squeakAttack(targetLoc);
                    } catch (GameActionException e) {}
                }
                return;
            }
        }
        
        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(targetLoc);
        }
    }
}