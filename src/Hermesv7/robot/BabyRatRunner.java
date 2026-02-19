package Hermesv7.robot;
import battlecode.common.*;
import Hermesv7.*;
import Hermesv7.combat.*;
import Hermesv7.economy.*;
import Hermesv7.nav.*;
import Hermesv7.nav.Mover;
import Hermesv7.comms.*;
import Hermesv7.strategy.*;
public class BabyRatRunner {
    public static void run() throws GameActionException {
        CommArray.update();

        System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " at " + Globals.myLoc + " | Cheese: " + Globals.myCheese + " | Enemies nearby: " + (Globals.nearbyEnemies != null ? Globals.nearbyEnemies.length : 0));

        // Quick cheese delivery if next to king
        if (Globals.myCheese > 0 && Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                    MapLocation kingLoc = ally.getLocation();
                    if (Globals.rc.canTransferCheese(kingLoc, Globals.myCheese)) {
                        Globals.rc.transferCheese(kingLoc, Globals.myCheese);
                        System.out.println("[Hermesv7] ✓ Rat #" + Globals.myID + " delivered " + Globals.myCheese + " cheese to king at " + kingLoc);
                        return;
                    }
                }
            }
        }

        // PRIORITY: Detect and handle enemy rats!
        if (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0 && !Globals.isCooperation) {
            System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " found " + Globals.nearbyEnemies.length + " enemies!");
            if (handleEnemyEncounter()) {
                return;
            }
        }
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying != null) {
            handleCarryingRat(carrying);
            return;
        }
        int teamCheese = Globals.rc.getAllCheese();
        boolean emergencyMode = (teamCheese <= 600);
        int numKings = Math.max(1, Globals.numKnownAlliedKings);
        int cheeseNeededPerRound = Constants.RATKING_CHEESE_CONSUMPTION * numKings;
        int safeBuffer = cheeseNeededPerRound * 200;
        boolean lowCheese = (teamCheese < safeBuffer);
        if (Globals.myCheese > 0) {
            MapLocation nearestKing = null;
            int minDist = Integer.MAX_VALUE;
            String kingSource = "none";
            if (Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        MapLocation kingLoc = ally.getLocation();
                        int dist = Globals.myLoc.distanceSquaredTo(kingLoc);
                        if (dist < minDist) {
                            minDist = dist;
                            nearestKing = kingLoc;
                            kingSource = "nearbyAllies";
                        }
                    }
                }
            }
            if (nearestKing == null) {
                nearestKing = CommArray.getNearestAlliedKing();
                if (nearestKing != null) {
                    minDist = Globals.myLoc.distanceSquaredTo(nearestKing);
                    kingSource = "CommArray.getNearestAlliedKing";
                }
            }
            if (nearestKing == null && Globals.numKnownAlliedKings > 0) {
                nearestKing = Globals.knownAlliedKings[0];
                kingSource = "Globals.knownAlliedKings";
            }
            if (nearestKing != null) {
                MapLocation actualKingLoc = nearestKing;
                if (kingSource.equals("CommArray.getNearestAlliedKing") || kingSource.equals("Globals.knownAlliedKings")) {
                    if (Globals.rc.canSenseLocation(nearestKing)) {
                        RobotInfo[] nearbyRobots = Globals.rc.senseNearbyRobots(nearestKing, 8, Globals.myTeam);
                        for (RobotInfo robot : nearbyRobots) {
                            if (robot != null && robot.getType() == UnitType.RAT_KING) {
                                actualKingLoc = robot.getLocation();
                                break;
                            }
                        }
                    }
                }
                if (Globals.rc.canTransferCheese(actualKingLoc, Globals.myCheese)) {
                    int amount = Globals.myCheese;
                    Globals.rc.transferCheese(actualKingLoc, amount);
                    System.out.println("[Hermesv7] ✓ Rat #" + Globals.myID + " delivered " + amount + " cheese to king at " + actualKingLoc);
                    return;
                }
                if (Globals.rc.isMovementReady()) {
                    Direction dir = Globals.myLoc.directionTo(actualKingLoc);
                    if (dir != Direction.CENTER) {
                        if (Mover.canMoveInDirection(dir)) {
                            Mover.tryMove(dir);
                            return;
                        }
                        Navigator.navigateTo(actualKingLoc);
                    }
                    return;
                }
            } else {
                MapLocation kingFromSharedArray = null;
                try {
                    for (int i = 16; i <= 23; i++) {
                        int packed = Globals.rc.readSharedArray(i);
                        if (packed != 0) {
                            int x = packed / 100;
                            int y = packed % 100;
                            MapLocation testLoc = new MapLocation(x, y);
                            if (testLoc.x >= 0 && testLoc.x < Globals.mapWidth &&
                                testLoc.y >= 0 && testLoc.y < Globals.mapHeight) {
                                kingFromSharedArray = testLoc;
                                if (Globals.rc.canTransferCheese(kingFromSharedArray, Globals.myCheese)) {
                                    int amount = Globals.myCheese;
                                    Globals.rc.transferCheese(kingFromSharedArray, amount);
                                    System.out.println("[Hermesv7] Delivered " + amount + " cheese to king at " + kingFromSharedArray + " (direct shared array read)");
                                    return;
                                }
                                break;
                            }
                        }
                    }
                } catch (GameActionException e) {
                }
                if (kingFromSharedArray != null) {
                    if (Globals.rc.isMovementReady()) {
                        Navigator.navigateTo(kingFromSharedArray);
                        return;
                    }
                }
                if (Globals.roundNum % 50 == 0) {
                    System.out.println("[Hermesv7] WARNING: Have " + Globals.myCheese + " cheese but can't find king!");
                }
                if (Globals.rc.isMovementReady()) {
                    if (Globals.numKnownAlliedKings > 0) {
                        Navigator.navigateTo(Globals.knownAlliedKings[0]);
                    } else {
                        Navigator.navigateTo(Globals.mapCenter);
                    }
                    return;
                }
            }
            return;
        }
        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            FleeNav.fleeFromMultiple(Globals.getLocations(Globals.nearbyCats));
            return;
        }
        boolean kingNeedsCheese = emergencyMode || lowCheese;
        if (!kingNeedsCheese) {
            boolean kingNearby = false;
            if (Globals.myCheese > 0 && Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        int distToKing = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                        if (distToKing <= 16) {
                            kingNearby = true;
                            break;
                        }
                    }
                }
            }
            if (!kingNearby && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
                RobotInfo enemyKing = null;
                RobotInfo enemyRat = null;
                for (RobotInfo enemy : Globals.nearbyEnemies) {
                    if (enemy == null) continue;
                    if (enemy.getType() == UnitType.RAT_KING) {
                        enemyKing = enemy;
                        try {
                            CommArray.reportEnemyKing(enemy.getLocation());
                            BabyRatState.setTargetKing(enemy.getLocation());
                            Squeaker.squeakAttack(enemy.getLocation());
                        } catch (GameActionException e) {}
                        break;
                    } else if (enemy.getType() == UnitType.BABY_RAT && enemyRat == null) {
                        enemyRat = enemy;
                    }
                }
                if (enemyKing != null) {
                    MapLocation kingLoc = enemyKing.getLocation();
                    if (Globals.myLoc.isAdjacentTo(kingLoc)) {
                        if (Globals.rc.isActionReady() && Globals.rc.canAttack(kingLoc)) {
                            int cheeseToSpend = Math.min(Globals.myCheese, 21);
                            Globals.rc.attack(kingLoc, cheeseToSpend);
                            return;
                        }
                    } else if (Globals.rc.isMovementReady()) {
                        Navigator.navigateTo(kingLoc);
                        return;
                    }
                }
                if (enemyRat != null) {
                    MapLocation ratLoc = enemyRat.getLocation();
                    if (Globals.myLoc.isAdjacentTo(ratLoc)) {
                        if (Globals.rc.isActionReady() && Globals.rc.canAttack(ratLoc)) {
                            int cheeseToSpend = DamageCalculator.getCheeseForEnemy(enemyRat);
                            Globals.rc.attack(ratLoc, cheeseToSpend);
                            return;
                        }
                    } else if (Globals.rc.isMovementReady()) {
                        Navigator.navigateTo(ratLoc);
                        return;
                    }
                }
            }
            if (Globals.nearbyEnemies == null || Globals.nearbyEnemies.length == 0) {
                MapLocation knownEnemyKing = CommArray.getNearestEnemyKing();
                if (knownEnemyKing != null && Globals.rc.isMovementReady()) {
                    Navigator.navigateTo(knownEnemyKing);
                    return;
                }
            }
        }
        if (Globals.myCheese > 0) {
            if (tryDeliverCheese()) {
                return;
            }
            if (Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        MapLocation kingLoc = ally.getLocation();
                        if (Globals.rc.canTransferCheese(kingLoc, Globals.myCheese)) {
                            Globals.rc.transferCheese(kingLoc, Globals.myCheese);
                            System.out.println("[Hermesv7] ✓ Rat #" + Globals.myID + " delivered " + Globals.myCheese + " cheese to king at " + kingLoc);
                            return;
                        }
                    }
                }
            }
            MapLocation king = null;
            if (Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        king = ally.getLocation();
                        break;
                    }
                }
            }
            if (king == null) {
                king = CommArray.getNearestAlliedKing();
            }
            if (king == null && Globals.numKnownAlliedKings > 0) {
                king = Globals.knownAlliedKings[0];
            }
            if (king != null && Globals.rc.isMovementReady()) {
                Direction dir = Globals.myLoc.directionTo(king);
                if (dir != Direction.CENTER && Mover.canMoveInDirection(dir)) {
                    Mover.tryMove(dir);
                } else {
                    Navigator.navigateTo(king);
                }
            }
            return;
        }
        BabyRatState.update();
        if (FleeLogic.shouldFlee()) {
            FleeLogic.flee();
            return;
        }
        if (Globals.isCooperation && Clock.getBytecodesLeft() > 2000) {
            try {
                BackstabEvaluator.evaluate();
            } catch (GameActionException e) {
            }
        }
        BabyRatState.decideNextTask();
        switch (BabyRatState.getState()) {
            case ATTACKING_CAT:
                CatAttacker.attack();
                break;
            case ATTACKING_ENEMY:
                if (Globals.rc.isActionReady() && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
                    RobotInfo offensiveTarget = Ratnapper.findBestRatnapTarget(false);
                    if (offensiveTarget != null) {
                        if (Ratnapper.tryRatnap(offensiveTarget)) {
                            return;
                        }
                    }
                    tryRatnapVulnerableEnemy();
                }
                EnemyAttacker.attack();
                break;
            case DEFENDING:
                defendKing();
                break;
            case DELIVERING:
                if (Globals.myCheese > 0) {
                    if (tryDeliverCheese()) {
                        return;
                    }
                    if (Globals.rc.isMovementReady()) {
                        MapLocation king = null;
                        if (Globals.nearbyAllies != null) {
                            for (RobotInfo ally : Globals.nearbyAllies) {
                                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                                    king = ally.getLocation();
                                    break;
                                }
                            }
                        }
                        if (king == null) {
                            king = CommArray.getNearestAlliedKing();
                        }
                        if (king == null && Globals.numKnownAlliedKings > 0) {
                            king = Globals.knownAlliedKings[0];
                        }
                        if (king != null) {
                            Navigator.navigateTo(king);
                        } else {
                            Navigator.navigateTo(Globals.mapCenter);
                        }
                    }
                } else {
                    BabyRatState.setState(BabyRatStateType.IDLE);
                }
                break;
            case COLLECTING:
                OptimizedCheeseCollection.collectOptimized();
                break;
            case SCOUTING:
                if (Globals.rc.isMovementReady()) {
                    Explorer.explore();
                }
                break;
            case IDLE:
            default:
                if (Globals.myCheese > 0) {
                    if (tryDeliverCheese()) {
                        return;
                    }
                    if (Globals.rc.isMovementReady()) {
                        MapLocation king = CommArray.getNearestAlliedKing();
                        if (king == null && Globals.nearbyAllies != null) {
                            for (RobotInfo ally : Globals.nearbyAllies) {
                                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                                    king = ally.getLocation();
                                    break;
                                }
                            }
                        }
                        if (king != null) {
                            Navigator.navigateTo(king);
                        }
                    }
                } else {
                    OptimizedCheeseCollection.collectOptimized();
                }
                break;
        }
        if (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 2000) {
            if (!Globals.isCooperation && Globals.globalCheese >= 5) {
                MapLocation kingLoc = CommArray.getNearestAlliedKing();
                if (kingLoc != null && Globals.myLoc.distanceSquaredTo(kingLoc) <= 16) {
                    TrapPlacer.tryPlaceRatTrap();
                }
            }
        }
        try {
            Squeaker.processIncomingSqueaks();
        } catch (GameActionException e) {
        }
    }
    private static void defendKing() throws GameActionException {
        MapLocation kingLoc = CommArray.getNearestAlliedKing();
        if (kingLoc == null) {
            BabyRatState.setState(BabyRatStateType.IDLE);
            return;
        }
        int distToKing = Globals.myLoc.distanceSquaredTo(kingLoc);
        if (distToKing > 25) {
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(kingLoc);
            }
            return;
        }
        if (Globals.rc.isActionReady() && !Globals.isCooperation && Globals.nearbyEnemies != null) {
            RobotInfo defensiveTarget = Ratnapper.findBestRatnapTarget(true);
            if (defensiveTarget != null) {
                if (Ratnapper.tryRatnap(defensiveTarget)) {
                    RobotInfo carrying = Globals.rc.getCarrying();
                    if (carrying != null) {
                        if (Thrower.tryThrowAtCat()) {
                            return;
                        }
                        if (Thrower.tryThrowAwayFromKing(kingLoc)) {
                            return;
                        }
                    }
                    return;
                }
            }
        }
        if (Globals.isCooperation && Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            if (Clock.getBytecodesLeft() > 300) {
                try {
                    Squeaker.squeakToLureCatAwayFromKing(kingLoc);
                } catch (GameActionException e) {
                }
            }
        }
        if (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            EnemyAttacker.attack();
        } else if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            CatAttacker.attack();
        } else {
            BabyRatState.setState(BabyRatStateType.IDLE);
        }
    }
    @SuppressWarnings("unused")
    private static boolean tryAttackCatOptimized() throws GameActionException {
        RobotInfo nearestCat = Globals.nearbyCats[0];
        MapLocation catLoc = nearestCat.getLocation();
        if (Globals.myLoc.isAdjacentTo(catLoc)) {
            if (Globals.rc.canAttack(catLoc)) {
                int cheeseToSpend = 0;
                if (Globals.globalCheese > 500 && Globals.myCheese >= 2) {
                    cheeseToSpend = 2;
                }
                Globals.rc.attack(catLoc, cheeseToSpend);
                return true;
            }
        }
        if (Globals.rc.isMovementReady()) {
            Direction dir = Globals.myLoc.directionTo(catLoc);
            if (Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
                return true;
            } else if (Globals.rc.canTurn()) {
                Globals.rc.turn(dir);
                return true;
            }
        }
        return false;
    }
    @SuppressWarnings("unused")
    private static RobotInfo findBackstabTarget() {
        if (Globals.nearbyEnemies == null || Globals.nearbyEnemies.length == 0) return null;
        RobotInfo best = null;
        int bestScore = 0;
        for (int i = 0; i < Globals.nearbyEnemies.length && i < 5; i++) {
            RobotInfo enemy = Globals.nearbyEnemies[i];
            if (enemy.getType() == UnitType.BABY_RAT) {
                int score = 100 - enemy.getHealth();
                if (Globals.myLoc.isAdjacentTo(enemy.getLocation())) {
                    score += 50;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = enemy;
                }
            }
        }
        return best;
    }
    private static boolean tryDeliverCheese() throws GameActionException {
        MapLocation nearestKing = null;
        int minDist = Integer.MAX_VALUE;
        if (Globals.nearbyAllies != null) {
            for (int i = 0; i < Globals.nearbyAllies.length && i < 10; i++) {
                RobotInfo ally = Globals.nearbyAllies[i];
                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                    int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        nearestKing = ally.getLocation();
                    }
                }
            }
        }
        if (nearestKing == null) {
            nearestKing = CommArray.getNearestAlliedKing();
        }
        if (nearestKing != null) {
            MapLocation actualKingLoc = nearestKing;
            boolean fromNearbyAllies = false;
            if (Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING && ally.getLocation().equals(nearestKing)) {
                        fromNearbyAllies = true;
                        break;
                    }
                }
            }
            if (!fromNearbyAllies && Globals.rc.canSenseLocation(nearestKing)) {
                RobotInfo[] nearbyRobots = Globals.rc.senseNearbyRobots(nearestKing, 8, Globals.myTeam);
                for (RobotInfo robot : nearbyRobots) {
                    if (robot != null && robot.getType() == UnitType.RAT_KING) {
                        actualKingLoc = robot.getLocation();
                        break;
                    }
                }
            }
            if (Globals.rc.canTransferCheese(actualKingLoc, Globals.myCheese)) {
                int amount = Globals.myCheese;
                Globals.rc.transferCheese(actualKingLoc, amount);
                System.out.println("[Hermesv7] ✓ Rat #" + Globals.myID + " delivered " + amount + " cheese to king at " + actualKingLoc);
                return true;
            }
            if (Globals.rc.isMovementReady()) {
                Direction dir = Globals.myLoc.directionTo(actualKingLoc);
                if (dir != Direction.CENTER) {
                    if (Mover.canMoveInDirection(dir)) {
                        Mover.tryMove(dir);
                        return true;
                    }
                    Navigator.navigateTo(actualKingLoc);
                }
                return true;
            }
        }
        return false;
    }
    @SuppressWarnings("unused")
    private static boolean tryFindAndCollectCheese() throws GameActionException {
        if (Globals.nearbyMapInfos == null) return false;
        MapLocation nearestCheese = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < Globals.nearbyMapInfos.length && Clock.getBytecodesLeft() > 500; i++) {
            MapInfo info = Globals.nearbyMapInfos[i];
            if (info == null) continue;
            if (info.hasCheeseMine() && Clock.getBytecodesLeft() > 300 && Globals.myType == UnitType.RAT_KING) {
                CommArray.reportCheeseMine(info.getMapLocation());
            }
            if (info.getCheeseAmount() > 0) {
                MapLocation loc = info.getMapLocation();
                if (loc != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(loc);
                    if (dist < minDist) {
                        minDist = dist;
                        nearestCheese = loc;
                    }
                }
            }
        }
        if (nearestCheese != null) {
            if (Globals.myLoc.isAdjacentTo(nearestCheese) || Globals.myLoc.equals(nearestCheese)) {
                if (Globals.rc.canPickUpCheese(nearestCheese)) {
                    Globals.rc.pickUpCheese(nearestCheese);
                    return true;
                }
            }
            if (Globals.rc.isMovementReady()) {
                Direction dir = Globals.myLoc.directionTo(nearestCheese);
                if (Globals.rc.canMove(dir)) {
                    Globals.rc.move(dir);
                    return true;
                } else if (Globals.rc.canTurn()) {
                    Globals.rc.turn(dir);
                    return true;
                }
            }
            return true;
        }
        if (Globals.rc.isMovementReady()) {
            MapLocation cheeseMine = CommArray.getNearestCheeseMine();
            if (cheeseMine != null) {
                Navigator.navigateTo(cheeseMine);
                return true;
            }
            Explorer.explore();
            return true;
        }
        return false;
    }
    private static MapLocation findBestAdjacentTileToKing(MapLocation kingCenter) throws GameActionException {
        if (kingCenter == null) return null;
        MapLocation bestTile = null;
        int minDist = Integer.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                if (dx == 0 && dy == 0) continue;
                MapLocation candidate = new MapLocation(kingCenter.x + dx, kingCenter.y + dy);
                if (candidate.x >= 0 && candidate.x < Globals.mapWidth &&
                    candidate.y >= 0 && candidate.y < Globals.mapHeight) {
                    int distToCandidate = Globals.myLoc.distanceSquaredTo(candidate);
                    int distToKing = candidate.distanceSquaredTo(kingCenter);
                    if (distToKing <= 16) {
                        if (Globals.rc.canSenseLocation(candidate)) {
                            if (Globals.rc.sensePassability(candidate)) {
                                RobotInfo robot = Globals.rc.senseRobotAtLocation(candidate);
                                if (robot == null || robot.getType() == UnitType.RAT_KING) {
                                    if (distToCandidate < minDist) {
                                        minDist = distToCandidate;
                                        bestTile = candidate;
                                    }
                                }
                            }
                        } else {
                            if (distToCandidate < minDist) {
                                minDist = distToCandidate;
                                bestTile = candidate;
                            }
                        }
                    }
                }
            }
        }
        return bestTile != null ? bestTile : kingCenter;
    }
    private static boolean handleEnemyEncounter() throws GameActionException {
        // Find the best enemy to target
        RobotInfo bestEnemyRat = null;
        RobotInfo enemyKing = null;
        int closestDist = Integer.MAX_VALUE;

        for (RobotInfo enemy : Globals.nearbyEnemies) {
            if (enemy == null) continue;

            if (enemy.getType() == UnitType.RAT_KING) {
                enemyKing = enemy;
                System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " FOUND ENEMY KING at " + enemy.getLocation() + "!!!");
                // Squeak about the king!
                try {
                    Squeaker.squeakAttack(enemy.getLocation());
                    CommArray.reportEnemyKing(enemy.getLocation());
                } catch (GameActionException e) {}
            } else if (enemy.getType() == UnitType.BABY_RAT) {
                int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                if (dist < closestDist) {
                    closestDist = dist;
                    bestEnemyRat = enemy;
                }
            }
        }

        // Prioritize attacking enemy king if found
        if (enemyKing != null) {
            MapLocation kingLoc = enemyKing.getLocation();
            if (Globals.myLoc.isAdjacentTo(kingLoc) && Globals.rc.isActionReady()) {
                if (Globals.rc.canAttack(kingLoc)) {
                    int cheeseToSpend = Math.min(Globals.myCheese, 25);
                    Globals.rc.attack(kingLoc, cheeseToSpend);
                    System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " ATTACKING ENEMY KING with " + cheeseToSpend + " cheese!");
                    return true;
                }
            } else if (Globals.rc.isMovementReady()) {
                System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " moving towards enemy king at " + kingLoc);
                Navigator.navigateTo(kingLoc);
                return true;
            }
        }

        // Handle enemy baby rats
        if (bestEnemyRat != null) {
            MapLocation enemyLoc = bestEnemyRat.getLocation();
            System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " targeting enemy rat at " + enemyLoc);

            // Squeak about enemy rat
            try {
                Squeaker.squeakAttack(enemyLoc);
            } catch (GameActionException e) {}

            // Try to ratnap first (if adjacent)
            if (Globals.myLoc.isAdjacentTo(enemyLoc) && Globals.rc.isActionReady()) {
                if (Globals.rc.canCarryRat(enemyLoc)) {
                    Globals.rc.carryRat(enemyLoc);
                    System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " RATNAPPED enemy rat!");
                    return true;
                }

                // If can't ratnap, bite!
                if (Globals.rc.canAttack(enemyLoc)) {
                    int cheeseToSpend = DamageCalculator.getCheeseForEnemy(bestEnemyRat);
                    Globals.rc.attack(enemyLoc, cheeseToSpend);
                    System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " BITING enemy rat with " + cheeseToSpend + " cheese!");
                    return true;
                }
            }

            // Move towards enemy
            if (Globals.rc.isMovementReady()) {
                System.out.println("[DEBUG BabyRat] Rat #" + Globals.myID + " moving towards enemy rat at " + enemyLoc);
                Navigator.navigateTo(enemyLoc);
                return true;
            }
        }

        return false;
    }

    private static void handleCarryingRat(RobotInfo carrying) throws GameActionException {
        if (carrying.getTeam() == Globals.myTeam) {
            if (tryDeliverCheese()) {
                return;
            }
            if (Globals.rc.isMovementReady()) {
                MapLocation king = CommArray.getNearestAlliedKing();
                if (king != null) {
                    MapLocation targetTile = findBestAdjacentTileToKing(king);
                    if (targetTile != null) {
                        Navigator.navigateTo(targetTile);
                    } else {
                        Navigator.navigateTo(king);
                    }
                }
            }
            return;
        }
        if (carrying.getTeam() != Globals.myTeam) {
            if (Thrower.tryThrowAtCat()) {
                return;
            }
            if (Thrower.tryThrowAtEnemyKing()) {
                return;
            }
            if (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
                RobotInfo nearestEnemy = Globals.nearbyEnemies[0];
                if (nearestEnemy != null) {
                    Direction toEnemy = Globals.myLoc.directionTo(nearestEnemy.getLocation());
                    if (Globals.myDir != toEnemy && Globals.rc.canTurn()) {
                        Globals.rc.turn(toEnemy);
                    }
                    if (Globals.rc.canThrowRat()) {
                        Globals.rc.throwRat();
                        return;
                    }
                }
            }
            if (Globals.rc.canThrowRat()) {
                Globals.rc.throwRat();
                return;
            }
        } else {
            if (Globals.rc.canDropRat(Direction.CENTER)) {
                Globals.rc.dropRat(Direction.CENTER);
            } else {
                for (Direction dir : Globals.ALL_DIRECTIONS) {
                    if (dir != Direction.CENTER && Globals.rc.canDropRat(dir)) {
                        Globals.rc.dropRat(dir);
                        return;
                    }
                }
            }
        }
    }
    private static void tryRatnapVulnerableEnemy() throws GameActionException {
        if (Globals.isCooperation) return;
        if (Globals.nearbyEnemies == null) return;
        RobotInfo bestTarget = null;
        int bestScore = 0;
        for (RobotInfo enemy : Globals.nearbyEnemies) {
            if (enemy == null || enemy.getType() != UnitType.BABY_RAT) continue;
            if (Ratnapper.canRatnap(enemy)) {
                int score = 100 - enemy.getHealth();
                if (enemy.getHealth() < Globals.myHealth) score += 50;
                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = enemy;
                }
            }
        }
        if (bestTarget != null) {
            Ratnapper.tryRatnap(bestTarget);
        }
    }
    @SuppressWarnings("unused")
    private static boolean tryPickupNearbyCheese() throws GameActionException {
        if (Globals.nearbyMapInfos == null) return false;
        for (int i = 0; i < Globals.nearbyMapInfos.length && i < 20; i++) {
            MapInfo info = Globals.nearbyMapInfos[i];
            if (info != null && info.getCheeseAmount() > 0) {
                MapLocation cheeseLoc = info.getMapLocation();
                if (cheeseLoc != null && Globals.rc.canPickUpCheese(cheeseLoc)) {
                    Globals.rc.pickUpCheese(cheeseLoc);
                    return true;
                }
            }
        }
        return false;
    }
}