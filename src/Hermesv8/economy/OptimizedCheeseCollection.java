package Hermesv8.economy;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;
import Hermesv8.nav.*;
import Hermesv8.nav.Mover;
public class OptimizedCheeseCollection {
    private static final int ROLE_COLLECTOR = 1;
    private static final int ROLE_RUNNER = 2;
    private static final int ROLE_SCOUT = 3;
    private static final int ROLE_ASSIGNMENT_START = 32;
    private static final int ROLE_ASSIGNMENT_END = 47;
    private static final int MINE_ASSIGNMENT_START = 32;
    public static boolean collectOptimized() throws GameActionException {
        boolean emergency = isEmergencyCheeseNeeded();
        if (Globals.nearbyMapInfos != null && Clock.getBytecodesLeft() > 300) {
            for (int i = 0; i < Globals.nearbyMapInfos.length && Clock.getBytecodesLeft() > 200; i++) {
                MapInfo info = Globals.nearbyMapInfos[i];
                if (info != null && info.hasCheeseMine()) {
                    try {
                        CommArray.reportCheeseMine(info.getMapLocation());
                    } catch (GameActionException e) {
                    }
                }
            }
        }
        if (Globals.myCheese > 0) {
            MapLocation nearestKing = findNearestKing();
            if (nearestKing == null) {
                nearestKing = CommArray.getNearestAlliedKing();
            }
            if (nearestKing == null && Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        nearestKing = ally.getLocation();
                        break;
                    }
                }
            }
            if (nearestKing != null) {
                MapLocation actualKingLoc = nearestKing;
                if (Globals.rc.canSenseLocation(nearestKing)) {
                    RobotInfo[] nearbyRobots = Globals.rc.senseNearbyRobots(nearestKing, 8, Globals.myTeam);
                    for (RobotInfo robot : nearbyRobots) {
                        if (robot != null && robot.getType() == UnitType.RAT_KING) {
                            actualKingLoc = robot.getLocation();
                            break;
                        }
                    }
                }
                if (Globals.rc.canTransferCheese(actualKingLoc, Globals.myCheese)) {
                    int amount = Globals.myCheese;
                    Globals.rc.transferCheese(actualKingLoc, amount);
                    System.out.println("[Hermesv8] ✓ Rat #" + Globals.myID + " delivered " + amount + " cheese to king at " + actualKingLoc);
                    return true;
                }
                if (Globals.rc.isMovementReady()) {
                    Navigator.navigateTo(actualKingLoc);
                    return true;
                }
            } else {
                if (Globals.rc.isMovementReady()) {
                    Explorer.explore();
                    return true;
                }
            }
            return true;
        }
        boolean nearEmergency = emergency;
        if (!nearEmergency) {
            int teamCheese = Globals.rc.getAllCheese();
            int numKings = Math.max(1, Globals.numKnownAlliedKings);
            int cheeseNeededPerRound = Constants.RATKING_CHEESE_CONSUMPTION * numKings;
            int safeBuffer = cheeseNeededPerRound * 200;
            boolean lowCheese = (teamCheese < safeBuffer);
            if (lowCheese || teamCheese <= 600) {
                nearEmergency = true;
            }
        }
        if (nearEmergency || emergency) {
            boolean found = findAndCollectEmergencyCheese();
            if (found) return true;
            MapLocation nearestMine = CommArray.getNearestCheeseMine();
            if (nearestMine != null && Globals.rc.isMovementReady()) {
                Navigator.navigateTo(nearestMine);
                return true;
            }
            if (Globals.rc.isMovementReady()) {
                MapLocation center = Globals.mapCenter;
                Navigator.navigateTo(center);
                return true;
            }
            return true;
        }
        return findAndCollectNearestCheese();
    }
    private static boolean deliverCheeseOptimized(boolean emergency) throws GameActionException {
        MapLocation nearestKing = findNearestKing();
        if (nearestKing == null) {
            if (emergency && Globals.rc.isMovementReady()) {
                MapLocation[] knownKings = Globals.knownAlliedKings;
                if (knownKings != null && Globals.numKnownAlliedKings > 0) {
                    Navigator.navigateTo(knownKings[0]);
                    return true;
                }
                Explorer.explore();
                return true;
            }
            return false;
        }
        @SuppressWarnings("unused")
        int distToKing = Globals.myLoc.distanceSquaredTo(nearestKing);
        if (Globals.rc.canTransferCheese(nearestKing, Globals.myCheese)) {
            int amount = Globals.myCheese;
            Globals.rc.transferCheese(nearestKing, amount);
            System.out.println("Delivered " + amount + " cheese (from deliverCheeseOptimized)");
            return true;
        }
        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(nearestKing);
            return true;
        }
        return false;
    }
    private static boolean findAndCollectEmergencyCheese() throws GameActionException {
        if (Globals.rc.isActionReady()) {
            MapLocation nearestNearby = null;
            int minDist = Integer.MAX_VALUE;
            int maxAmount = 0;
            MapInfo[] mapInfos = Globals.nearbyMapInfos;
            MapLocation myLoc = Globals.myLoc;
            for (int i = 0; i < mapInfos.length; i++) {
                MapInfo info = mapInfos[i];
                if (info != null && info.getCheeseAmount() > 0) {
                    MapLocation cheeseLoc = info.getMapLocation();
                    if (cheeseLoc != null) {
                        int dist = myLoc.distanceSquaredTo(cheeseLoc);
                        int amount = info.getCheeseAmount();
                        if (dist < minDist || (dist == minDist && amount > maxAmount)) {
                            minDist = dist;
                            maxAmount = amount;
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
                Navigator.navigateTo(nearestCheese);
                return true;
            }
        }
        if (Globals.rc.isMovementReady()) {
            MapLocation nearestMine = CommArray.getNearestCheeseMine();
            if (nearestMine != null) {
                Navigator.navigateTo(nearestMine);
            } else {
                MapLocation center = Globals.mapCenter;
                Navigator.navigateTo(center);
            }
            return true;
        }
        return false;
    }
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
        MapLocation nearestNearby = null;
        int minDist = Integer.MAX_VALUE;
        int maxAmount = 0;
        MapInfo[] mapInfos = Globals.nearbyMapInfos;
        MapLocation myLoc = Globals.myLoc;
        if (mapInfos != null) {
            for (int i = 0; i < mapInfos.length; i++) {
                MapInfo info = mapInfos[i];
                if (info != null && info.getCheeseAmount() > 0) {
                    MapLocation loc = info.getMapLocation();
                    if (loc != null) {
                        int dist = myLoc.distanceSquaredTo(loc);
                        int amount = info.getCheeseAmount();
                        if (dist < minDist || (dist == minDist && amount > maxAmount)) {
                            minDist = dist;
                            maxAmount = amount;
                            nearestNearby = loc;
                        }
                    }
                }
            }
        }
        if (nearestNearby != null) {
            if (Globals.rc.canPickUpCheese(nearestNearby)) {
                Globals.rc.pickUpCheese(nearestNearby);
                return true;
            }
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(nearestNearby);
                return true;
            }
            Direction dir = Globals.myLoc.directionTo(nearestNearby);
            if (dir != Direction.CENTER && Globals.rc.canTurn() && Globals.rc.getDirection() != dir) {
                Globals.rc.turn(dir);
            }
            return true;
        }
        MapLocation nearestMine = CommArray.getNearestCheeseMine();
        if (nearestMine != null) {
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(nearestMine);
                return true;
            }
            Direction dir = Globals.myLoc.directionTo(nearestMine);
            if (dir != Direction.CENTER && Globals.rc.canTurn() && Globals.rc.getDirection() != dir) {
                Globals.rc.turn(dir);
            }
            return true;
        }
        MapLocation nearestCheese = findNearestCheeseLocation();
        if (nearestCheese != null) {
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(nearestCheese);
                return true;
            }
            Direction dir = Globals.myLoc.directionTo(nearestCheese);
            if (dir != Direction.CENTER && Globals.rc.canTurn() && Globals.rc.getDirection() != dir) {
                Globals.rc.turn(dir);
            }
            return true;
        }
        if (nearestMine == null && Globals.rc.isMovementReady()) {
            Explorer.explore();
            return true;
        }
        if (Globals.rc.isMovementReady()) {
            Explorer.explore();
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
        if (Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                    int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = ally.getLocation();
                    }
                }
            }
        }
        if (nearest == null && Globals.knownAlliedKings != null) {
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
    @SuppressWarnings("unused")
    private static MapLocation findBestAdjacentTileToKing(MapLocation kingCenter) throws GameActionException {
        if (kingCenter == null) return null;
        MapLocation bestTile = null;
        int minDist = Integer.MAX_VALUE;
        for (Direction dir : Direction.allDirections()) {
            if (dir == Direction.CENTER) continue;
            MapLocation candidate = kingCenter.add(dir);
            if (candidate.x >= 0 && candidate.x < Globals.mapWidth &&
                candidate.y >= 0 && candidate.y < Globals.mapHeight) {
                if (Globals.rc.canSenseLocation(candidate)) {
                    if (Globals.rc.sensePassability(candidate)) {
                        RobotInfo robot = Globals.rc.senseRobotAtLocation(candidate);
                        if (robot == null || robot.getType() == UnitType.RAT_KING) {
                            int dist = Globals.myLoc.distanceSquaredTo(candidate);
                            if (dist < minDist) {
                                minDist = dist;
                                bestTile = candidate;
                            }
                        }
                    }
                } else {
                    int dist = Globals.myLoc.distanceSquaredTo(candidate);
                    if (dist < minDist) {
                        minDist = dist;
                        bestTile = candidate;
                    }
                }
            }
        }
        return bestTile;
    }
    @SuppressWarnings("unused")
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