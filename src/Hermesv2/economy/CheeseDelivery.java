package Hermesv2.economy;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.robot.*;
import Hermesv2.comms.*;
import Hermesv2.nav.*;

public class CheeseDelivery {

    public static void deliver() throws GameActionException {
        MapLocation targetKing = findNearestAlliedKing();
        
        if (targetKing == null) {
            targetKing = CommArray.getNearestAlliedKing();
            if (targetKing == null) {
                BabyRatState.setState(BabyRatStateType.SCOUTING);
                return;
            }
        }
        
        BabyRatState.setTargetKing(targetKing);
        
        int distToKing = Globals.myLoc.distanceSquaredTo(targetKing);
        
        if (distToKing <= GameConstants.CHEESE_DROP_RADIUS_SQUARED) {
            if (Globals.rc.canTransferCheese(targetKing, Globals.myCheese)) {
                int amount = Globals.myCheese;
                Globals.rc.transferCheese(targetKing, amount);
                Squeaker.squeakCheese(targetKing);
                BabyRatState.setState(BabyRatStateType.IDLE);
                return;
            }
        }
        
        Navigator.navigateTo(targetKing);
    }

    private static MapLocation findNearestAlliedKing() throws GameActionException {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo ally : Globals.nearbyAllies) {
            if (ally.getType() == UnitType.RAT_KING) {
                int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = ally.getLocation();
                }
            }
        }
        
        return nearest;
    }
}
