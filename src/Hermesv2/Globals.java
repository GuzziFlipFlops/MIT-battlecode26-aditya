package Hermesv2;

import battlecode.common.*;
import Hermesv2.comms.*;

public class Globals {

    public static RobotController rc;
    public static int myID;
    public static Team myTeam;
    public static Team enemyTeam;
    public static UnitType myType;
    public static int mapWidth;
    public static int mapHeight;
    public static MapLocation mapCenter;
    public static int roundNum;
    public static MapLocation myLoc;
    public static Direction myDir;
    public static int myHealth;
    public static int myCheese;
    public static int globalCheese;
    public static int teamDirt;
    public static boolean isCooperation;
    public static RobotInfo[] nearbyAllies;
    public static RobotInfo[] nearbyEnemies;
    public static RobotInfo[] nearbyCats;
    public static MapInfo[] nearbyMapInfos;
    public static MapLocation[] knownCheeseMines = new MapLocation[10];
    public static int numKnownCheeseMines = 0;
    public static MapLocation[] knownEnemyKings = new MapLocation[5];
    public static int numKnownEnemyKings = 0;
    public static MapLocation[] knownAlliedKings = new MapLocation[5];
    public static int numKnownAlliedKings = 0;
    
    public static MapLocation catSpawnLocation;

    public static final Direction[] ALL_DIRECTIONS = Direction.allDirections();
    public static final Direction[] CARDINAL_DIRECTIONS = Direction.cardinalDirections();

    public static void init(RobotController robotController) {
        rc = robotController;
        myID = rc.getID();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myType = rc.getType();
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        mapCenter = new MapLocation(mapWidth / 2, mapHeight / 2);
        catSpawnLocation = mapCenter;
        CommArray.init();
    }

    public static void update() throws GameActionException {
        roundNum = rc.getRoundNum();
        myLoc = rc.getLocation();
        myDir = rc.getDirection();
        myHealth = rc.getHealth();
        myCheese = rc.getRawCheese();
        globalCheese = rc.getGlobalCheese();
        teamDirt = rc.getDirt();
        isCooperation = rc.isCooperation();
        updateNearbySensing();
        CommArray.update();
    }

    private static void updateNearbySensing() throws GameActionException {
        RobotInfo[] allNearby = rc.senseNearbyRobots();
        if (allNearby == null) {
            allNearby = new RobotInfo[0];
        }
        
        int allyCount = 0, enemyCount = 0, catCount = 0;
        
        for (RobotInfo robot : allNearby) {
            if (robot == null) continue;
            UnitType type = robot.getType();
            if (type == UnitType.CAT) {
                catCount++;
            } else if (robot.getTeam() == myTeam) {
                allyCount++;
            } else {
                enemyCount++;
            }
        }
        
        nearbyAllies = new RobotInfo[allyCount];
        nearbyEnemies = new RobotInfo[enemyCount];
        nearbyCats = new RobotInfo[catCount];
        int ai = 0, ei = 0, ci = 0;
        
        for (RobotInfo robot : allNearby) {
            if (robot == null) continue;
            UnitType type = robot.getType();
            if (type == UnitType.CAT) {
                nearbyCats[ci++] = robot;
            } else if (robot.getTeam() == myTeam) {
                nearbyAllies[ai++] = robot;
            } else {
                nearbyEnemies[ei++] = robot;
            }
        }
        
        nearbyMapInfos = rc.senseNearbyMapInfos();
        if (nearbyMapInfos == null) {
            nearbyMapInfos = new MapInfo[0];
        }
        
        if (nearbyAllies == null) nearbyAllies = new RobotInfo[0];
        if (nearbyEnemies == null) nearbyEnemies = new RobotInfo[0];
        if (nearbyCats == null) nearbyCats = new RobotInfo[0];
    }

    public static int getSpawnCost() throws GameActionException {
        return rc.getCurrentRatCost();
    }
    
    public static boolean isNearCatSpawn(MapLocation loc, int radiusSq) {
        return loc.distanceSquaredTo(catSpawnLocation) <= radiusSq;
    }
}
