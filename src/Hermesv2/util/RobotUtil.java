package Hermesv2.util;

import battlecode.common.*;
import Hermesv2.*;

public class RobotUtil {

    public static RobotInfo findClosest(RobotInfo[] robots, MapLocation target) {
        if (robots == null || robots.length == 0) return null;
        
        RobotInfo closest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (RobotInfo robot : robots) {
            if (robot == null) continue;
            int dist = robot.getLocation().distanceSquaredTo(target);
            if (dist < minDist) {
                minDist = dist;
                closest = robot;
            }
        }
        
        return closest;
    }

    public static int countType(RobotInfo[] robots, UnitType type) {
        int count = 0;
        for (RobotInfo robot : robots) {
            if (robot != null && robot.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public static boolean isInVisionCone(MapLocation loc, MapLocation robotLoc, Direction facing, int visionRadiusSq, double visionAngle) {
        int distSq = loc.distanceSquaredTo(robotLoc);
        if (distSq > visionRadiusSq) return false;
        
        if (visionAngle >= 360.0) return true;
        
        Direction toLoc = robotLoc.directionTo(loc);
        if (toLoc == Direction.CENTER) return true;
        
        Direction leftBound = facing.rotateLeft().rotateLeft();
        Direction rightBound = facing.rotateRight().rotateRight();
        
        Direction current = facing;
        for (int i = 0; i < 3; i++) {
            if (toLoc == current) return true;
            current = current.rotateLeft();
        }
        current = facing.rotateRight();
        for (int i = 0; i < 3; i++) {
            if (toLoc == current) return true;
            current = current.rotateRight();
        }
        
        return false;
    }

    public static boolean isInVisionConeSimple(MapLocation loc, MapLocation robotLoc, Direction facing, int visionRadiusSq) {
        return loc.isWithinDistanceSquared(robotLoc, visionRadiusSq, facing, 90.0, false);
    }
}
