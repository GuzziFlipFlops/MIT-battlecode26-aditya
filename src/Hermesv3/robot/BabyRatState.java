package Hermesv3.robot;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.comms.*;
import Hermesv3.combat.*;

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
        
        
        MapLocation nearestKing = CommArray.getNearestAlliedKing();
        if (nearestKing != null) {
            int distToKing = Globals.myLoc.distanceSquaredTo(nearestKing);
            if (distToKing <= 25) {
                
                boolean kingInDanger = false;
                try {
                    int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
                    kingInDanger = (flag == 1 || flag == 2); 
                } catch (GameActionException e) {
                }
                
                
                if (!kingInDanger && Globals.nearbyEnemies != null) {
                    for (RobotInfo enemy : Globals.nearbyEnemies) {
                        if (enemy != null) {
                            int distToEnemy = nearestKing.distanceSquaredTo(enemy.getLocation());
                            if (distToEnemy <= 16) {
                                kingInDanger = true;
                                break;
                            }
                        }
                    }
                }
                
                if (kingInDanger) {
                    state = BabyRatStateType.DEFENDING;
                    return;
                }
            }
        }
        
        
        if (Globals.myCheese > 0) {
            if (nearestKing != null) {
                
                boolean emergency = false;
                try {
                    int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
                    emergency = (flag == 2);
                } catch (GameActionException e) {
                }
                
                
                state = BabyRatStateType.DELIVERING;
                targetKing = nearestKing;
                return;
            }
        }
        
        
        RobotInfo[] enemies = (Globals.nearbyEnemies != null) ? Globals.nearbyEnemies : new RobotInfo[0];
        RobotInfo[] cats = (Globals.nearbyCats != null) ? Globals.nearbyCats : new RobotInfo[0];
        for (int i = 0; i < enemies.length; i++) {
            if (enemies[i] != null && (enemies[i].getType() == UnitType.RAT_KING || enemies[i].getType() == UnitType.BABY_RAT)) {
                state = BabyRatStateType.ATTACKING_ENEMY;
                targetKing = (enemies[i].getType() == UnitType.RAT_KING) ? enemies[i].getLocation() : targetKing;
                return;
            }
        }
        
        
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) {
            state = BabyRatStateType.ATTACKING_ENEMY;
            targetKing = enemyKing;
            return;
        }
        
        
        if (targetKing != null) {
            state = BabyRatStateType.ATTACKING_ENEMY;
            return;
        }
        
        
        MapLocation kingForDefense = CommArray.getNearestAlliedKing();
        if (kingForDefense != null) {
            int distToKing = Globals.myLoc.distanceSquaredTo(kingForDefense);
            if (distToKing <= 25) {
                boolean kingThreatened = false;
                for (int i = 0; i < cats.length && !kingThreatened; i++) {
                    if (cats[i] != null && cats[i].getLocation().distanceSquaredTo(kingForDefense) <= 16) {
                        kingThreatened = true;
                    }
                }
                if (!Globals.isCooperation && !kingThreatened) {
                    for (int i = 0; i < enemies.length && !kingThreatened; i++) {
                        if (enemies[i] != null && enemies[i].getLocation().distanceSquaredTo(kingForDefense) <= 16) {
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
        
        
        if (enemies.length > 0) {
            state = BabyRatStateType.ATTACKING_ENEMY;
            return;
        }
        
        
        
        boolean emergencyCheese = false;
        try {
            int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
            emergencyCheese = (flag == 2); 
        } catch (GameActionException e) {
        }
        
        
        if (emergencyCheese) {
            if (Globals.numKnownCheeseMines > 0) {
                targetCheeseMine = CommArray.getNearestCheeseMine();
                if (targetCheeseMine != null) {
                    state = BabyRatStateType.COLLECTING;
                    return;
                }
            }
            
            state = BabyRatStateType.COLLECTING;
            return;
        }
        
        
        state = BabyRatStateType.SCOUTING;
    }
}
