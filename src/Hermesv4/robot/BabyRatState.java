package Hermesv4.robot;

import battlecode.common.*;
import Hermesv4.*;
import Hermesv4.comms.*;
import Hermesv4.combat.*;

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
        
        boolean earlyGame = Globals.roundNum < 50;
        
        try {
            int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
            if (flag == 2 || flag == 1) {
                state = BabyRatStateType.COLLECTING;
                return;
            }
        } catch (GameActionException e) {
        }
        
        MapLocation nearestKingForCheese = CommArray.getNearestAlliedKing();
        if (nearestKingForCheese != null) {
            try {
                int distToKing = Globals.myLoc.distanceSquaredTo(nearestKingForCheese);
                if (distToKing <= 400) {
                    RobotInfo kingInfo = Globals.rc.senseRobotAtLocation(nearestKingForCheese);
                    if (kingInfo != null && kingInfo.getType() == UnitType.RAT_KING) {
                        int kingCheese = Globals.rc.getAllCheese();
                        int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 150;
                        if (kingCheese < minBuffer) {
                            state = BabyRatStateType.COLLECTING;
                            return;
                        }
                    }
                }
            } catch (GameActionException e) {
            }
        }
        
        RobotInfo[] enemies = (Globals.nearbyEnemies != null) ? Globals.nearbyEnemies : new RobotInfo[0];
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
        
        if (earlyGame && Globals.roundNum < 30) {
            state = BabyRatStateType.SCOUTING;
            return;
        }
        
        MapLocation nearestKing = CommArray.getNearestAlliedKing();
        if (nearestKing != null) {
            try {
                RobotInfo kingInfo = Globals.rc.senseRobotAtLocation(nearestKing);
                if (kingInfo != null && kingInfo.getType() == UnitType.RAT_KING) {
                    int kingCheese = Globals.rc.getAllCheese();
                    int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 120;
                    if (kingCheese < minBuffer) {
                        state = BabyRatStateType.COLLECTING;
                        return;
                    }
                }
            } catch (GameActionException e) {
            }
        }
        
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
                state = BabyRatStateType.DELIVERING;
                targetKing = nearestKing;
                return;
            }
        }
        
        if (nearestKing != null) {
            try {
                int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
                if (flag == 2) {
                    state = BabyRatStateType.COLLECTING;
                    return;
                }
                
                try {
                    RobotInfo kingInfo = Globals.rc.senseRobotAtLocation(nearestKing);
                    if (kingInfo != null && kingInfo.getType() == UnitType.RAT_KING) {
                        int kingCheese = Globals.rc.getAllCheese();
                        int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 30;
                        if (kingCheese < minBuffer && Globals.myLoc.distanceSquaredTo(nearestKing) <= 100) {
                            state = BabyRatStateType.COLLECTING;
                            return;
                        }
                    }
                } catch (GameActionException e) {
                }
            } catch (GameActionException e) {
            }
        }
        
        
        RobotInfo[] cats = (Globals.nearbyCats != null) ? Globals.nearbyCats : new RobotInfo[0];
        for (int i = 0; i < enemies.length; i++) {
            if (enemies[i] != null && (enemies[i].getType() == UnitType.RAT_KING || enemies[i].getType() == UnitType.BABY_RAT)) {
                state = BabyRatStateType.ATTACKING_ENEMY;
                targetKing = (enemies[i].getType() == UnitType.RAT_KING) ? enemies[i].getLocation() : targetKing;
                return;
            }
        }
        
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
