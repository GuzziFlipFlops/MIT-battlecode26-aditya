package Hermes.combat;

import battlecode.common.*;
import Hermes.*;
import Hermes.robot.*;
import Hermes.nav.*;
import Hermes.strategy.*;
import Hermes.comms.*;

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
        
        if (target == null && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            target = TargetSelector.selectEnemyTarget();
        }
        
        MapLocation enemyKingLoc = CommArray.getNearestEnemyKing();
        if (target == null && enemyKingLoc != null) {
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(enemyKingLoc);
                return;
            }
        }
        
        if (target == null) {
            if (Globals.rc.isMovementReady()) {
                if (enemyKingLoc != null) {
                    Navigator.navigateTo(enemyKingLoc);
                    return;
                }
                
                MapLocation myLoc = Globals.myLoc;
                MapLocation center = Globals.mapCenter;
                int dx = myLoc.x - center.x;
                int dy = myLoc.y - center.y;
                MapLocation enemySpawn = new MapLocation(center.x - dx, center.y - dy);
                
                if (enemySpawn.x < 0) enemySpawn = new MapLocation(0, enemySpawn.y);
                if (enemySpawn.x >= Globals.mapWidth) enemySpawn = new MapLocation(Globals.mapWidth - 1, enemySpawn.y);
                if (enemySpawn.y < 0) enemySpawn = new MapLocation(enemySpawn.x, 0);
                if (enemySpawn.y >= Globals.mapHeight) enemySpawn = new MapLocation(enemySpawn.x, Globals.mapHeight - 1);
                
                Navigator.navigateTo(enemySpawn);
                return;
            }
            return;
        }
        
        if (Globals.isCooperation) {
            try {
                CommArray.signalBackstab();
            } catch (GameActionException e) {
            }
        }
        
        MapLocation targetLoc = target.getLocation();
        
        if (Globals.rc.isActionReady() && Ratnapper.canRatnap(target)) {
            if (Ratnapper.tryRatnap(target)) {
                return;
            }
        }
        
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
