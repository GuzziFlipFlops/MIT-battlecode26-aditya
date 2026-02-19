package Hermesv7.robot;
import battlecode.common.*;
import Hermesv7.*;
import Hermesv7.nav.*;
import Hermesv7.economy.*;
public class KingRetreater {
    public static void retreat() throws GameActionException {
        if (Globals.rc.isActionReady()) {
            while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000) {
                boolean actionTaken = false;
                if (TrapPlacer.tryPlaceRatTrap()) {
                    actionTaken = true;
                    continue;
                }
                if (Globals.globalCheese >= Constants.DIRT_COST) {
                    if (TrapPlacer.tryPlaceDefensiveWall()) {
                        actionTaken = true;
                        continue;
                    }
                }
                if (KingManager.trySpawnRats()) {
                    actionTaken = true;
                    continue;
                }
                if (!actionTaken) break;
            }
        }
        if (Globals.rc.isMovementReady() && Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            MapLocation[] dangers = new MapLocation[Globals.nearbyCats.length];
            for (int i = 0; i < Globals.nearbyCats.length; i++) {
                if (Globals.nearbyCats[i] != null) {
                    dangers[i] = Globals.nearbyCats[i].getLocation();
                }
            }
            FleeNav.fleeFromMultiple(dangers);
        }
    }
}