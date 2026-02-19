package Hermesv7.economy;
import Hermesv7.*;
import Hermesv7.strategy.*;
public class AdaptiveCheeseEconomy {
    public static int calculateMinBuffer(int roundsLeft) {
        if (roundsLeft > 1500) {
            return Constants.RATKING_CHEESE_CONSUMPTION * 500 + 200;
        } else if (roundsLeft > 1000) {
            return Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 2) + 300;
        } else if (roundsLeft > 500) {
            return Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft * 2 / 3) + 400;
        } else if (roundsLeft > 100) {
            return Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft * 4 / 5) + 500;
        } else {
            return Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 30);
        }
    }
    public static int calculateSpawnThreshold(int roundsLeft, int excessCheese, int phase) {
        if (phase == GamePhase.PHASE_EARLY) {
            if (roundsLeft > 1500) {
                return -50;
            } else if (roundsLeft > 1000) {
                return 0;
            } else {
                return 20;
            }
        } else if (phase == GamePhase.PHASE_MID) {
            if (excessCheese > 150) {
                return 0;
            } else if (excessCheese > 0) {
                return 0;
            } else {
                return 30;
            }
        } else {
            if (excessCheese > 100) {
                return 0;
            } else if (roundsLeft > 200) {
                return 50;
            } else {
                return 100;
            }
        }
    }
    public static int calculateTrapThreshold(int roundsLeft, int phase) {
        if (phase == GamePhase.PHASE_EARLY) {
            return 30;
        } else if (phase == GamePhase.PHASE_MID) {
            return 20;
        } else {
            return 30;
        }
    }
    public static boolean canAffordSpawn(int availableCheese, int spawnCost, int roundsLeft, int phase) {
        int minBuffer = calculateMinBuffer(roundsLeft);
        if (availableCheese < spawnCost + minBuffer) {
            return false;
        }
        int excessCheese = availableCheese - minBuffer - spawnCost;
        int threshold = calculateSpawnThreshold(roundsLeft, excessCheese, phase);
        return excessCheese >= threshold;
    }
    public static boolean canAffordTrap(int availableCheese, int trapCost, int roundsLeft, int phase) {
        int minBuffer = calculateMinBuffer(roundsLeft);
        if (availableCheese < trapCost + minBuffer) {
            return false;
        }
        int excessCheese = availableCheese - minBuffer - trapCost;
        int threshold = calculateTrapThreshold(roundsLeft, phase);
        return excessCheese >= threshold;
    }
    public static int getMaxSpawnsPerTurn(int roundsLeft, int excessCheese, int phase) {
        if (phase == GamePhase.PHASE_EARLY && roundsLeft > 1500 && excessCheese > 300) {
            return 2;
        } else if (phase == GamePhase.PHASE_EARLY && excessCheese > 200) {
            return 2;
        } else if (excessCheese > 300) {
            return 2;
        } else {
            return 1;
        }
    }
    public static boolean isInCrisis(int availableCheese, int roundsLeft) {
        int minBuffer = calculateMinBuffer(roundsLeft);
        return availableCheese < minBuffer + 50;
    }
    public static boolean isEconomyHealthy(int availableCheese, int roundsLeft) {
        int minBuffer = calculateMinBuffer(roundsLeft);
        int excess = availableCheese - minBuffer;
        return excess >= 200;
    }
}