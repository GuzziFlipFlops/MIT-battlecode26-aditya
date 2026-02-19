package Hermes.combat;

import battlecode.common.*;
import Hermes.*;

public class Thrower {

    public static boolean tryThrowRat() throws GameActionException {
        if (!Globals.rc.canThrowRat()) return false;
        Globals.rc.throwRat();
        return true;
    }

    public static boolean tryThrowAtCat() throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying == null) return false;
        
        RobotInfo bestCat = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo cat : Globals.nearbyCats) {
            int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
            if (dist < minDist) {
                minDist = dist;
                bestCat = cat;
            }
        }
        
        if (bestCat == null) return false;
        
        Direction toCat = Globals.myLoc.directionTo(bestCat.getLocation());
        
        if (Globals.myDir != toCat && Globals.rc.canTurn()) {
            Globals.rc.turn(toCat);
        }
        
        if (Globals.rc.canThrowRat()) {
            Globals.rc.throwRat();
            return true;
        }
        
        return false;
    }

    public static boolean tryThrowAtEnemyKing() throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying == null) return false;
        if (carrying.getTeam() == Globals.myTeam) return false;
        
        RobotInfo bestKing = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo enemy : Globals.nearbyEnemies) {
            if (enemy.getType() == UnitType.RAT_KING) {
                int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    bestKing = enemy;
                }
            }
        }
        
        if (bestKing == null) return false;
        
        Direction toKing = Globals.myLoc.directionTo(bestKing.getLocation());
        
        if (Globals.myDir != toKing && Globals.rc.canTurn()) {
            Globals.rc.turn(toKing);
        }
        
        if (Globals.rc.canThrowRat()) {
            Globals.rc.throwRat();
            return true;
        }
        
        return false;
    }

    public static boolean tryDropRat(Direction dir) throws GameActionException {
        if (Globals.rc.canDropRat(dir)) {
            Globals.rc.dropRat(dir);
            return true;
        }
        return false;
    }
}
