package Hermesv6.util;
import battlecode.common.*;
import Hermesv6.*;
public class MapUtil {
    public static int manhattanDistance(MapLocation a, MapLocation b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }
    public static boolean onMap(MapLocation loc) {
        return loc.x >= 0 && loc.x < Globals.mapWidth &&
               loc.y >= 0 && loc.y < Globals.mapHeight;
    }
    public static boolean onEdge(MapLocation loc) {
        return loc.x == 0 || loc.x == Globals.mapWidth - 1 ||
               loc.y == 0 || loc.y == Globals.mapHeight - 1;
    }
    public static MapLocation clampToMap(MapLocation loc) {
        int x = Math.max(0, Math.min(Globals.mapWidth - 1, loc.x));
        int y = Math.max(0, Math.min(Globals.mapHeight - 1, loc.y));
        return new MapLocation(x, y);
    }
    public static MapLocation rotationalMirror(MapLocation loc) {
        return new MapLocation(
            Globals.mapWidth - 1 - loc.x,
            Globals.mapHeight - 1 - loc.y
        );
    }
}