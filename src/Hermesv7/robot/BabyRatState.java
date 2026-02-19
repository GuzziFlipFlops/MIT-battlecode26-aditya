package Hermesv7.robot;
import battlecode.common.*;
import Hermesv7.*;
import Hermesv7.comms.*;
public class BabyRatState {
    private static BabyRatStateType state = BabyRatStateType.IDLE;
    private static MapLocation targetCheeseMine = null;
    private static MapLocation targetKing = null;
    public static void update() throws GameActionException {
        if (Globals.myCheese > 0) {
            state = BabyRatStateType.DELIVERING;
            MapLocation nearestKing = null;
            if (Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        nearestKing = ally.getLocation();
                        break;
                    }
                }
            }
            if (nearestKing == null) {
                nearestKing = CommArray.getNearestAlliedKing();
            }
            if (nearestKing != null) {
                targetKing = nearestKing;
            }
        } else if (state == BabyRatStateType.DELIVERING) {
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
        if (nearestKing == null && Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                    nearestKing = ally.getLocation();
                    break;
                }
            }
        }
        if (Globals.myCheese > 0) {
            if (nearestKing == null && Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        nearestKing = ally.getLocation();
                        break;
                    }
                }
            }
            if (nearestKing != null) {
                state = BabyRatStateType.DELIVERING;
                targetKing = nearestKing;
                return;
            }
            state = BabyRatStateType.DELIVERING;
            return;
        }
        int teamCheese = Globals.rc.getAllCheese();
        boolean emergencyMode = (teamCheese <= 600);
        int numKings = Math.max(1, Globals.numKnownAlliedKings);
        int cheeseNeededPerRound = Constants.RATKING_CHEESE_CONSUMPTION * numKings;
        int safeBuffer = cheeseNeededPerRound * 200;
        boolean lowCheese = (teamCheese < safeBuffer);
        boolean kingNeedsCheese = emergencyMode || lowCheese;
        if (Globals.myCheese == 0) {
            if (!kingNeedsCheese && teamCheese > safeBuffer) {
            } else {
                state = BabyRatStateType.COLLECTING;
                return;
            }
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
        if (kingNeedsCheese) {
            state = BabyRatStateType.COLLECTING;
            return;
        }
        RobotInfo[] enemies = (Globals.nearbyEnemies != null) ? Globals.nearbyEnemies : new RobotInfo[0];
        if (!kingNeedsCheese) {
            for (int i = 0; i < enemies.length; i++) {
                if (enemies[i] != null && (enemies[i].getType() == UnitType.RAT_KING || enemies[i].getType() == UnitType.BABY_RAT)) {
                    state = BabyRatStateType.ATTACKING_ENEMY;
                    targetKing = (enemies[i].getType() == UnitType.RAT_KING) ? enemies[i].getLocation() : targetKing;
                    return;
                }
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
        if (kingNeedsCheese) {
            state = BabyRatStateType.COLLECTING;
            return;
        }
        if (nearestKing != null) {
            try {
                RobotInfo kingInfo = Globals.rc.senseRobotAtLocation(nearestKing);
                if (kingInfo != null && kingInfo.getType() == UnitType.RAT_KING) {
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
        if (nearestKing != null) {
            try {
                int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
                if (flag == 2) {
                    state = BabyRatStateType.COLLECTING;
                    return;
                }
                if (kingNeedsCheese) {
                    state = BabyRatStateType.COLLECTING;
                    return;
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
        if (kingNeedsCheese) {
            state = BabyRatStateType.COLLECTING;
            return;
        }
        if (nearestKing != null) {
            try {
                RobotInfo kingInfo = Globals.rc.senseRobotAtLocation(nearestKing);
                if (kingInfo != null && kingInfo.getType() == UnitType.RAT_KING) {
                }
            } catch (GameActionException e) {
            }
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