package Hermesv5.robot;
import battlecode.common.*;
import Hermesv5.*;
import Hermesv5.combat.*;
import Hermesv5.economy.*;
import Hermesv5.nav.*;
import Hermesv5.comms.*;
import Hermesv5.strategy.*;
import Hermesv5.util.Rand;
public class BabyRatRunner {
    public static void run() throws GameActionException {
        RobotInfo carrying = Globals.rc.getCarrying();
        if (carrying != null) {
            handleCarryingRat(carrying);
            return;
        }
        if (Globals.myCheese > 0) {
            MapLocation nearestKing = CommArray.getNearestAlliedKing();
            if (nearestKing == null && Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.RAT_KING) {
                        nearestKing = ally.getLocation();
                        break;
                    }
                }
            }
            if (nearestKing != null) {
                int dist = Globals.myLoc.distanceSquaredTo(nearestKing);
                if (dist <= GameConstants.CHEESE_DROP_RADIUS_SQUARED) {
                    if (Globals.rc.isActionReady() && Globals.rc.canTransferCheese(nearestKing, Globals.myCheese)) {
                        Globals.rc.transferCheese(nearestKing, Globals.myCheese);
                        return;
                    }
                }
                if (Globals.rc.isMovementReady()) {
                    Navigator.navigateTo(nearestKing);
                    return;
                }
            }
        }
        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            FleeNav.fleeFromMultiple(Globals.getLocations(Globals.nearbyCats));
            return;
        }
        int teamCheese = Globals.rc.getAllCheese();
        int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 1000;
        boolean kingNeedsCheese = (teamCheese < minBuffer);
        if (!kingNeedsCheese && Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy == null) continue;
                if (enemy.getType() == UnitType.RAT_KING || enemy.getType() == UnitType.BABY_RAT) {
                    try {
                        if (enemy.getType() == UnitType.RAT_KING) {
                            CommArray.reportEnemyKing(enemy.getLocation());
                            BabyRatState.setTargetKing(enemy.getLocation());
                            Squeaker.squeakAttack(enemy.getLocation());
                        }
                    } catch (GameActionException e) {}
                    if (Globals.myLoc.isAdjacentTo(enemy.getLocation())) {
                        if (Globals.rc.isActionReady() && Globals.rc.canAttack(enemy.getLocation())) {
                            int cheeseToSpend = DamageCalculator.getCheeseForEnemy(enemy);
                            Globals.rc.attack(enemy.getLocation(), cheeseToSpend);
                            return;
                        }
                    } else if (Globals.rc.isMovementReady()) {
                        Navigator.navigateTo(enemy.getLocation());
                        return;
                    }
                }
            }
        }
        if (!kingNeedsCheese) {
            MapLocation knownEnemyKing = CommArray.getNearestEnemyKing();
            if (knownEnemyKing != null) {
                if (Globals.rc.isMovementReady()) {
                    Navigator.navigateTo(knownEnemyKing);
                    return;
                }
            }
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
                    if (!tryDeliverCheese()) {
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
                    tryDeliverCheese();
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
            int dist = Globals.myLoc.distanceSquaredTo(nearestKing);
            if (dist <= GameConstants.CHEESE_DROP_RADIUS_SQUARED) {
                if (Globals.rc.canTransferCheese(nearestKing, Globals.myCheese)) {
                    int amount = Globals.myCheese;
                    Globals.rc.transferCheese(nearestKing, amount);
                    System.out.println("Delivered " + amount + " cheese");
                    if (Clock.getBytecodesLeft() > 500) {
                        Squeaker.squeakCheese(nearestKing);
                    }
                    return true;
                }
            }
            if (Globals.rc.isMovementReady()) {
                Navigator.navigateTo(nearestKing);
                return true;
            }
        }
        return false;
    }
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
    private static void handleCarryingRat(RobotInfo carrying) throws GameActionException {
        if (carrying.getTeam() == Globals.myTeam) {
            if (tryDeliverCheese()) {
                return;
            }
            if (Globals.rc.isMovementReady()) {
                MapLocation king = CommArray.getNearestAlliedKing();
                if (king != null) {
                    Navigator.navigateTo(king);
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