package Hermesv6.combat;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.robot.*;
import Hermesv6.nav.*;
import Hermesv6.comms.*;
public class FleeLogic {
    public static boolean shouldFlee() throws GameActionException {
        if (Globals.nearbyCats.length > 0 && Globals.myHealth < 50) {
            return true;
        }
        for (RobotInfo cat : Globals.nearbyCats) {
            if (Globals.myLoc.distanceSquaredTo(cat.getLocation()) <= 4) {
                return true;
            }
        }
        if (!Globals.isCooperation) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy.getType() == UnitType.RAT_KING) {
                    if (Globals.myLoc.distanceSquaredTo(enemy.getLocation()) <= 9) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static void flee() throws GameActionException {
        BabyRatState.setState(BabyRatStateType.FLEEING);
        MapLocation[] dangers = new MapLocation[Globals.nearbyCats.length + Globals.nearbyEnemies.length];
        int idx = 0;
        for (RobotInfo cat : Globals.nearbyCats) {
            dangers[idx++] = cat.getLocation();
        }
        if (!Globals.isCooperation) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                dangers[idx++] = enemy.getLocation();
            }
        }
        FleeNav.fleeFromMultiple(dangers);
        if (!shouldFlee()) {
            BabyRatState.setState(BabyRatStateType.IDLE);
        }
    }
}