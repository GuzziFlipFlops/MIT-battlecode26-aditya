package Hermesv5;
import battlecode.common.*;
import Hermesv5.robot.*;
public class RobotPlayer {
    static RobotController rc;
    static int turnCount = 0;
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        Globals.init(rc);
        while (true) {
            turnCount++;
            Globals.update();
            switch (rc.getType()) {
                case BABY_RAT:
                    BabyRatRunner.run();
                    break;
                case RAT_KING:
                    RatKingRunner.run();
                    break;
                default:
                    break;
            }
            Clock.yield();
        }
    }
}