package Hermesv2.economy;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.comms.*;
import Hermesv2.nav.*;
import Hermesv2.robot.*;

public class OptimizedCheeseCollection {

    private static final int ROLE_COLLECTOR = 1;
    private static final int ROLE_RUNNER = 2;
    private static final int ROLE_SCOUT = 3;
    
    private static final int ROLE_ASSIGNMENT_START = 32;
    private static final int ROLE_ASSIGNMENT_END = 47;
    private static final int MINE_ASSIGNMENT_START = 32; 
    
    public static boolean collectOptimized() throws GameActionException {
        boolean emergency = isEmergencyCheeseNeeded();
        
        if (Globals.myCheese > 0) {
            boolean delivered = deliverCheeseOptimized(emergency);
            if (delivered) return true;
            if (Globals.rc.isMovementReady()) {
                MapLocation king = findNearestKing();
                if (king != null) {
                    Navigator.navigateTo(king);
                    return true;
                }
            }
            return true;
        }
        
        if (emergency) {
            boolean found = findAndCollectEmergencyCheese();
            if (found) return true;
            MapLocation nearestMine = CommArray.getNearestCheeseMine();
            if (nearestMine != null && Globals.rc.isMovementReady()) {
                Navigator.navigateTo(nearestMine);
                return true;
            }
            if (Globals.rc.isMovementReady()) {
                Explorer.explore();
                return true;
            }
            return true;
        }
        
        MapLocation nearestKing = findNearestKing();
        boolean nearEmergency = false;
        if (nearestKing != null) {
            int distToKing = Globals.myLoc.distanceSquaredTo(nearestKing);
            if (distToKing <= 16) {
                int kingCheese = 0;
                try {
                    RobotInfo kingInfo = Globals.rc.senseRobotAtLocation(nearestKing);
                    if (kingInfo != null && kingInfo.getType() == UnitType.RAT_KING) {
                        kingCheese = Globals.rc.getAllCheese();
                    }
                } catch (GameActionException e) {
                }
                
                int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 15;
                if (kingCheese < minBuffer) {
                    nearEmergency = true;
                }
            }
        }
        
        if (nearEmergency) {
            if (findAndCollectNearestCheese()) {
                return true;
            }
        }
        
        int role = getMyRole();
        if (role == ROLE_COLLECTOR) {
            return collectAtAssignedMine();
        } else if (role == ROLE_RUNNER) {
            return runCheeseRelay();
        } else {
            return findAndCollectNearestCheese();
        }
    }
    
    private static boolean deliverCheeseOptimized(boolean emergency) throws GameActionException {
        MapLocation nearestKing = findNearestKing();
        if (nearestKing == null) {
            if (emergency && Globals.rc.isMovementReady()) {
                Explorer.explore();
                return true;
            }
            return false;
        }
        
        int distToKing = Globals.myLoc.distanceSquaredTo(nearestKing);
        
        if (distToKing <= GameConstants.CHEESE_DROP_RADIUS_SQUARED) {
            if (Globals.rc.isActionReady() && Globals.rc.canTransferCheese(nearestKing, Globals.myCheese)) {
                int amount = Globals.myCheese;
                Globals.rc.transferCheese(nearestKing, amount);
                return true;
            }
        }
        
        if (Globals.rc.isMovementReady()) {
            if (distToKing <= 16) {
                Direction dir = Globals.myLoc.directionTo(nearestKing);
                if (Mover.tryMove(dir)) {
                    return true;
                }
            }
            Navigator.navigateTo(nearestKing);
            return true;
        }
        
        return false;
    }
    
