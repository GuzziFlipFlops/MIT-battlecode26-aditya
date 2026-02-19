package Hermesv2.robot;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.nav.*;

public class KingRetreater {

    public static void retreat() throws GameActionException {
        if (Globals.rc.isActionReady()) {
            KingManager.trySpawnRats();
        }
        
        if (Globals.rc.isMovementReady()) {
            if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
                MapLocation[] dangers = new MapLocation[Globals.nearbyCats.length];
                for (int i = 0; i < Globals.nearbyCats.length; i++) {
                    if (Globals.nearbyCats[i] != null) {
                        dangers[i] = Globals.nearbyCats[i].getLocation();
                    }
                }
                FleeNav.fleeFromMultiple(dangers);
            } else {
                KingManager.moveTowardBetterPosition();
            }
        }
    }
}
