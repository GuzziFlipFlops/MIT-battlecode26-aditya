package Hermesv2.combat;

import battlecode.common.*;
import Hermesv2.*;

public class ThreatAssessor {

    public static final int THREAT_NONE = 0;
    public static final int THREAT_LOW = 1;
    public static final int THREAT_MEDIUM = 2;
    public static final int THREAT_HIGH = 3;
    public static final int THREAT_CRITICAL = 4;

    public static int assessThreat() {
        int threat = THREAT_NONE;
        
        for (RobotInfo cat : Globals.nearbyCats) {
            int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
            if (dist <= 2) {
                return THREAT_CRITICAL;
            } else if (dist <= 9) {
                threat = Math.max(threat, THREAT_HIGH);
            } else if (dist <= 20) {
                threat = Math.max(threat, THREAT_MEDIUM);
            }
        }
        
        if (!Globals.isCooperation) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                
                if (enemy.getType() == UnitType.RAT_KING) {
                    if (dist <= 9) {
                        threat = Math.max(threat, THREAT_HIGH);
                    } else if (dist <= 25) {
                        threat = Math.max(threat, THREAT_MEDIUM);
                    }
                } else {
                    if (dist <= 2) {
                        threat = Math.max(threat, THREAT_MEDIUM);
                    } else if (dist <= 8) {
                        threat = Math.max(threat, THREAT_LOW);
                    }
                }
            }
        }
        
        if (Globals.myHealth < 30) {
            threat = Math.min(THREAT_CRITICAL, threat + 1);
        } else if (Globals.myHealth < 50) {
            threat = Math.min(THREAT_HIGH, threat + 1);
        }
        
        return threat;
    }
}
