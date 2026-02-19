package Hermesv4.combat;

import battlecode.common.*;
import Hermesv4.*;

public class DamageCalculator {

    public static final int BASE_DAMAGE = GameConstants.RAT_BITE_DAMAGE;

    public static int getCheeseForCat() throws GameActionException {
        if (Globals.globalCheese > 800 && Globals.myCheese >= 3) {
            return 3;
        }
        if (Globals.globalCheese > 500 && Globals.myCheese >= 2) {
            return 2;
        }
        return 0;
    }

    public static int getCheeseForEnemy(RobotInfo target) throws GameActionException {
        int targetHealth = target.getHealth();
        boolean isRatKing = (target.getType() == UnitType.RAT_KING);
        
        
        
        
        
        if (targetHealth <= BASE_DAMAGE) {
            return 0; 
        }
        
        int damageNeeded = targetHealth - BASE_DAMAGE;
        
        
        int maxCheese = isRatKing ? 21 : 8;
        int availableCheese = Math.min(Globals.myCheese, maxCheese);
        
        
        if (damageNeeded >= 4 && availableCheese >= 21) {
            return 21; 
        } else if (damageNeeded >= 3 && availableCheese >= 8) {
            return 8; 
        } else if (damageNeeded >= 2 && availableCheese >= 3) {
            return 3; 
        } else if (damageNeeded >= 1 && availableCheese >= 2) {
            return 2; 
        }
        
        
        if (isRatKing && availableCheese >= 3 && targetHealth <= BASE_DAMAGE + 3) {
            return 3;
        }
        
        return 0;
    }

    public static int calculateDamage(int cheeseSpent) {
        if (cheeseSpent >= 21) return BASE_DAMAGE + 4;
        if (cheeseSpent >= 8) return BASE_DAMAGE + 3;
        if (cheeseSpent >= 3) return BASE_DAMAGE + 2;
        if (cheeseSpent >= 2) return BASE_DAMAGE + 1;
        return BASE_DAMAGE;
    }
}
