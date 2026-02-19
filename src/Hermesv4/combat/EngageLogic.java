package Hermesv4.combat;

import battlecode.common.*;
import Hermesv4.*;

public class EngageLogic {

    public static boolean shouldEngageEnemies() {
        int ourStrength = 0;
        int theirStrength = 0;
        
        for (RobotInfo ally : Globals.nearbyAllies) {
            if (ally.getType() == UnitType.BABY_RAT) {
                ourStrength += ally.getHealth();
            } else if (ally.getType() == UnitType.RAT_KING) {
                ourStrength += ally.getHealth() * 2;
            }
        }
        ourStrength += Globals.myHealth;
        
        for (RobotInfo enemy : Globals.nearbyEnemies) {
            if (enemy.getType() == UnitType.BABY_RAT) {
                theirStrength += enemy.getHealth();
            } else if (enemy.getType() == UnitType.RAT_KING) {
                theirStrength += enemy.getHealth() * 2;
            }
        }
        
        return ourStrength > theirStrength * 1.3;
    }

    public static boolean haveNumericalAdvantage() {
        return shouldEngageEnemies();
    }
}
