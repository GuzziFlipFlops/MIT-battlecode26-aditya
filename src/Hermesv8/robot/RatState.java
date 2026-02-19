package Hermesv8.robot;

import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;
import Hermesv8.util.DebugLogger;
import Hermesv8.nav.*;

public class RatState {
    private static RatStateType state = RatStateType.GATHER;
    private static RatStateType lastState = RatStateType.GATHER;

    public static RatStateType getState() {
        return state;
    }

    public static void setState(RatStateType newState) {
        if (newState != state) {
            DebugLogger.logState("RatState", state.toString(), newState.toString());
            lastState = state;
            state = newState;
        }
    }

    public static void update() throws GameActionException {
        int mode = getMode();
        int myCheese = Globals.myCheese;
        int myHealth = Globals.myHealth;

        if (myCheese > 0) {
            setState(RatStateType.RETURN);
            return;
        }

        boolean kingNeedsCheese = false;
        try {
            int teamCheese = Globals.rc.getAllCheese();
            int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 200;
            kingNeedsCheese = (teamCheese < minBuffer);
            int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
            if (flag == 2) kingNeedsCheese = true;
        } catch (GameActionException e) {}

        boolean enemyNearby = (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0);
        boolean lowHealth = (myHealth < 50);
        boolean outnumbered = isOutnumbered();

        if (kingNeedsCheese && !enemyNearby) {
            setState(RatStateType.GATHER);
            return;
        }

        if (lowHealth || outnumbered) {
            setState(RatStateType.RETREAT);
            return;
        }

        if (state == RatStateType.RETREAT) {
            if (!enemyNearby && myHealth >= 30) {
                if (mode == Constants.MODE_ATTACK && myCheese <= 1) {
                    setState(RatStateType.FIGHT);
                } else {
                    setState(RatStateType.GATHER);
                }
            }
            return;
        }

        if (state == RatStateType.RETURN) {
            if (myCheese == 0) {
                if (mode == Constants.MODE_ATTACK && !enemyNearby) {
                    setState(RatStateType.FIGHT);
                } else {
                    setState(RatStateType.GATHER);
                }
            }
            return;
        }

        if (state == RatStateType.GATHER) {
            if (myCheese > 0) {
                setState(RatStateType.RETURN);
            } else if (mode == Constants.MODE_ATTACK && myCheese == 0 && !enemyNearby) {
                setState(RatStateType.FIGHT);
            }
            return;
        }

        if (state == RatStateType.FIGHT) {
            if (myCheese >= Constants.CARRY_CAP) {
                setState(RatStateType.RETURN);
            } else if (mode != Constants.MODE_ATTACK) {
                setState(RatStateType.GATHER);
            } else if (lowHealth || outnumbered) {
                setState(RatStateType.RETREAT);
            }
            return;
        }
    }

    private static boolean isOutnumbered() {
        if (Globals.nearbyEnemies == null || Globals.nearbyEnemies.length == 0) return false;
        int enemyCount = Globals.nearbyEnemies.length;
        int allyCount = (Globals.nearbyAllies != null) ? Globals.nearbyAllies.length : 0;
        return enemyCount > allyCount + 1;
    }

    private static int getMode() {
        try {
            return CommArray.getFlag(CommArray.MODE_INDEX);
        } catch (GameActionException e) {
            return Constants.MODE_ECONOMY;
        }
    }
}
