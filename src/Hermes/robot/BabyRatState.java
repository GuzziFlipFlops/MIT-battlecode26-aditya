package Hermes.robot;

import battlecode.common.*;
import Hermes.*;
import Hermes.comms.*;
import Hermes.combat.*;

public class BabyRatState {

    private static BabyRatStateType state = BabyRatStateType.IDLE;
    private static MapLocation targetCheeseMine = null;
    private static MapLocation targetKing = null;

    public static void update() throws GameActionException {
        if (Globals.myCheese > 0 && state == BabyRatStateType.COLLECTING) {
            state = BabyRatStateType.DELIVERING;
        }
        
        if (Globals.myCheese == 0 && state == BabyRatStateType.DELIVERING) {
            state = BabyRatStateType.IDLE;
        }
    }

    public static BabyRatStateType getState() {
        return state;
    }

    public static void setState(BabyRatStateType newState) {
        state = newState;
    }

    public static MapLocation getTargetCheeseMine() {
        return targetCheeseMine;
    }

    public static void setTargetCheeseMine(MapLocation loc) {
        targetCheeseMine = loc;
    }

    public static MapLocation getTargetKing() {
        return targetKing;
    }

    public static void setTargetKing(MapLocation loc) {
        targetKing = loc;
    }

    public static void decideNextTask() throws GameActionException {
        if (Globals.rc.getCarrying() != null) {
            return;
        }
        
        // PRIORITY 1: Attack nearby enemy rat king immediately
        RobotInfo[] enemies = (Globals.nearbyEnemies != null) ? Globals.nearbyEnemies : new RobotInfo[0];
        
        for (int i = 0; i < enemies.length; i++) {
            if (enemies[i] != null && enemies[i].getType() == UnitType.RAT_KING) {
                state = BabyRatStateType.ATTACKING_ENEMY;
                targetKing = enemies[i].getLocation();
                return;
            }
        }
        
        // PRIORITY 2: Attack known enemy king location
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) {
            state = BabyRatStateType.ATTACKING_ENEMY;
            targetKing = enemyKing;
            return;
        }
        
        // PRIORITY 3: Deliver cheese if we have it (ALWAYS deliver - king always needs it)
        if (Globals.myCheese > 0) {
            MapLocation nearestKing = CommArray.getNearestAlliedKing();
            if (nearestKing != null) {
                // Check emergency flag
                boolean emergency = false;
                try {
                    int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
                    emergency = (flag == 2);
                } catch (GameActionException e) {
                }
                
                // Always deliver if we have cheese, but especially if emergency
                state = BabyRatStateType.DELIVERING;
                targetKing = nearestKing;
                return;
            }
        }
        
        // PRIORITY 4: Attack nearby cats (always, not just in cooperation)
        RobotInfo[] cats = (Globals.nearbyCats != null) ? Globals.nearbyCats : new RobotInfo[0];
        if (cats.length > 0 && Globals.myHealth > 50) {
            state = BabyRatStateType.ATTACKING_CAT;
            return;
        }
        
        // PRIORITY 5: Defend our king if threatened
        MapLocation nearestKing = CommArray.getNearestAlliedKing();
        if (nearestKing != null) {
            int distToKing = Globals.myLoc.distanceSquaredTo(nearestKing);
            if (distToKing <= 25) {
                boolean kingThreatened = false;
                for (int i = 0; i < cats.length && !kingThreatened; i++) {
                    if (cats[i] != null && cats[i].getLocation().distanceSquaredTo(nearestKing) <= 16) {
                        kingThreatened = true;
                    }
                }
                if (!Globals.isCooperation && !kingThreatened) {
                    for (int i = 0; i < enemies.length && !kingThreatened; i++) {
                        if (enemies[i] != null && enemies[i].getLocation().distanceSquaredTo(nearestKing) <= 16) {
                            kingThreatened = true;
                        }
                    }
                }
                if (kingThreatened) {
                    state = BabyRatStateType.DEFENDING;
                    return;
                }
            }
        }
        
        // PRIORITY 6: Attack any nearby enemies
        if (enemies.length > 0) {
            state = BabyRatStateType.ATTACKING_ENEMY;
            return;
        }
        
        // PRIORITY 7: Scout/explore to find enemy king (aggressive exploration)
        // Check if king is in emergency - if so, prioritize cheese collection
        boolean emergencyCheese = false;
        try {
            int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
            emergencyCheese = (flag == 2); // Flag 2 means critical cheese
        } catch (GameActionException e) {
        }
        
        // If emergency, prioritize cheese collection over scouting
        if (emergencyCheese) {
            if (Globals.numKnownCheeseMines > 0) {
                targetCheeseMine = CommArray.getNearestCheeseMine();
                if (targetCheeseMine != null) {
                    state = BabyRatStateType.COLLECTING;
                    return;
                }
            }
            // Even if no known mines, try to find cheese nearby
            state = BabyRatStateType.COLLECTING;
            return;
        }
        
        // PRIORITY 8: Scout toward enemy territory to find their king
        state = BabyRatStateType.SCOUTING;
    }
}
