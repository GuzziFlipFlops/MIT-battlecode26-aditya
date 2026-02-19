package Hermesv8.nav;

import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.fast.*;

public class GradientBFS {
    private static final int MAX_DIST = 255;
    public static int[][] distHome = null;
    public static int[][] distTarget = null;
    private static FastQueue queue = new FastQueue(1000);
    private static boolean[] visited = new boolean[3600];

    public static void init() {
        if (distHome == null) {
            distHome = new int[Globals.mapWidth][Globals.mapHeight];
            distTarget = new int[Globals.mapWidth][Globals.mapHeight];
        }
    }

    public static void computeDistHome(MapLocation kingLoc) throws GameActionException {
        if (distHome == null) init();
        if (kingLoc == null) return;

        for (int i = 0; i < Globals.mapWidth * Globals.mapHeight; i++) {
            visited[i] = false;
            int x = i / Globals.mapHeight;
            int y = i % Globals.mapHeight;
            if (x < Globals.mapWidth && y < Globals.mapHeight) {
                distHome[x][y] = MAX_DIST;
            }
        }

        queue.clear();
        queue.add(kingLoc);
        int startIdx = kingLoc.x * Globals.mapHeight + kingLoc.y;
        visited[startIdx] = true;
        distHome[kingLoc.x][kingLoc.y] = 0;

        while (!queue.isEmpty() && Clock.getBytecodesLeft() > 500) {
            MapLocation current = queue.poll();
            if (current == null) break;
            int x = current.x;
            int y = current.y;
            int currentDist = distHome[x][y];

            if (currentDist >= MAX_DIST - 1) continue;

            for (Direction dir : Direction.allDirections()) {
                if (dir == Direction.CENTER) continue;
                int nx = x + dir.dx;
                int ny = y + dir.dy;

                if (nx < 0 || nx >= Globals.mapWidth || ny < 0 || ny >= Globals.mapHeight) continue;

                MapLocation nextLoc = new MapLocation(nx, ny);
                if (Navigation.map != null && Navigation.map[nx][ny] == Navigation.WALL) continue;
                if (Globals.rc.canSenseLocation(nextLoc)) {
                    if (!Globals.rc.sensePassability(nextLoc)) continue;
                } else if (Navigation.map != null && Navigation.map[nx][ny] != Navigation.EMPTY) {
                    continue;
                }

                int nextIdx = nx * Globals.mapHeight + ny;
                if (visited[nextIdx]) continue;

                visited[nextIdx] = true;
                distHome[nx][ny] = currentDist + 1;
                queue.add(nextLoc);
            }
        }
    }

    public static void computeDistTarget(MapLocation targetLoc) throws GameActionException {
        if (distTarget == null) init();
        if (targetLoc == null) return;

        for (int i = 0; i < Globals.mapWidth * Globals.mapHeight; i++) {
            visited[i] = false;
            int x = i / Globals.mapHeight;
            int y = i % Globals.mapHeight;
            if (x < Globals.mapWidth && y < Globals.mapHeight) {
                distTarget[x][y] = MAX_DIST;
            }
        }

        queue.clear();
        queue.add(targetLoc);
        int startIdx = targetLoc.x * Globals.mapHeight + targetLoc.y;
        visited[startIdx] = true;
        distTarget[targetLoc.x][targetLoc.y] = 0;

        while (!queue.isEmpty() && Clock.getBytecodesLeft() > 500) {
            MapLocation current = queue.poll();
            if (current == null) break;
            int x = current.x;
            int y = current.y;
            int currentDist = distTarget[x][y];

            if (currentDist >= MAX_DIST - 1) continue;

            for (Direction dir : Direction.allDirections()) {
                if (dir == Direction.CENTER) continue;
                int nx = x + dir.dx;
                int ny = y + dir.dy;

                if (nx < 0 || nx >= Globals.mapWidth || ny < 0 || ny >= Globals.mapHeight) continue;

                MapLocation nextLoc = new MapLocation(nx, ny);
                if (Navigation.map != null && Navigation.map[nx][ny] == Navigation.WALL) continue;
                if (Globals.rc.canSenseLocation(nextLoc)) {
                    if (!Globals.rc.sensePassability(nextLoc)) continue;
                } else if (Navigation.map != null && Navigation.map[nx][ny] != Navigation.EMPTY) {
                    continue;
                }

                int nextIdx = nx * Globals.mapHeight + ny;
                if (visited[nextIdx]) continue;

                visited[nextIdx] = true;
                distTarget[nx][ny] = currentDist + 1;
                queue.add(nextLoc);
            }
        }
    }

    public static int getDistHome(MapLocation loc) {
        if (distHome == null || loc == null) return MAX_DIST;
        if (loc.x < 0 || loc.x >= Globals.mapWidth || loc.y < 0 || loc.y >= Globals.mapHeight) return MAX_DIST;
        return distHome[loc.x][loc.y];
    }

    public static int getDistTarget(MapLocation loc) {
        if (distTarget == null || loc == null) return MAX_DIST;
        if (loc.x < 0 || loc.x >= Globals.mapWidth || loc.y < 0 || loc.y >= Globals.mapHeight) return MAX_DIST;
        return distTarget[loc.x][loc.y];
    }

    public static Direction pickGradientMoveHome(boolean congestionAware) throws GameActionException {
        return pickGradientMove(distHome, congestionAware);
    }

    public static Direction pickGradientMoveTarget(boolean congestionAware) throws GameActionException {
        return pickGradientMove(distTarget, congestionAware);
    }

    private static Direction pickGradientMove(int[][] distField, boolean congestionAware) throws GameActionException {
        if (distField == null || !Globals.rc.isMovementReady()) return null;

        MapLocation myLoc = Globals.myLoc;
        if (myLoc.x < 0 || myLoc.x >= Globals.mapWidth || myLoc.y < 0 || myLoc.y >= Globals.mapHeight) return null;

        int currentDist = distField[myLoc.x][myLoc.y];
        if (currentDist == 0 || currentDist >= MAX_DIST) return null;

        Direction bestDir = null;
        int bestDist = currentDist;
        int bestScore = Integer.MAX_VALUE;

        for (Direction dir : Direction.allDirections()) {
            if (dir == Direction.CENTER) continue;
            MapLocation nextLoc = myLoc.add(dir);
            if (nextLoc.x < 0 || nextLoc.x >= Globals.mapWidth || 
                nextLoc.y < 0 || nextLoc.y >= Globals.mapHeight) continue;

            if (Navigation.map != null && Navigation.map[nextLoc.x][nextLoc.y] == Navigation.WALL) continue;
            if (Globals.rc.canSenseLocation(nextLoc)) {
                if (!Globals.rc.sensePassability(nextLoc)) continue;
            } else if (Navigation.map != null && Navigation.map[nextLoc.x][nextLoc.y] != Navigation.EMPTY) {
                continue;
            }

            RobotInfo robot = Globals.rc.senseRobotAtLocation(nextLoc);
            if (robot != null && robot.getTeam() == Globals.myTeam) {
                if (congestionAware) continue;
            }

            int nextDist = distField[nextLoc.x][nextLoc.y];
            if (nextDist < bestDist) {
                int score = nextDist;
                if (congestionAware && robot != null) {
                    score += 10;
                }
                if (score < bestScore) {
                    bestScore = score;
                    bestDist = nextDist;
                    bestDir = dir;
                }
            }
        }

        return bestDir;
    }
}
