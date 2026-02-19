package Hermesv8.robot;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.combat.*;
import Hermesv8.economy.*;
import Hermesv8.nav.*;
import Hermesv8.nav.Mover;
import Hermesv8.comms.*;
import Hermesv8.strategy.*;
import Hermesv8.util.DebugLogger;
public class BabyRatRunner {
    public static void run() throws GameActionException {
        try {
        // Check for promotion signal first
        if (KingExpansion.checkForPromotion()) {
            return; // Became a king, will run KingManager next turn
        }

        if (Globals.roundNum % 100 == 0 && Globals.myID % 10 == 0) {
            System.out.println("[Rat #" + Globals.myID + "] Round " + Globals.roundNum + ": State=" + RatState.getState() + ", Cheese=" + Globals.myCheese + ", Loc=" + Globals.myLoc);
        }
        CommArray.update();
        if (Clock.getBytecodesLeft() > 200) {
            Navigation.scout();
        }

        // Update tracking systems
        if (Clock.getBytecodesLeft() > 500) {
            try {
                CheeseTracker.update();
                EnemyTracker.update();
            } catch (Exception e) {}
        }

        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying != null) {
            handleCarryingRat(carrying);
            return;
        }

        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            FleeNav.fleeFromMultiple(Globals.getLocations(Globals.nearbyCats));
            return;
        }
        
        boolean kingNeedsCheese = false;
        try {
            int teamCheese = Globals.rc.getAllCheese();
            int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 200;
            kingNeedsCheese = (teamCheese < minBuffer);
            int flag = CommArray.getFlag(CommArray.FLAG_DEFEND_KING);
            if (flag == 2) kingNeedsCheese = true;
        } catch (GameActionException e) {}