    private static boolean findAndCollectEmergencyCheese() throws GameActionException {
        if (Globals.rc.isActionReady()) {
            MapLocation nearestNearby = null;
            int minDist = Integer.MAX_VALUE;
            
            MapInfo[] mapInfos = Globals.nearbyMapInfos;
            MapLocation myLoc = Globals.myLoc;
            for (int i = 0; i < mapInfos.length; i++) {
                MapInfo info = mapInfos[i];
                if (info != null && info.getCheeseAmount() > 0) {
                    MapLocation cheeseLoc = info.getMapLocation();
                    if (cheeseLoc != null) {
                        int dist = myLoc.distanceSquaredTo(cheeseLoc);
                        if (dist < minDist) {
                            minDist = dist;
                            nearestNearby = cheeseLoc;
                        }
                    }
                }
            }
            
            if (nearestNearby != null && Globals.rc.canPickUpCheese(nearestNearby)) {
                Globals.rc.pickUpCheese(nearestNearby);
                return true;
            }
        }
        
        MapLocation nearestCheese = findNearestCheeseLocation();
        if (nearestCheese != null) {
            if (Globals.rc.isMovementReady()) {
                Direction dir = Globals.myLoc.directionTo(nearestCheese);
                if (Mover.tryMove(dir)) {
                    return true;
                }
                for (Direction adjDir : new Direction[] {dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()}) {
                    if (Mover.tryMove(adjDir)) {
                        return true;
                    }
                }
                Navigator.navigateTo(nearestCheese);
                return true;
            }
        }
        
        if (Globals.rc.isMovementReady()) {
            if (Globals.numKnownCheeseMines == 0) {
                Navigator.navigateTo(Globals.mapCenter);
            } else {
                MapLocation nearestMine = CommArray.getNearestCheeseMine();
                if (nearestMine != null) {
                    Navigator.navigateTo(nearestMine);
                } else {
                    Explorer.explore();
                }
            }
            return true;
        }
        
        return false;
    }
    
    private static boolean collectAtAssignedMine() throws GameActionException {
        MapLocation assignedMine = getAssignedMine();
        if (assignedMine == null) {
            return findAndCollectNearestCheese();
        }
        
        if (Globals.myLoc.distanceSquaredTo(assignedMine) <= 25) {
            if (Globals.rc.isActionReady()) {
                MapInfo[] mapInfos = Globals.nearbyMapInfos;
                for (int i = 0; i < mapInfos.length; i++) {
                    MapInfo info = mapInfos[i];
                    if (info != null && info.getCheeseAmount() > 0) {
                        MapLocation cheeseLoc = info.getMapLocation();
                        if (cheeseLoc != null && Globals.rc.canPickUpCheese(cheeseLoc)) {
                            Globals.rc.pickUpCheese(cheeseLoc);
                            return true;
                        }
                    }
                }
            }
        }
        
        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(assignedMine);
            return true;
        }
        
