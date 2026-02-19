package Hermesv5.combat;
import battlecode.common.*;
import Hermesv5.*;
import Hermesv5.robot.*;
import Hermesv5.comms.*;
import Hermesv5.nav.*;
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