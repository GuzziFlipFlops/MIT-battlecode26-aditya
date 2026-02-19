package Hermesv8.comms;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.robot.*;
import Hermesv8.nav.*;
public class Squeaker {
    public static final int SQUEAK_DANGER = 0x10000000;
    public static final int SQUEAK_CHEESE = 0x20000000;
    public static final int SQUEAK_HELP = 0x30000000;
    public static final int SQUEAK_ATTACK = 0x40000000;
    public static void squeakDanger(MapLocation dangerLoc) throws GameActionException {
        int message = SQUEAK_DANGER | CommArray.packLocation(dangerLoc);
        Globals.rc.squeak(message);
    }
    public static void squeakCheese(MapLocation cheeseLoc) throws GameActionException {
        int message = SQUEAK_CHEESE | CommArray.packLocation(cheeseLoc);
        Globals.rc.squeak(message);
    }
    public static void squeakHelp() throws GameActionException {
        int message = SQUEAK_HELP | CommArray.packLocation(Globals.myLoc);
        Globals.rc.squeak(message);
    }
    public static void squeakAttack(MapLocation targetLoc) throws GameActionException {
        int message = SQUEAK_ATTACK | CommArray.packLocation(targetLoc);
        Globals.rc.squeak(message);
    }
    public static void processIncomingSqueaks() throws GameActionException {
        Message[] squeaks = Globals.rc.readSqueaks(-1);
        if (squeaks == null || squeaks.length == 0) return;
        MapLocation myLoc = Globals.myLoc;
        int myHealth = Globals.myHealth;
        for (int i = 0; i < squeaks.length && Clock.getBytecodesLeft() > 500; i++) {
            Message squeak = squeaks[i];
            if (squeak == null) continue;
            int content = squeak.getBytes();
            int type = content & 0xF0000000;
            MapLocation loc = CommArray.unpackLocation(content & 0x0FFFFFFF);
            if (loc == null) continue;
            if (type == SQUEAK_DANGER) {
                if (myLoc.distanceSquaredTo(loc) <= 16 && myHealth < 60) {
                    FleeNav.fleeFrom(loc);
                    return;
                }
            } else if (type == SQUEAK_CHEESE) {
                if (Globals.myCheese == 0 && Globals.numKnownCheeseMines == 0) {
                    BabyRatState.setTargetCheeseMine(loc);
                }
            } else if (type == SQUEAK_ATTACK && !Globals.isCooperation) {
                RobotInfo[] enemies = Globals.nearbyEnemies;
                for (int j = 0; j < enemies.length; j++) {
                    if (enemies[j] != null && enemies[j].getLocation().equals(loc)) {
                        BabyRatState.setState(BabyRatStateType.ATTACKING_ENEMY);
                        BabyRatState.setTargetKing(loc);
                        return;
                    }
                }
                MapLocation knownEnemyKing = CommArray.getNearestEnemyKing();
                if (knownEnemyKing != null && knownEnemyKing.equals(loc)) {
                    BabyRatState.setState(BabyRatStateType.ATTACKING_ENEMY);
                    BabyRatState.setTargetKing(loc);
                    return;
                }
            }
        }
    }
    public static void squeakCatLocation() throws GameActionException {
        if (Globals.nearbyCats.length > 0) {
            RobotInfo nearestCat = Globals.nearbyCats[0];
            int minDist = Globals.myLoc.distanceSquaredTo(nearestCat.getLocation());
            for (RobotInfo cat : Globals.nearbyCats) {
                int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearestCat = cat;
                }
            }
            squeakDanger(nearestCat.getLocation());
        }
    }
    public static void squeakToLureCat(MapLocation targetLoc) throws GameActionException {
        if (targetLoc != null && Clock.getBytecodesLeft() > 200) {
            squeakAttack(targetLoc);
        }
    }
    public static void squeakToLureCatToEnemy() throws GameActionException {
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) {
            squeakToLureCat(enemyKing);
            return;
        }
        int centerX = Globals.mapCenter.x;
        int centerY = Globals.mapCenter.y;
        int myX = Globals.myLoc.x;
        int myY = Globals.myLoc.y;
        int enemyX = centerX + (centerX - myX);
        int enemyY = centerY + (centerY - myY);
        enemyX = Math.max(0, Math.min(Globals.mapWidth - 1, enemyX));
        enemyY = Math.max(0, Math.min(Globals.mapHeight - 1, enemyY));
        MapLocation enemyArea = new MapLocation(enemyX, enemyY);
        squeakToLureCat(enemyArea);
    }
    public static void squeakToLureCatAwayFromKing(MapLocation kingLoc) throws GameActionException {
        if (kingLoc == null) return;
        Direction awayFromKing = kingLoc.directionTo(Globals.myLoc);
        if (awayFromKing == Direction.CENTER) {
            awayFromKing = Direction.NORTH;
        }
        MapLocation lureTarget = Globals.myLoc.add(awayFromKing).add(awayFromKing);
        squeakToLureCat(lureTarget);
    }
}