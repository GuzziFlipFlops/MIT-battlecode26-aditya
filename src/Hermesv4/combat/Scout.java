package Hermesv4.combat;

import battlecode.common.*;
import Hermesv4.*;
import Hermesv4.robot.*;
import Hermesv4.comms.*;
import Hermesv4.nav.*;

public class Scout {

    public static void scout() throws GameActionException {
        for (MapInfo info : Globals.nearbyMapInfos) {
            if (info.hasCheeseMine()) {
                MapLocation mineLoc = info.getMapLocation();
                Squeaker.squeakCheese(mineLoc);
                if (Globals.myType == UnitType.RAT_KING) {
                    CommArray.reportCheeseMine(mineLoc);
                }
                BabyRatState.setTargetCheeseMine(mineLoc);
                BabyRatState.setState(BabyRatStateType.COLLECTING);
                return;
            }
        }
        
        Explorer.explore();
    }
}
