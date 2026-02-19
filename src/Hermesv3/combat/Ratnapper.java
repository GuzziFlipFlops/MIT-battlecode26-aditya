package Hermesv3.combat;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.comms.*;

public class Ratnapper {

    public static boolean canRatnap(RobotInfo target) {
        if (target.getType() != UnitType.BABY_RAT) return false;
        
        MapLocation targetLoc = target.getLocation();
        if (!Globals.myLoc.isAdjacentTo(targetLoc)) return false;
        
        Direction targetFacing = target.getDirection();
        Direction toUs = targetLoc.directionTo(Globals.myLoc);
        
        boolean facingAway = !isInVisionCone(targetFacing, toUs);
        boolean lowerHealth = target.getHealth() < Globals.myHealth;
        boolean isAlly = target.getTeam() == Globals.myTeam;
        
        return facingAway || lowerHealth || isAlly;
    }

    private static boolean isInVisionCone(Direction facing, Direction toCheck) {
        if (facing == Direction.CENTER) return true;
        if (toCheck == Direction.CENTER) return true;
        
        Direction current = facing;
        for (int i = 0; i < 3; i++) {
            if (current == toCheck) return true;
            current = current.rotateLeft();
        }
        current = facing.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (current == toCheck) return true;
            current = current.rotateRight();
        }
        
        return false;
    }

    public static boolean tryRatnap(RobotInfo target) throws GameActionException {
        if (target == null) return false;
        
        MapLocation targetLoc = target.getLocation();
        
        if (Globals.rc.canCarryRat(targetLoc)) {
            Globals.rc.carryRat(targetLoc);
            return true;
        }
        
        return false;
    }
    
    public static RobotInfo findBestRatnapTarget(boolean defensive) throws GameActionException {
        if (Globals.nearbyEnemies == null) return null;
        
        RobotInfo bestTarget = null;
        int bestScore = Integer.MIN_VALUE;
        
        MapLocation kingLoc = null;
        if (defensive) {
            kingLoc = CommArray.getNearestAlliedKing();
        }
        
        for (RobotInfo enemy : Globals.nearbyEnemies) {
            if (enemy == null || enemy.getType() != UnitType.BABY_RAT) continue;
            
            if (!canRatnap(enemy)) continue;
            
            int score = 0;
            
            
            score += (100 - enemy.getHealth()) * 2;
            
            
            if (defensive && kingLoc != null) {
                int distToKing = enemy.getLocation().distanceSquaredTo(kingLoc);
                if (distToKing <= 4) score += 100;
                else if (distToKing <= 9) score += 50;
                else if (distToKing <= 16) score += 25;
            }
            
            
            if (!defensive) {
                MapLocation enemyKing = CommArray.getNearestEnemyKing();
                if (enemyKing != null) {
                    int distToEnemyKing = enemy.getLocation().distanceSquaredTo(enemyKing);
                    if (distToEnemyKing <= 4) score += 80; 
                }
            }
            
            
            Direction enemyFacing = enemy.getDirection();
            Direction toUs = enemy.getLocation().directionTo(Globals.myLoc);
            if (!isInVisionCone(enemyFacing, toUs)) {
                score += 30; 
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestTarget = enemy;
            }
        }
        
        return bestTarget;
    }
}
