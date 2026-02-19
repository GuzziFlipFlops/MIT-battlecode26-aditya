package Hermes.combat;

import battlecode.common.*;
import Hermes.*;
import Hermes.robot.*;
import Hermes.comms.*;
import Hermes.nav.*;

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
