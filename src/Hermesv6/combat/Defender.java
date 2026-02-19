package Hermesv6.combat;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.robot.*;
import Hermesv6.comms.*;
import Hermesv6.nav.*;
public class Defender {
    public static void defend() throws GameActionException {
        MapLocation kingLoc = CommArray.getNearestAlliedKing();
        if (kingLoc == null) {
            BabyRatState.setState(BabyRatStateType.IDLE);
            return;
        }
        int distToKing = Globals.myLoc.distanceSquaredTo(kingLoc);
        if (distToKing > 16) {
            Navigator.navigateTo(kingLoc);
        } else if (distToKing < 4) {
            FleeNav.fleeFrom(kingLoc);
        }
        if (Globals.nearbyCats.length > 0) {
            CatAttacker.attack();
            return;
        }
        if (!Globals.isCooperation && Globals.nearbyEnemies.length > 0) {
            EnemyAttacker.attack();
            return;
        }
    }
}