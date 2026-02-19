package Hermes.strategy;

import battlecode.common.*;
import Hermes.*;
import Hermes.comms.*;

public class BackstabEvaluator {

    private static boolean backstabRecommended = false;
    private static int ourDamage = 0;
    private static int estimatedTotalDamage = 0;

    public static void evaluate() throws GameActionException {
        if (!Globals.isCooperation) {
            backstabRecommended = false;
            return;
        }
        
        if (Globals.roundNum < 200) {
            backstabRecommended = false;
            return;
        }
        
        ourDamage = CommArray.getTotalCatDamage();
        estimatedTotalDamage = Math.max(ourDamage * 2, 1000);
        
        double catDamageAdvantage = calculateDamageAdvantage();
        double kingAdvantage = calculateKingAdvantage();
        double cheeseAdvantage = calculateCheeseAdvantage();
        
        double coopScore = 0.5 * catDamageAdvantage + 0.3 * kingAdvantage + 0.2 * cheeseAdvantage;
        double backstabScore = 0.3 * catDamageAdvantage + 0.5 * kingAdvantage + 0.2 * cheeseAdvantage;
        
        if (backstabScore > coopScore + 0.1) {
            backstabRecommended = true;
            CommArray.signalBackstab();
        }
        
        int phase = GamePhase.calculate();
        if (phase == GamePhase.PHASE_LATE && Globals.roundNum > 1500) {
            backstabRecommended = true;
            CommArray.signalBackstab();
        }
        
        if (Globals.nearbyCats.length == 0 && Globals.roundNum > 500 && phase == GamePhase.PHASE_MID) {
            if (kingAdvantage > 0.6) {
                backstabRecommended = true;
                CommArray.signalBackstab();
            }
        }
    }

    private static double calculateDamageAdvantage() {
        if (estimatedTotalDamage == 0) return 0.5;
        return (double) ourDamage / estimatedTotalDamage;
    }

    private static double calculateKingAdvantage() {
        int ourKings = Globals.numKnownAlliedKings;
        int theirKings = Globals.numKnownEnemyKings;
        
        if (ourKings == 0 && theirKings == 0) return 0.5;
        if (theirKings == 0) return 1.0;
        if (ourKings == 0) return 0.0;
        
        int totalKings = ourKings + theirKings;
        int ourHealth = 0;
        int theirHealth = 0;
        
        for (int i = 0; i < Globals.nearbyAllies.length && i < 10; i++) {
            RobotInfo ally = Globals.nearbyAllies[i];
            if (ally.getType() == UnitType.RAT_KING) {
                ourHealth += ally.getHealth();
            }
        }
        
        for (int i = 0; i < Globals.nearbyEnemies.length && i < 10; i++) {
            RobotInfo enemy = Globals.nearbyEnemies[i];
            if (enemy.getType() == UnitType.RAT_KING) {
                theirHealth += enemy.getHealth();
            }
        }
        
        if (ourHealth + theirHealth > 0) {
            return (double) ourHealth / (ourHealth + theirHealth);
        }
        
        return (double) ourKings / totalKings;
    }

    private static double calculateCheeseAdvantage() throws GameActionException {
        int ourTransfer = CommArray.getTotalCheeseTransfer();
        int estimatedTotal = Math.max(ourTransfer * 2, 100);
        if (estimatedTotal == 0) return 0.5;
        return Math.min((double) ourTransfer / estimatedTotal, 1.0);
    }

    public static boolean isRecommended() {
        return backstabRecommended;
    }
}
