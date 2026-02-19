package Hermes.strategy;

import battlecode.common.*;
import Hermes.*;
import Hermes.comms.*;

public class StrategicDecisions {

    public static void make() throws GameActionException {
        BackstabEvaluator.evaluate();
        
        int phase = GamePhase.calculate();
        CommArray.setFlag(CommArray.FLAG_GAME_PHASE, phase);
    }
}
