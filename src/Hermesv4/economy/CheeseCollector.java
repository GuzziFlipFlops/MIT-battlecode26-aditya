package Hermesv4.economy;

import battlecode.common.*;
import Hermesv4.*;
import Hermesv4.robot.*;
import Hermesv4.nav.*;

public class CheeseCollector {

    public static void collect() throws GameActionException {
        MapLocation targetCheeseMine = BabyRatState.getTargetCheeseMine();
        
        if (targetCheeseMine == null) {
            BabyRatState.setState(BabyRatStateType.IDLE);
            return;
        }
        
        int distToMine = Globals.myLoc.distanceSquaredTo(targetCheeseMine);
        
        if (distToMine <= 25) {
            for (MapInfo info : Globals.nearbyMapInfos) {
                if (info.getCheeseAmount() > 0) {
                    MapLocation cheeseLoc = info.getMapLocation();
                    
                    if (Globals.myLoc.isAdjacentTo(cheeseLoc) || Globals.myLoc.equals(cheeseLoc)) {
                        if (Globals.rc.canPickUpCheese(cheeseLoc)) {
                            Globals.rc.pickUpCheese(cheeseLoc);
                            return;
                        }
                    }
                    
                    Navigator.navigateTo(cheeseLoc);
                    return;
                }
            }
        }
        
        Navigator.navigateTo(targetCheeseMine);
    }
}
