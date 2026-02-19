package Hermesv2.combat;

import battlecode.common.*;
import Hermesv2.*;

public class Ratnapper {

    public static boolean canRatnap(RobotInfo target) {
        if (target.getType() != UnitType.BABY_RAT) return false;
        
        MapLocation targetLoc = target.getLocation();
        if (!Globals.myLoc.isAdjacentTo(targetLoc)) return false;
        
        Direction targetFacing = target.getDirection();
        Direction toUs = targetLoc.directionTo(Globals.myLoc);
        
        boolean facingAway = !isInVisionCone(targetFacing, toUs);
        boolean lowerHealth = target.getHealth() < Globals.myHealth;
        boolean isAlly = target.getTeam() == Globals.myTeam;
        
        return facingAway || lowerHealth || isAlly;
    }

    private static boolean isInVisionCone(Direction facing, Direction toCheck) {
        if (facing == Direction.CENTER) return true;
        if (toCheck == Direction.CENTER) return true;
        
        Direction current = facing;
        for (int i = 0; i < 3; i++) {
            if (current == toCheck) return true;
            current = current.rotateLeft();
        }
        current = facing.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (current == toCheck) return true;
            current = current.rotateRight();
        }
        
        return false;
    }

    public static boolean tryRatnap(RobotInfo target) throws GameActionException {
        if (target == null) return false;
        
        MapLocation targetLoc = target.getLocation();
        
        if (Globals.rc.canCarryRat(targetLoc)) {
            Globals.rc.carryRat(targetLoc);
            return true;
        }
        
        return false;
    }
}
