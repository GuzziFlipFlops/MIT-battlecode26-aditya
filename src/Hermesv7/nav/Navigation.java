package Hermesv7.nav;
import Hermesv7.*;
import battlecode.common.*;
import Hermesv7.fast.FastQueue;
public class Navigation {
    static final int UNKNOWN = 0;
    static final int RUIN = 1;
    static final int WALL = 2;
    static final int EMPTY = 3;
    static final int ALLY = 4;
    static final int ENEMY = 5;
    public static final int H_SYM = 0;
    public static final int V_SYM = 1;
    public static final int R_SYM = 2;
    static final int SYMM_BYTECODE = 5000;
    private static FastQueue symmQueue = new FastQueue(500);
    static int[][] map;
    protected static RobotController rc = Globals.rc;
    protected static MapLocation currentLocation = Globals.myLoc;
    protected static MapInfo[] visibleTiles = Globals.nearbyMapInfos;
    protected static int MAP_WIDTH = Globals.mapWidth;
    protected static int MAP_HEIGHT = Globals.mapHeight;
    protected static boolean isHSYM = false;
    protected static boolean isVSYM = false;
    protected static boolean isRSYM = false;
    public static void init(RobotController r) {
        map = new int[MAP_WIDTH][MAP_HEIGHT];
    }
    public static void scout() {
        if (visibleTiles == null) return;
        for (MapInfo tile : visibleTiles) {
            if (tile != null) {
                updateTile(tile);
            }
        }
    }
    public static void updateTile(MapInfo m) {
        if (m == null) return;
        int type;
        MapLocation pos = m.getMapLocation();
        if (pos == null) return;
        if (m.isWall()) {
            type = WALL;
        } else {
            type = EMPTY;
        }
        if (pos.x >= 0 && pos.x < MAP_WIDTH && pos.y >= 0 && pos.y < MAP_HEIGHT) {
            if (type != map[pos.x][pos.y]) {
                map[pos.x][pos.y] = type;
                symmQueue.add(pos);
            }
        }
    }
    public static void updateSymm() throws GameActionException {
        while (!symmQueue.isEmpty()) {
            if (Clock.getBytecodesLeft() < SYMM_BYTECODE) return;
            MapLocation pos = symmQueue.poll();
            if (pos == null) break;
            int curType = map[pos.x][pos.y];
            switch (curType) {
                case RUIN:
                    if (isHSYM) {
                        MapLocation nxt = getHSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == WALL || map[nxt.x][nxt.y] == EMPTY)) {
                            isHSYM = false;
                        }
                    }
                    if (isVSYM) {
                        MapLocation nxt = getVSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == WALL || map[nxt.x][nxt.y] == EMPTY)) {
                            isVSYM = false;
                        }
                    }
                    if (isRSYM) {
                        MapLocation nxt = getRSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == WALL || map[nxt.x][nxt.y] == EMPTY)) {
                            isRSYM = false;
                        }
                    }
                    break;
                case WALL:
                    if (isHSYM) {
                        MapLocation nxt = getHSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == RUIN || map[nxt.x][nxt.y] >= EMPTY)) {
                            isHSYM = false;
                        }
                    }
                    if (isVSYM) {
                        MapLocation nxt = getVSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == RUIN || map[nxt.x][nxt.y] >= EMPTY)) {
                            isVSYM = false;
                        }
                    }
                    if (isRSYM) {
                        MapLocation nxt = getRSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == RUIN || map[nxt.x][nxt.y] >= EMPTY)) {
                            isRSYM = false;
                        }
                    }
                    break;
                case EMPTY:
                case ALLY:
                case ENEMY:
                    if (isHSYM) {
                        MapLocation nxt = getHSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == WALL || map[nxt.x][nxt.y] == RUIN)) {
                            isHSYM = false;
                        }
                    }
                    if (isVSYM) {
                        MapLocation nxt = getVSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == WALL || map[nxt.x][nxt.y] == RUIN)) {
                            isVSYM = false;
                        }
                    }
                    if (isRSYM) {
                        MapLocation nxt = getRSym(pos);
                        if (rc.onTheMap(nxt) && (map[nxt.x][nxt.y] == WALL || map[nxt.x][nxt.y] == RUIN)) {
                            isRSYM = false;
                        }
                    }
                    break;
            }
        }
    }
    private static MapLocation getHSym(MapLocation loc) {
        return new MapLocation(MAP_WIDTH - 1 - loc.x, loc.y);
    }
    private static MapLocation getVSym(MapLocation loc) {
        return new MapLocation(loc.x, MAP_HEIGHT - 1 - loc.y);
    }
    private static MapLocation getRSym(MapLocation loc) {
        return new MapLocation(MAP_WIDTH - 1 - loc.x, MAP_HEIGHT - 1 - loc.y);
    }
    protected static boolean passable(RobotController rc, Direction d) throws GameActionException {
        MapLocation adjacentLocation = rc.getLocation().add(d);
        if (!rc.canSenseLocation(adjacentLocation)) {
            return false;
        }
        return rc.sensePassability(adjacentLocation) && !rc.canSenseRobotAtLocation(adjacentLocation);
    }
    protected static boolean safe(RobotController rc, Direction d) throws GameActionException {
        if (!passable(rc, d)) {
            return false;
        }
        MapLocation adjacentLocation = rc.getLocation().add(d);
        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null && adjacentLocation.distanceSquaredTo(cat.getLocation()) <= 16) {
                    return false;
                }
            }
        }
        return true;
    }
    protected static void updateMovement() {
        currentLocation = Globals.myLoc;
    }
    protected static boolean isValidMapLocation(MapLocation loc) {
        return loc.x >= 0 && loc.x < MAP_WIDTH && loc.y >= 0 && loc.y < MAP_HEIGHT;
    }
}