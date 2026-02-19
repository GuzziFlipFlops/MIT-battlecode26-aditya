package Hermesv7.combat;
import battlecode.common.*;
import Hermesv7.*;
public class Thrower {
    public static boolean tryThrowRat() throws GameActionException {
        if (!Globals.rc.canThrowRat()) return false;
        Globals.rc.throwRat();
        return true;
    }
    public static boolean tryThrowAtCat() throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying == null) return false;
        if (carrying.getTeam() != Globals.myTeam) {
            RobotInfo bestCat = null;
            int minDist = Integer.MAX_VALUE;
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    if (cat == null) continue;
                    int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                    if (dist <= 16 && dist < minDist) {
                        minDist = dist;
                        bestCat = cat;
                    }
                }
            }
            if (bestCat == null) {
                MapLocation nearestCatLoc = null;
                for (int i = 0; i < Globals.numKnownCats && i < 10; i++) {
                    MapLocation catLoc = Globals.knownCats[i];
                    if (catLoc != null) {
                        int dist = Globals.myLoc.distanceSquaredTo(catLoc);
                        if (dist <= 16 && dist < minDist) {
                            minDist = dist;
                            nearestCatLoc = catLoc;
                        }
                    }
                }
                if (nearestCatLoc != null) {
                    Direction toCat = Globals.myLoc.directionTo(nearestCatLoc);
                    if (Globals.myDir != toCat && Globals.rc.canTurn()) {
                        Globals.rc.turn(toCat);
                    }
                    if (Globals.rc.canThrowRat()) {
                        Globals.rc.throwRat();
                        return true;
                    }
                }
            }
            if (bestCat != null) {
                Direction toCat = Globals.myLoc.directionTo(bestCat.getLocation());
                if (Globals.myDir != toCat && Globals.rc.canTurn()) {
                    Globals.rc.turn(toCat);
                }
                if (Globals.rc.canThrowRat()) {
                    Globals.rc.throwRat();
                    return true;
                }
            }
        }
        if (carrying.getTeam() == Globals.myTeam && carrying.getHealth() < 30) {
            if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
                RobotInfo bestCat = Globals.nearbyCats[0];
                Direction toCat = Globals.myLoc.directionTo(bestCat.getLocation());
                if (Globals.myDir != toCat && Globals.rc.canTurn()) {
                    Globals.rc.turn(toCat);
                }
                if (Globals.rc.canThrowRat()) {
                    Globals.rc.throwRat();
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean tryThrowAtEnemyKing() throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying == null) return false;
        if (carrying.getTeam() == Globals.myTeam) return false;
        RobotInfo bestKing = null;
        int minDist = Integer.MAX_VALUE;
        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        bestKing = enemy;
                    }
                }
            }
        }
        if (bestKing == null) {
            MapLocation nearestKingLoc = null;
            for (int i = 0; i < Globals.numKnownEnemyKings; i++) {
                MapLocation kingLoc = Globals.knownEnemyKings[i];
                if (kingLoc != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(kingLoc);
                    if (dist < minDist && dist <= 25) {
                        minDist = dist;
                        nearestKingLoc = kingLoc;
                    }
                }
            }
            if (nearestKingLoc != null) {
                Direction toKing = Globals.myLoc.directionTo(nearestKingLoc);
                if (Globals.myDir != toKing && Globals.rc.canTurn()) {
                    Globals.rc.turn(toKing);
                }
                if (Globals.rc.canThrowRat()) {
                    Globals.rc.throwRat();
                    return true;
                }
            }
        }
        if (bestKing == null) return false;
        Direction toKing = Globals.myLoc.directionTo(bestKing.getLocation());
        if (Globals.myDir != toKing && Globals.rc.canTurn()) {
            Globals.rc.turn(toKing);
        }
        if (Globals.rc.canThrowRat()) {
            Globals.rc.throwRat();
            return true;
        }
        return false;
    }
    public static boolean tryThrowAwayFromKing(MapLocation kingLoc) throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying == null) return false;
        if (kingLoc == null) return false;
        Direction awayFromKing = kingLoc.directionTo(Globals.myLoc);
        if (awayFromKing == Direction.CENTER) {
            awayFromKing = Direction.NORTH;
        }
        if (Globals.myDir != awayFromKing && Globals.rc.canTurn()) {
            Globals.rc.turn(awayFromKing);
        }
        if (Globals.rc.canThrowRat()) {
            Globals.rc.throwRat();
            return true;
        }
        return false;
    }
    public static boolean tryDropRat(Direction dir) throws GameActionException {
        if (Globals.rc.canDropRat(dir)) {
            Globals.rc.dropRat(dir);
            return true;
        }
        return false;
    }
}