        if (Globals.myCheese > 0) {
            MapLocation nearestKing = findNearestKingForDelivery();
            if (nearestKing == null && Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        nearestKing = ally.getLocation();
                        break;
                    }
                }
            }
            if (nearestKing != null) {
                // Try to transfer cheese if possible (canTransferCheese handles range check)
                if (Globals.rc.isActionReady() && Globals.rc.canTransferCheese(nearestKing, Globals.myCheese)) {
                    Globals.rc.transferCheese(nearestKing, Globals.myCheese);
                    System.out.println("[Hermesv8] ✓ Rat #" + Globals.myID + " delivered " + Globals.myCheese + " cheese");
                    return;
                }
                if (Globals.rc.isMovementReady()) {
                    Navigator.navigateTo(nearestKing);
                    return;
                }
            } else if (Globals.roundNum % 100 == 0) {
                System.out.println("[Rat #" + Globals.myID + "] Has cheese but can't find king!");
            }
        }

        RatState.update();
        RatStateType state = RatState.getState();

        if (Globals.myCheese > 0) {
            RatState.setState(RatStateType.RETURN);
            state = RatStateType.RETURN;
        } else if (kingNeedsCheese) {
            RatState.setState(RatStateType.GATHER);
            state = RatStateType.GATHER;
        } else if (state == RatStateType.GATHER && !kingNeedsCheese && Globals.myCheese == 0) {
            RatState.update();
            state = RatState.getState();
        }
        
        if ((state == RatStateType.FIGHT || (state == RatStateType.RETURN && Globals.myCheese > 0)) && Globals.rc.getCarrying() == null && !kingNeedsCheese) {
            if (Globals.rc.isActionReady()) {
                RobotInfo allyToGrab = findBestAlliedRatToGrab();
                if (allyToGrab != null && Ratnapper.canRatnap(allyToGrab)) {
                    if (Ratnapper.tryRatnap(allyToGrab)) {
                        return;
                    }
                }
            }
        }

        switch (state) {
            case RETURN:
                handleReturnState();
                break;
            case RETREAT:
                handleRetreatState();
                break;
            case FIGHT:
                handleFightState();
                break;
            case GATHER:
            default:
                handleGatherState();
                break;
        }
        } catch (GameActionException e) {
            DebugLogger.logException("BabyRatRunner.run", e, "main_loop");
            throw e;
        } catch (Exception e) {
            DebugLogger.logException("BabyRatRunner.run", e, "unexpected_error");
        }
    }

    private static MapLocation findNearestKingForDelivery() {
        MapLocation nearestKing = null;
        int minDist = Integer.MAX_VALUE;

        if (Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
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

        if (nearestKing == null && Globals.numKnownAlliedKings > 0) {
            nearestKing = Globals.knownAlliedKings[0];
        }

        return nearestKing;
    }

    private static void handleReturnState() throws GameActionException {
        MapLocation nearestKing = findNearestKingForDelivery();
        if (nearestKing == null && Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.RAT_KING) {
                    nearestKing = ally.getLocation();
                    break;
                }
            }
        }
        if (nearestKing == null) {
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(Globals.mapCenter);
            }
            return;
        }

        if (Globals.rc.isActionReady() && Globals.rc.canTransferCheese(nearestKing, Globals.myCheese)) {
            Globals.rc.transferCheese(nearestKing, Globals.myCheese);
            return;
        }

        if (Globals.rc.isMovementReady()) {
            Navigator.navigateTo(nearestKing);
        } else if (Globals.rc.canTurn()) {
            Direction dir = Globals.myLoc.directionTo(nearestKing);
            if (dir != Direction.CENTER && Globals.rc.getDirection() != dir) {
                Globals.rc.turn(dir);
            }
        }
    }

    private static void handleRetreatState() throws GameActionException {
        if (Globals.rc.isMovementReady()) {
            MapLocation nearestKing = findNearestKingForDelivery();
            if (nearestKing != null) {
                Navigator.navigateTo(nearestKing);
            } else {
                Navigator.navigateTo(Globals.mapCenter);
            }
        }
    }

    private static void handleFightState() throws GameActionException {
        if (Globals.myCheese > 0) {
            RatState.setState(RatStateType.RETURN);
            handleReturnState();
            return;
        }

        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying != null) {
            if (carrying.getTeam() == Globals.myTeam) {
                tryThrowAlliedRatAtEnemy();
                return;
            } else {
                handleCarryingRat(carrying);
            return;
            }
        }

        if (Globals.rc.isActionReady()) {
            RobotInfo allyToGrab = findBestAlliedRatToGrab();
            if (allyToGrab != null && Ratnapper.canRatnap(allyToGrab)) {
                if (Ratnapper.tryRatnap(allyToGrab)) {
                    return;
                }
            }
        }

        if (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
            RobotInfo target = null;
            int bestPriority = -1;

            // Prioritize: enemy kings > weak rats > other rats
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null) {
                    int priority = 0;
                    if (enemy.getType() == UnitType.RAT_KING) {
                        priority = 10000 - enemy.getHealth(); // Kings are top priority
                        try {
                            CommArray.reportEnemyKing(enemy.getLocation());
                        } catch (Exception e) {}
                    } else if (enemy.getType() == UnitType.BABY_RAT) {
                        priority = 1000 - enemy.getHealth(); // Weak rats next
                    }

                    if (priority > bestPriority) {
                        bestPriority = priority;
                        target = enemy;
                    }
                }
            }

            if (target != null) {
                MapLocation targetLoc = target.getLocation();
                if (Globals.myLoc.isAdjacentTo(targetLoc)) {
                    if (Globals.rc.isActionReady() && Globals.rc.canAttack(targetLoc)) {
                        // Spend max cheese on enemy kings (21 is max)
                        int cheeseToSpend = (target.getType() == UnitType.RAT_KING) ?
                            Math.min(Globals.myCheese, 21) :
                            DamageCalculator.getCheeseForEnemy(target);
                        Globals.rc.attack(targetLoc, cheeseToSpend);
                        if (target.getType() == UnitType.RAT_KING) {
                            System.out.println("[Hermesv8] ⚔ Rat #" + Globals.myID + " hit enemy king with " + cheeseToSpend + " cheese!");
                        }
                        return;
                    }
                } else if (Globals.rc.isMovementReady()) {
                    Direction dir = Globals.myLoc.directionTo(targetLoc);
                    if (Globals.rc.canMove(dir)) {
                        Globals.rc.move(dir);
                    } else {
                        Navigator.navigateTo(targetLoc);
                    }
                    return;
                }
            }
        }

        // Use EnemyTracker to find best attack target
        MapLocation attackTarget = EnemyTracker.getBestAttackTarget();
        if (attackTarget == null) {
            attackTarget = getAttackTarget(); // Fallback to old method
        }

        if (attackTarget != null && Globals.rc.isMovementReady()) {
            Navigator.navigateTo(attackTarget);
        }
    }

    private static void handleGatherState() throws GameActionException {
        if (Globals.myCheese > 0) {
            RatState.setState(RatStateType.RETURN);
            handleReturnState();
            return;
        }

        // PRIORITY 1: Pick up adjacent cheese
        MapLocation adjacentCheese = CheesePathfinder.getAdjacentCheese();
        if (adjacentCheese != null && Globals.rc.isActionReady()) {
            if (Globals.rc.canPickUpCheese(adjacentCheese)) {
                int amount = 0;
                for (MapInfo info : Globals.nearbyMapInfos) {
                    if (info != null && info.getMapLocation().equals(adjacentCheese)) {
                        amount = info.getCheeseAmount();
                        break;
                    }
                }
                Globals.rc.pickUpCheese(adjacentCheese);
                System.out.println("[Hermesv8] ✓ Rat #" + Globals.myID + " picked up " + amount + " cheese");
                RatState.setState(RatStateType.RETURN);
                return;
            }
        }

        // PRIORITY 2: Navigate to best cheese using improved pathfinding
        if (Globals.rc.isMovementReady()) {
            if (CheesePathfinder.navigateToCheese()) {
                return;
            }
        }

        // PRIORITY 3: Fallback to basic exploration
        if (Globals.rc.isMovementReady()) {
            Direction dir = Globals.myLoc.directionTo(Globals.mapCenter);
            if (Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
            } else {
                Direction[] dirs = {dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
                for (Direction d : dirs) {
                    if (Globals.rc.canMove(d)) {
                        Globals.rc.move(d);
                        break;
                    }
                }
            }
        }
    }

    private static MapLocation findNearestCheeseLocation() {
        if (Globals.nearbyMapInfos == null) return null;
        MapLocation nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < Globals.nearbyMapInfos.length && Clock.getBytecodesLeft() > 200; i++) {
            MapInfo info = Globals.nearbyMapInfos[i];
            if (info != null && info.getCheeseAmount() > 0) {
                MapLocation loc = info.getMapLocation();
                if (loc != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(loc);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = loc;
                    }
                }
            }
        }
        if (nearest == null && Globals.numKnownCheeseMines > 0) {
            for (int i = 0; i < Globals.numKnownCheeseMines; i++) {
                MapLocation mine = Globals.knownCheeseMines[i];
                if (mine != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(mine);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = mine;
                    }
                }
            }
        }
        return nearest;
    }

    private static MapLocation getAttackTarget() {
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) return enemyKing;

        try {
            int targetX = CommArray.getFlag(CommArray.ATTACK_TARGET_X);
            int targetY = CommArray.getFlag(CommArray.ATTACK_TARGET_Y);
            if (targetX > 0 && targetY > 0 && targetX < Globals.mapWidth && targetY < Globals.mapHeight) {
                return new MapLocation(targetX, targetY);
            }
        } catch (GameActionException e) {
            DebugLogger.logException("getAttackTarget", e, "read_attack_target");
        }

        return Globals.mapCenter;
    }

    private static void handleCarryingRat(RobotInfo carrying) throws GameActionException {
        if (carrying.getTeam() == Globals.myTeam) {
            if (tryThrowAlliedRatAtEnemy()) {
                return;
            }
            if (Globals.rc.isMovementReady()) {
                MapLocation enemyKing = CommArray.getNearestEnemyKing();
                if (enemyKing != null) {
                    Navigator.navigateTo(enemyKing);
                } else if (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
                    Navigator.navigateTo(Globals.nearbyEnemies[0].getLocation());
                } else {
                    MapLocation king = CommArray.getNearestAlliedKing();
                    if (king != null) {
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
        }
    }

    private static RobotInfo findBestAlliedRatToGrab() {
        if (Globals.nearbyAllies == null) return null;
        RobotInfo bestAlly = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo ally : Globals.nearbyAllies) {
            if (ally == null || ally.getType() != UnitType.BABY_RAT) continue;
            if (ally.getTeam() != Globals.myTeam) continue;
            if (ally.getHealth() < 20) continue;
            int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
            if (dist <= 2 && dist < minDist) {
                minDist = dist;
                bestAlly = ally;
            }
        }
        return bestAlly;
    }

    private static boolean tryThrowAlliedRatAtEnemy() throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying == null || carrying.getTeam() != Globals.myTeam) return false;

        MapLocation enemyKingLoc = null;

        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    enemyKingLoc = enemy.getLocation();
                break;
                }
            }
        }

        if (enemyKingLoc == null) {
            enemyKingLoc = CommArray.getNearestEnemyKing();
        }

        if (enemyKingLoc == null) {
            MapLocation attackTarget = getAttackTarget();
            if (attackTarget != null) {
                enemyKingLoc = attackTarget;
            }
        }

        if (enemyKingLoc != null) {
            Direction toKing = Globals.myLoc.directionTo(enemyKingLoc);
            if (Globals.myDir != toKing && Globals.rc.canTurn()) {
                Globals.rc.turn(toKing);
            }
            if (Globals.rc.canThrowRat()) {
                Globals.rc.throwRat();
                return true;
            }
        }

        RobotInfo bestEnemy = null;
        int minDist = Integer.MAX_VALUE;

        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy == null) continue;
                int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                if (dist <= 25 && dist < minDist) {
                    minDist = dist;
                    bestEnemy = enemy;
                }
            }
        }

        if (bestEnemy != null) {
            Direction toEnemy = Globals.myLoc.directionTo(bestEnemy.getLocation());
            if (Globals.myDir != toEnemy && Globals.rc.canTurn()) {
                Globals.rc.turn(toEnemy);
            }
            if (Globals.rc.canThrowRat()) {
                Globals.rc.throwRat();
                return true;
            }
        }

        if (Globals.rc.canThrowRat()) {
            Globals.rc.throwRat();
            return true;
        }

        return false;
    }

    @SuppressWarnings("unused")
    private static void defendKing() throws GameActionException {
        MapLocation kingLoc = CommArray.getNearestAlliedKing();
        if (kingLoc == null) {
            BabyRatState.setState(BabyRatStateType.IDLE);
            return;
        }
        int distToKing = Globals.myLoc.distanceSquaredTo(kingLoc);
        if (distToKing > 25) {
            if (Globals.rc.isMovementReady()) {
                DebugLogger.logNav("navigateTo", kingLoc, null, false, "called");
                Navigator.navigateTo(kingLoc);
            }
            return;
        }
        if (Globals.rc.isActionReady() && !Globals.isCooperation && Globals.nearbyEnemies != null) {
            RobotInfo defensiveTarget = Ratnapper.findBestRatnapTarget(true);
            if (defensiveTarget != null) {
                if (Ratnapper.tryRatnap(defensiveTarget)) {
                    RobotInfo carrying = Globals.rc.getCarrying();
            DebugLogger.logAction("check_carrying", carrying != null, carrying != null ? "carrying:" + carrying.getID() : "not_carrying");
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
                    DebugLogger.logException("squeakToLureCatAwayFromKing", e, "squeak_failed");
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
                DebugLogger.logCombat("attack", null, cheeseToSpend, true);
                Globals.rc.attack(catLoc, cheeseToSpend);
                return true;
            }
        }
        if (Globals.rc.isMovementReady()) {
            Direction dir = Globals.myLoc.directionTo(catLoc);
            if (Globals.rc.canMove(dir)) {
                DebugLogger.logNav("move", null, dir, true, "moved");
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
                DebugLogger.logDelivery("transfer_attempt", amount, actualKingLoc, false);
                Globals.rc.transferCheese(actualKingLoc, amount);
                DebugLogger.logDelivery("delivered", amount, actualKingLoc, true);
                System.out.println("[Hermesv8] ✓ Rat #" + Globals.myID + " delivered " + amount + " cheese to king at " + actualKingLoc);
                if (Clock.getBytecodesLeft() > 500) {
                    Squeaker.squeakCheese(actualKingLoc);
                }
                return true;
            }
            if (Globals.rc.isMovementReady()) {
                Direction dir = Globals.myLoc.directionTo(actualKingLoc);
                if (dir != Direction.CENTER) {
                    if (Mover.canMoveInDirection(dir)) {
                        Mover.tryMove(dir);
                        return true;
                    }
                    DebugLogger.logNav("navigateTo", actualKingLoc, null, false, "called");
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

            if (Globals.myCheese >= Constants.CARRY_CAP) {
                DebugLogger.logCheese("at_cap", Globals.myCheese, Globals.myLoc);
                DebugLogger.logState("BabyRatRunner", RatState.getState().toString(), "RETURN");
                return false;
            }
            if (Globals.myLoc.isAdjacentTo(nearestCheese) || Globals.myLoc.equals(nearestCheese)) {
                if (Globals.rc.canPickUpCheese(nearestCheese)) {
                    DebugLogger.logCollection("pickup_attempt", nearestCheese, 0, false);
                    Globals.rc.pickUpCheese(nearestCheese);
                DebugLogger.logCollection("picked_up", nearestCheese, Globals.myCheese, true);
                    return true;
                }
            }
            if (Globals.rc.isMovementReady()) {
                Direction dir = Globals.myLoc.directionTo(nearestCheese);
                if (Globals.rc.canMove(dir)) {
                    DebugLogger.logNav("move", null, dir, true, "moved");
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
                DebugLogger.logNav("navigateTo", cheeseMine, null, false, "called");
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
            DebugLogger.logCombat("ratnap_attempt", bestTarget, 0, false);
            Ratnapper.tryRatnap(bestTarget);
                DebugLogger.logCombat("ratnap_success", bestTarget, 0, true);
        }
    }
    @SuppressWarnings("unused")
    private static boolean tryPickupNearbyCheese() throws GameActionException {

        if (Globals.myCheese >= Constants.CARRY_CAP) {
                DebugLogger.logCheese("at_cap", Globals.myCheese, Globals.myLoc);
                DebugLogger.logState("BabyRatRunner", RatState.getState().toString(), "RETURN");
            return false;
        }
        if (Globals.nearbyMapInfos == null) return false;
        for (int i = 0; i < Globals.nearbyMapInfos.length && i < 20; i++) {
            MapInfo info = Globals.nearbyMapInfos[i];
            if (info != null && info.getCheeseAmount() > 0) {
                MapLocation cheeseLoc = info.getMapLocation();
                if (cheeseLoc != null && Globals.rc.canPickUpCheese(cheeseLoc)) {
                    DebugLogger.logCollection("pickup_attempt", cheeseLoc, 0, false);
                    Globals.rc.pickUpCheese(cheeseLoc);
                DebugLogger.logCollection("picked_up", cheeseLoc, Globals.myCheese, true);
                    return true;
                }
            }
        }
        return false;
    }
}