        return false;
    }
    
    private static boolean runCheeseRelay() throws GameActionException {
        if (Globals.myCheese > 0) {
            return deliverCheeseOptimized(false);
        }
        
        MapLocation nearestKing = findNearestKing();
        MapLocation nearestMine = CommArray.getNearestCheeseMine();
        
        if (nearestMine == null || nearestKing == null) {
            return findAndCollectNearestCheese();
        }
        
        MapLocation bestCheese = findCheeseOnPath(nearestMine, nearestKing);
        if (bestCheese != null) {
            if (Globals.rc.isActionReady()) {
                if (Globals.myLoc.isAdjacentTo(bestCheese) && Globals.rc.canPickUpCheese(bestCheese)) {
                    Globals.rc.pickUpCheese(bestCheese);
                    return true;
                }
            }
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(bestCheese);
                return true;
            }
        }
        
        return findAndCollectNearestCheese();
    }
    
    private static boolean findAndCollectNearestCheese() throws GameActionException {
        if (Globals.rc.isActionReady()) {
            MapLocation nearestNearby = null;
            int minDist = Integer.MAX_VALUE;
            MapInfo[] mapInfos = Globals.nearbyMapInfos;
            MapLocation myLoc = Globals.myLoc;
            
            for (int i = 0; i < mapInfos.length; i++) {
                MapInfo info = mapInfos[i];
                if (info != null && info.getCheeseAmount() > 0) {
                    MapLocation loc = info.getMapLocation();
                    if (loc != null) {
                        int dist = myLoc.distanceSquaredTo(loc);
                        if (dist < minDist) {
                            minDist = dist;
                            nearestNearby = loc;
                        }
                    }
                }
            }
            
            if (nearestNearby != null && Globals.rc.canPickUpCheese(nearestNearby)) {
                Globals.rc.pickUpCheese(nearestNearby);
                return true;
            }
            
            if (nearestNearby != null && myLoc.isAdjacentTo(nearestNearby) && Globals.rc.isMovementReady()) {
                return true;
            }
        }
        
        MapLocation nearestMine = CommArray.getNearestCheeseMine();
        if (nearestMine != null) {
            if (Globals.rc.isMovementReady()) {
                int dist = Globals.myLoc.distanceSquaredTo(nearestMine);
                if (dist <= 16) {
                    Direction dir = Globals.myLoc.directionTo(nearestMine);
                    if (Mover.tryMove(dir)) {
                        return true;
                    }
                }
                Navigator.navigateTo(nearestMine);
                return true;
            }
        }
        
        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(Globals.mapCenter);
            return true;
        }
        
        return false;
    }
    
    private static MapLocation findCheeseOnPath(MapLocation mine, MapLocation king) {
        MapLocation best = null;
        int bestScore = Integer.MAX_VALUE;
        MapInfo[] mapInfos = Globals.nearbyMapInfos;
        MapLocation myLoc = Globals.myLoc;
        
        for (int i = 0; i < mapInfos.length; i++) {
            MapInfo info = mapInfos[i];
            if (info != null && info.getCheeseAmount() > 0) {
                MapLocation loc = info.getMapLocation();
                if (loc != null) {
                    int distToMine = loc.distanceSquaredTo(mine);
                    int distToKing = loc.distanceSquaredTo(king);
                    int distToUs = myLoc.distanceSquaredTo(loc);
                    int score = distToUs + (distToMine + distToKing) / 2;
                    
                    if (score < bestScore) {
                        bestScore = score;
                        best = loc;
                    }
                }
            }
        }
        
        return best;
    }
    
    private static MapLocation findNearestCheeseLocation() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (MapInfo info : Globals.nearbyMapInfos) {
            if (info != null && (info.getCheeseAmount() > 0 || info.hasCheeseMine())) {
                MapLocation loc = info.getMapLocation();
                if (loc != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(loc);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = loc;
                    }
                }
            }
        }
        
        if (nearest == null && Globals.numKnownCheeseMines > 0) {
            for (int i = 0; i < Globals.numKnownCheeseMines; i++) {
                MapLocation mine = Globals.knownCheeseMines[i];
                if (mine != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(mine);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = mine;
                    }
                }
            }
        }
        
        return nearest;
    }
    
    private static MapLocation findNearestKing() {
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo ally : Globals.nearbyAllies) {
            if (ally.getType() == UnitType.RAT_KING) {
                int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = ally.getLocation();
                }
            }
        }
        
        if (nearest == null) {
            for (int i = 0; i < Globals.numKnownAlliedKings; i++) {
                MapLocation king = Globals.knownAlliedKings[i];
                if (king != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(king);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = king;
                    }
                }
            }
        }
        
        return nearest;
    }
    
    private static boolean isEmergencyCheeseNeeded() {
        try {
            int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
            return flag == 2; 
        } catch (GameActionException e) {
            return false;
        }
    }
    
    private static int getMyRole() throws GameActionException {
        int roleIndex = ROLE_ASSIGNMENT_START + (Globals.myID % (ROLE_ASSIGNMENT_END - ROLE_ASSIGNMENT_START + 1));
        int assignment = Globals.rc.readSharedArray(roleIndex);
        
        if (assignment == 0) {
            int role = (Globals.myID % 10);
            if (role < 3) {
                return ROLE_COLLECTOR;
            } else if (role < 8) {
                return ROLE_RUNNER;
            } else {
                return ROLE_SCOUT;
            }
        }
        
        return assignment & 0xFF; 
    }
    
    private static MapLocation getAssignedMine() throws GameActionException {
        int roleIndex = MINE_ASSIGNMENT_START + (Globals.myID % (ROLE_ASSIGNMENT_END - MINE_ASSIGNMENT_START + 1));
        int assignment = Globals.rc.readSharedArray(roleIndex);
        
        if (assignment == 0) return null;
        
        int minePacked = (assignment >> 8) & 0xFFFF;
        return CommArray.unpackLocation(minePacked);
    }
}

