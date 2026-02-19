package Hermesv8.economy;
import battlecode.common.*;
import Hermesv8.*;
public class CheeseEconomy {
    public static final int SURVIVAL_RESERVE = 300;
    public static final int SPAWN_RESERVE = 500;
    public static final int TRAP_RESERVE = 100;
    public static int getSpendableCheese() throws GameActionException {
        int total = Globals.globalCheese;
        int reserve = calculateReserve();
        return Math.max(0, total - reserve);
    }
    private static int calculateReserve() {
        int reserve = 0;
        int roundsLeft = GameConstants.GAME_MAX_NUMBER_OF_ROUNDS - Globals.roundNum;
        int survivalNeeded = Math.min(3 * roundsLeft, SURVIVAL_RESERVE);
        reserve += survivalNeeded;
        reserve += SPAWN_RESERVE;
        if (Globals.roundNum > 200) {
            reserve += TRAP_RESERVE;
        }
        return reserve;
    }
    public static boolean canAfford(int cost) throws GameActionException {
        return getSpendableCheese() >= cost;
    }
    public static boolean isEconomyHealthy() throws GameActionException {
        return Globals.globalCheese > SURVIVAL_RESERVE + SPAWN_RESERVE;
    }
    public static boolean isInCrisis() throws GameActionException {
        return Globals.globalCheese < 30;
    }
}