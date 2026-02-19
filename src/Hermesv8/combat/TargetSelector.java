package Hermesv8.combat;
import battlecode.common.*;
import Hermesv8.*;
public class TargetSelector {
    public static RobotInfo selectBestTarget() {
        RobotInfo bestTarget = null;
        int bestScore = Integer.MIN_VALUE;
        for (RobotInfo cat : Globals.nearbyCats) {
            int score = scoreCatTarget(cat);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = cat;
            }
        }
        if (!Globals.isCooperation) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                int score = scoreEnemyTarget(enemy);
                if (score > bestScore) {
                    bestScore = score;
                    bestTarget = enemy;
                }
            }
        }
        return bestTarget;
    }
    public static RobotInfo selectEnemyTarget() {
        RobotInfo best = null;
        int bestScore = Integer.MIN_VALUE;
        for (RobotInfo enemy : Globals.nearbyEnemies) {
            int score = scoreEnemyTarget(enemy);
            if (score > bestScore) {
                bestScore = score;
                best = enemy;
            }
        }
        return best;
    }
    private static int scoreCatTarget(RobotInfo cat) {
        int score = 1000;
        int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
        score -= dist * 5;
        if (dist <= 2) {
            score += 500;
        }
        score -= cat.getHealth() / 100;
        return score;
    }
    private static int scoreEnemyTarget(RobotInfo enemy) {
        int score = 500;
        int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
        score -= dist * 10;
        if (dist <= 2) {
            score += 300;
        }
        score -= enemy.getHealth() * 2;
        score += enemy.getRawCheeseAmount() * 5;
        if (enemy.getType() == UnitType.RAT_KING) {
            score += 200;
            score -= 300;
        }
        if (Ratnapper.canRatnap(enemy)) {
            score += 150;
        }
        return score;
    }
}