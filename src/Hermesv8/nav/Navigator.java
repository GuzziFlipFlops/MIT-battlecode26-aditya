package Hermesv8.nav;
import battlecode.common.*;
import Hermesv8.*;
public class Navigator {
    private static MapLocation lastTarget = null;
    private static MapLocation lastLocation = null;
    private static int stuckCounter = 0;
    private static Direction bugDirection = null;
    private static boolean bugClockwise = true;

    public static boolean navigateTo(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;
        if (Globals.myLoc.equals(target)) return true;

        // Detect if stuck
        if (lastLocation != null && lastLocation.equals(Globals.myLoc) &&
            lastTarget != null && lastTarget.equals(target)) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
            bugDirection = null;
        }
        lastLocation = Globals.myLoc;
        lastTarget = target;

        // If stuck for 3+ turns, use bug navigation
        if (stuckCounter >= 3) {
            return bugNavigate(target);
        }

        // Try direct movement first
        Direction directDir = Globals.myLoc.directionTo(target);
        if (directDir != Direction.CENTER && Globals.rc.canMove(directDir)) {
            Globals.rc.move(directDir);
            bugDirection = null;
            return true;
        }

        // Use A* for longer distances if we have bytecodes
        int distSq = Globals.myLoc.distanceSquaredTo(target);
        if (distSq > 4 && Clock.getBytecodesLeft() > 4000) {
            Direction aStarDir = AStarPathfinder.findBestDirection(target);
            if (aStarDir != null && aStarDir != Direction.CENTER) {
                if (Mover.tryMove(aStarDir)) {
                    bugDirection = null;
                    return true;
                }
            }
        }

        // Try directions that get closer
        Direction[] dirs = Direction.allDirections();
        int currentDist = Globals.myLoc.distanceSquaredTo(target);
        for (int i = 0; i < dirs.length; i++) {
            Direction dir = dirs[i];
            if (dir == Direction.CENTER) continue;
            MapLocation nextLoc = Globals.myLoc.add(dir);
            if (nextLoc.distanceSquaredTo(target) < currentDist && Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
                bugDirection = null;
                return true;
            }
        }

        // Last resort: any valid move
        for (int i = 0; i < dirs.length; i++) {
            Direction dir = dirs[i];
            if (dir == Direction.CENTER) continue;
            if (Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
                bugDirection = null;
                return true;
            }
        }

        return false;
    }

    /**
     * Bug navigation algorithm for obstacle avoidance
     */
    private static boolean bugNavigate(MapLocation target) throws GameActionException {
        if (bugDirection == null) {
            bugDirection = Globals.myLoc.directionTo(target);
            bugClockwise = (Globals.myID % 2 == 0); // Vary direction by ID
        }

        // Try to follow obstacle edge
        for (int i = 0; i < 8; i++) {
            if (Globals.rc.canMove(bugDirection)) {
                Globals.rc.move(bugDirection);

                // Check if we can move toward target again
                Direction toTarget = Globals.myLoc.directionTo(target);
                if (Globals.rc.canMove(toTarget)) {
                    bugDirection = null; // Reset bug mode
                }
                return true;
            }

            // Turn along obstacle edge
            bugDirection = bugClockwise ? bugDirection.rotateRight() : bugDirection.rotateLeft();
        }

        return false;
    }

    public static boolean navigateToSafe(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;
        if (Globals.myLoc.equals(target)) return true;

        Direction directDir = Globals.myLoc.directionTo(target);
        if (directDir != Direction.CENTER) {
            MapLocation nextLoc = Globals.myLoc.add(directDir);
            if (Globals.rc.canSenseLocation(nextLoc)) {
                if (Globals.rc.sensePassability(nextLoc) && Globals.rc.senseRobotAtLocation(nextLoc) == null) {
                    if (Globals.rc.canMove(directDir)) {
                        Globals.rc.move(directDir);
                        return true;
                    }
                }
            } else if (Globals.rc.canMove(directDir)) {
                Globals.rc.move(directDir);
                return true;
            }
        }

        int distSq = Globals.myLoc.distanceSquaredTo(target);
        if (distSq > 4 && Clock.getBytecodesLeft() > 4000) {
            Direction aStarDir = AStarPathfinder.findBestDirection(target);
            if (aStarDir != null && aStarDir != Direction.CENTER) {
                MapLocation nextLoc = Globals.myLoc.add(aStarDir);
                if (Globals.rc.canSenseLocation(nextLoc)) {
                    if (Globals.rc.sensePassability(nextLoc) && Globals.rc.senseRobotAtLocation(nextLoc) == null) {
                        if (Mover.tryMove(aStarDir)) {
                            return true;
                        }
                    }
                } else if (Mover.tryMove(aStarDir)) {
                    return true;
                }
            }
        }
            
        for (Direction dir : Direction.allDirections()) {
            if (dir == Direction.CENTER) continue;
            MapLocation nextLoc = Globals.myLoc.add(dir);
            if (nextLoc.distanceSquaredTo(target) < Globals.myLoc.distanceSquaredTo(target)) {
                if (Globals.rc.canSenseLocation(nextLoc)) {
                    if (Globals.rc.sensePassability(nextLoc) && Globals.rc.senseRobotAtLocation(nextLoc) == null) {
                        if (Globals.rc.canMove(dir)) {
                            Globals.rc.move(dir);
                            return true;
                        }
                    }
                } else if (Globals.rc.canMove(dir)) {
                    Globals.rc.move(dir);
                    return true;
                }
            }
        }
        
        return false;
    }
}