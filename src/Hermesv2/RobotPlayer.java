package Hermesv2;

import battlecode.common.*;
import Hermesv2.robot.*;

public class RobotPlayer {

    static RobotController rc;
    static int turnCount = 0;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        Globals.init(rc);
        

        while (true) {
            turnCount++;
            
            if (rc.getType() == UnitType.RAT_KING && turnCount % 10 == 0) {
                System.out.println("Turn " + turnCount + ": " + rc.getType() + " at " + rc.getLocation());
            }
            
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
