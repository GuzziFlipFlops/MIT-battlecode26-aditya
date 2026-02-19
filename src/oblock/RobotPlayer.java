package oblock;

import battlecode.common.*;

import java.util.Random;


public class RobotPlayer {

    static int turnCount = 0;
    static final Random rng = new Random(6147);
    static boolean t = false;

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    // ADITYA STATIC CODE START



   // 0..2 which of the 3 positions we're trying next

    // for baby rats



    // for kings
    static int best = -1;
    static boolean rkMovedForward = false; // moved north once (only once)

    static boolean disabled = false;
    static boolean produced = false;




    // ADITYA CODE END

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!

     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {


        while (true) {


            turnCount += 1;

            try {

                if (!rc.getType().toString().equals("RAT_KING")) {

                    if (rc.readSharedArray(52) == 1 && !disabled) {
                        if (rc.canBecomeRatKing()) {
                            rc.becomeRatKing();
                            rc.writeSharedArray(52, 2);
                        }
                        continue;
                    }
                    if (rc.readSharedArray(51) == 2) {disabled = true;}


                }







                if (rc.getType().toString().equals("RAT_KING")) { // only for spawning next king

                    if (rc.getRoundNum() == 1) rc.writeSharedArray(51, 0); // reproduce

                    if (rc.getRoundNum() == 13) { rc.writeSharedArray(51, 0); best = -1; rkMovedForward = false; }// make more

                    MapLocation md = rc.getLocation();
                    int cx = md.x;
                    int cy = md.y;
                    if (rc.readSharedArray(51) == 0 && !produced) {
                        rc.writeSharedArray(50, 1);
                        rc.writeSharedArray(52, 1); // rat king count
// north
                    if (best == -1
                            && rc.onTheMap(new MapLocation(cx - 1, cy + 3)) && rc.sensePassability(new MapLocation(cx - 1, cy + 3))
                            && rc.onTheMap(new MapLocation(cx,     cy + 3)) && rc.sensePassability(new MapLocation(cx,     cy + 3))
                            && rc.onTheMap(new MapLocation(cx + 1, cy + 3)) && rc.sensePassability(new MapLocation(cx + 1, cy + 3))
                            && rc.onTheMap(new MapLocation(cx - 1, cy + 4)) && rc.sensePassability(new MapLocation(cx - 1, cy + 4))
                            && rc.onTheMap(new MapLocation(cx,     cy + 4)) && rc.sensePassability(new MapLocation(cx,     cy + 4))
                            && rc.onTheMap(new MapLocation(cx + 1, cy + 4)) && rc.sensePassability(new MapLocation(cx + 1, cy + 4))
                            && rc.onTheMap(new MapLocation(cx - 1, cy + 2)) && rc.sensePassability(new MapLocation(cx - 1, cy + 2))
                            && rc.onTheMap(new MapLocation(cx,     cy + 2)) && rc.sensePassability(new MapLocation(cx,     cy + 2))
                            && rc.onTheMap(new MapLocation(cx + 1, cy + 2)) && rc.sensePassability(new MapLocation(cx + 1, cy + 2))


                    ) {
                        best = 1;
                        rc.writeSharedArray(51, 1);
                    }
// east
                    if (best == -1
                            && rc.onTheMap(new MapLocation(cx + 2, cy - 1)) && rc.sensePassability(new MapLocation(cx + 2, cy - 1))
                            && rc.onTheMap(new MapLocation(cx + 2, cy    )) && rc.sensePassability(new MapLocation(cx + 2, cy    ))
                            && rc.onTheMap(new MapLocation(cx + 2, cy + 1)) && rc.sensePassability(new MapLocation(cx + 2, cy + 1))
                            && rc.onTheMap(new MapLocation(cx + 3, cy - 1)) && rc.sensePassability(new MapLocation(cx + 3, cy - 1))
                            && rc.onTheMap(new MapLocation(cx + 3, cy    )) && rc.sensePassability(new MapLocation(cx + 3, cy    ))
                            && rc.onTheMap(new MapLocation(cx + 3, cy + 1)) && rc.sensePassability(new MapLocation(cx + 3, cy + 1))
                            && rc.onTheMap(new MapLocation(cx + 4, cy - 1)) && rc.sensePassability(new MapLocation(cx + 4, cy - 1))
                            && rc.onTheMap(new MapLocation(cx + 4, cy    )) && rc.sensePassability(new MapLocation(cx + 4, cy    ))
                            && rc.onTheMap(new MapLocation(cx + 4, cy + 1)) && rc.sensePassability(new MapLocation(cx + 4, cy + 1))
                    ) {
                        best = 2;
                        rc.writeSharedArray(51, 1);

                    }
  // south
                    if (best == -1
                            && rc.onTheMap(new MapLocation(cx - 1, cy - 3)) && rc.sensePassability(new MapLocation(cx - 1, cy - 3))
                            && rc.onTheMap(new MapLocation(cx,     cy - 3)) && rc.sensePassability(new MapLocation(cx,     cy - 3))
                            && rc.onTheMap(new MapLocation(cx + 1, cy - 3)) && rc.sensePassability(new MapLocation(cx + 1, cy - 3))
                            && rc.onTheMap(new MapLocation(cx - 1, cy - 4)) && rc.sensePassability(new MapLocation(cx - 1, cy - 4))
                            && rc.onTheMap(new MapLocation(cx,     cy - 4)) && rc.sensePassability(new MapLocation(cx,     cy - 4))
                            && rc.onTheMap(new MapLocation(cx + 1, cy - 4)) && rc.sensePassability(new MapLocation(cx + 1, cy - 4))
                            && rc.onTheMap(new MapLocation(cx - 1, cy - 2)) && rc.sensePassability(new MapLocation(cx - 1, cy - 2))
                            && rc.onTheMap(new MapLocation(cx,     cy - 2)) && rc.sensePassability(new MapLocation(cx,     cy - 2))
                            && rc.onTheMap(new MapLocation(cx + 1, cy - 2)) && rc.sensePassability(new MapLocation(cx + 1, cy - 2))
                    ) {
                        best = 0;
                        rc.writeSharedArray(51, 1);

                    }

  // west
                    if (best == -1
                            && rc.onTheMap(new MapLocation(cx - 3, cy - 1)) && rc.sensePassability(new MapLocation(cx - 3, cy - 1))
                            && rc.onTheMap(new MapLocation(cx - 3, cy    )) && rc.sensePassability(new MapLocation(cx - 3, cy    ))
                            && rc.onTheMap(new MapLocation(cx - 3, cy + 1)) && rc.sensePassability(new MapLocation(cx - 3, cy + 1))
                            && rc.onTheMap(new MapLocation(cx - 4, cy - 1)) && rc.sensePassability(new MapLocation(cx - 4, cy - 1))
                            && rc.onTheMap(new MapLocation(cx - 4, cy    )) && rc.sensePassability(new MapLocation(cx - 4, cy    ))
                            && rc.onTheMap(new MapLocation(cx - 4, cy + 1)) && rc.sensePassability(new MapLocation(cx - 4, cy + 1))
                            && rc.onTheMap(new MapLocation(cx - 2, cy - 1)) && rc.sensePassability(new MapLocation(cx - 2, cy - 1))
                            && rc.onTheMap(new MapLocation(cx - 2, cy    )) && rc.sensePassability(new MapLocation(cx - 2, cy    ))
                            && rc.onTheMap(new MapLocation(cx - 2, cy + 1)) && rc.sensePassability(new MapLocation(cx - 2, cy + 1))

                    ) {
                        best = 3;
                        rc.writeSharedArray(51, 1);

                    }}



                    if (rc.readSharedArray(51) <= 4 && !produced) {

                        // ---------------- NORTH (best == 1) ----------------
                        if (best == 1) {

                            if (!rkMovedForward) {
                                if (rc.canMove(Direction.NORTH)) {
                                    rc.move(Direction.NORTH);
                                    rkMovedForward = true;
                                    int x = rc.readSharedArray(51);
                                    x++;
                                    rc.writeSharedArray(51, x);
                                }
                            }
                            else {
                                int s = rc.readSharedArray(50);

                                MapLocation md2 = rc.getLocation();
                                int x2 = md2.x;
                                int y2 = md2.y;

                                // (your code spawns at y2 - 2)
                                if (s == 1) {
                                    MapLocation sp = new MapLocation(x2 - 1, y2 - 2); // left
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 2);
                                    }
                                }
                                else if (s == 2) {
                                    MapLocation sp = new MapLocation(x2, y2 - 2); // middle
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 3);
                                    }
                                }
                                else if (s == 3) {
                                    MapLocation sp = new MapLocation(x2 + 1, y2 - 2); // right
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 4);
                                    }
                                }
                                else if (s == 4) {
                                    rkMovedForward = false;
                                    rc.writeSharedArray(50, 1);
                                }
                            }
                        }

                        // ---------------- SOUTH (best == 0) ----------------
                        if (best == 0) {

                            if (!rkMovedForward) {
                                if (rc.canMove(Direction.SOUTH)) {
                                    rc.move(Direction.SOUTH);
                                    rkMovedForward = true;
                                    int x = rc.readSharedArray(51);
                                    x++;
                                    rc.writeSharedArray(51, x);
                                }
                            }
                            else {
                                int s = rc.readSharedArray(50);

                                MapLocation md2 = rc.getLocation();
                                int x2 = md2.x;
                                int y2 = md2.y;

                                // mirror of NORTH: spawn at y2 + 2
                                if (s == 1) {
                                    MapLocation sp = new MapLocation(x2 - 1, y2 + 2); // left
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 2);
                                    }
                                }
                                else if (s == 2) {
                                    MapLocation sp = new MapLocation(x2, y2 + 2); // middle
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 3);
                                    }
                                }
                                else if (s == 3) {
                                    MapLocation sp = new MapLocation(x2 + 1, y2 + 2); // right
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 4);
                                    }
                                }
                                else if (s == 4) {
                                    rkMovedForward = false;
                                    rc.writeSharedArray(50, 1);
                                }
                            }
                        }

                        // ---------------- EAST (best == 2) ----------------
                        if (best == 2) {

                            if (!rkMovedForward) {
                                if (rc.canMove(Direction.EAST)) {
                                    rc.move(Direction.EAST);
                                    rkMovedForward = true;
                                    int x = rc.readSharedArray(51);
                                    x++;
                                    rc.writeSharedArray(51, x);
                                }
                            }
                            else {
                                int s = rc.readSharedArray(50);

                                MapLocation md2 = rc.getLocation();
                                int x2 = md2.x;
                                int y2 = md2.y;

                                // "in front" relative to EAST means x - 2 in your NORTH block style becomes x - 2 here.
                                // Left/mid/right relative to EAST corresponds to y-1, y, y+1.
                                if (s == 1) {
                                    MapLocation sp = new MapLocation(x2 - 2, y2 - 1); // left
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 2);
                                    }
                                }
                                else if (s == 2) {
                                    MapLocation sp = new MapLocation(x2 - 2, y2); // middle
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 3);
                                    }
                                }
                                else if (s == 3) {
                                    MapLocation sp = new MapLocation(x2 - 2, y2 + 1); // right
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 4);
                                    }
                                }
                                else if (s == 4) {
                                    rkMovedForward = false;
                                    rc.writeSharedArray(50, 1);
                                }
                            }
                        }

                        // ---------------- WEST (best == 3) ----------------
                        if (best == 3) {

                            if (!rkMovedForward) {
                                if (rc.canMove(Direction.WEST)) {
                                    rc.move(Direction.WEST);
                                    rkMovedForward = true;
                                    int x = rc.readSharedArray(51);
                                    x++;
                                    rc.writeSharedArray(51, x);
                                }
                            }
                            else {
                                int s = rc.readSharedArray(50);

                                MapLocation md2 = rc.getLocation();
                                int x2 = md2.x;
                                int y2 = md2.y;

                                // mirror of EAST: spawn at x2 + 2, and vary y-1/y/y+1
                                if (s == 1) {
                                    MapLocation sp = new MapLocation(x2 + 2, y2 - 1); // left
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 2);
                                    }
                                }
                                else if (s == 2) {
                                    MapLocation sp = new MapLocation(x2 + 2, y2); // middle
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 3);
                                    }
                                }
                                else if (s == 3) {
                                    MapLocation sp = new MapLocation(x2 + 2, y2 + 1); // right
                                    if (rc.canBuildRat(sp)) {
                                        rc.buildRat(sp);
                                        rc.writeSharedArray(50, 4);
                                    }
                                }
                                else if (s == 4) {
                                    rkMovedForward = false;
                                    rc.writeSharedArray(50, 1);
                                }
                            }
                        }

                    }
                    if (rc.readSharedArray(51) >=5) {produced = true; rc.writeSharedArray(51, 1);}
                    System.out.println(rc.readSharedArray(51));



                }


            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
        }

    }
}
