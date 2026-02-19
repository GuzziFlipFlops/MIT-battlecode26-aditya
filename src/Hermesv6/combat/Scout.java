package Hermesv6.combat;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.robot.*;
import Hermesv6.comms.*;
import Hermesv6.nav.*;
public class Scout {
    public static void scout() throws GameActionException {
        for (MapInfo info : Globals.nearbyMapInfos) {
            if (info.hasCheeseMine()) {
                MapLocation mineLoc = info.getMapLocation();
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