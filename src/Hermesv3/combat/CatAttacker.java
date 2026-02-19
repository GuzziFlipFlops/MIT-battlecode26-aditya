package Hermesv3.combat;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.robot.*;
import Hermesv3.nav.*;
import Hermesv3.comms.*;

public class CatAttacker {

    public static void attack() throws GameActionException {
        if (Globals.nearbyCats == null || Globals.nearbyCats.length == 0) {
            BabyRatState.setState(BabyRatStateType.IDLE);
            return;
        }
        
        RobotInfo targetCat = null;
        int minDist = Integer.MAX_VALUE;
        int lowestHealth = Integer.MAX_VALUE;
        
        for (RobotInfo cat : Globals.nearbyCats) {
            if (cat == null) continue;
            int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
            int health = cat.getHealth();
            
            if (health < lowestHealth || (health == lowestHealth && dist < minDist)) {
                lowestHealth = health;
                minDist = dist;
                targetCat = cat;
            }
        }
        
        if (targetCat == null) return;
        
        MapLocation catLoc = targetCat.getLocation();
        
        if (Globals.isCooperation && Globals.rc.isActionReady() && Globals.nearbyEnemies != null) {
            MapLocation enemyKing = null;
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    enemyKing = enemy.getLocation();
                    break;
                }
            }
            if (enemyKing != null && catLoc.distanceSquaredTo(enemyKing) > catLoc.distanceSquaredTo(Globals.myLoc)) {
                try {
                    Squeaker.squeakAttack(enemyKing);
                } catch (GameActionException e) {
                }
            }
        }
        
        if (Globals.myLoc.isAdjacentTo(catLoc)) {
            if (Globals.rc.isActionReady() && Globals.rc.canAttack(catLoc)) {
                int cheeseToSpend = DamageCalculator.getCheeseForCat();
                if (cheeseToSpend > 0 && Globals.globalCheese < cheeseToSpend * 3) {
                    cheeseToSpend = 0;
                }
                int damage = DamageCalculator.calculateDamage(cheeseToSpend);
                Globals.rc.attack(catLoc, cheeseToSpend);
                
                if (Globals.myType == UnitType.RAT_KING && Clock.getBytecodesLeft() > 500) {
                    try {
                        CommArray.addCatDamage(damage);
                    } catch (GameActionException e) {
                    }
                }
                
                if (Globals.myHealth < 40 && Globals.rc.isMovementReady()) {
                    FleeNav.fleeFrom(catLoc);
                }
                return;
            }
        }
        
        if (Globals.myHealth < 50 && minDist <= 4 && Globals.rc.isMovementReady()) {
            FleeNav.fleeFrom(catLoc);
            return;
        }
        
        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(catLoc);
        }
    }
}
