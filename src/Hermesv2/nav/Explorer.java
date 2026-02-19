package Hermesv2.nav;

import battlecode.common.*;
import Hermesv2.*;

public class Explorer {

    public static boolean explore() throws GameActionException {
        if (Globals.isNearCatSpawn(Globals.myLoc, 36)) {
            Direction away = Globals.mapCenter.directionTo(Globals.myLoc);
            if (away != Direction.CENTER && Mover.tryMove(away)) {
                return true;
            }
        }
        
        int distToCenter = Globals.myLoc.distanceSquaredTo(Globals.mapCenter);
        
        if (distToCenter > 400) {
            return Navigator.navigateTo(Globals.mapCenter);
        } else {
            return RandomMover.tryMoveRandom();
        }
    }
}
