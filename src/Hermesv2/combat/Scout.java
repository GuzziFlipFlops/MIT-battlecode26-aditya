package Hermesv2.combat;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.robot.*;
import Hermesv2.comms.*;
import Hermesv2.nav.*;

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
