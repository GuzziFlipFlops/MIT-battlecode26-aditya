package Hermes.robot;

import battlecode.common.*;
import Hermes.*;
import Hermes.comms.*;

public class SenseReporter {

    public static void report() throws GameActionException {
        for (RobotInfo cat : Globals.nearbyCats) {
            Squeaker.squeakDanger(cat.getLocation());
        }
        
        for (MapInfo info : Globals.nearbyMapInfos) {
            if (info.hasCheeseMine()) {
                Squeaker.squeakCheese(info.getMapLocation());
            }
        }
    }
}
