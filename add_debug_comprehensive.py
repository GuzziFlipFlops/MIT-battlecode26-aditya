#!/usr/bin/env python3
import re
import os

def add_comprehensive_debug(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if 'BabyRatRunner' in filepath:
        content = add_debug_babyratrunner(content)
    elif 'KingManager' in filepath:
        content = add_debug_kingmanager(content)
    elif 'RatState' in filepath and 'Type' not in filepath:
        content = add_debug_ratstate(content)
    elif 'CommArray' in filepath:
        content = add_debug_commarray(content)
    elif 'GradientBFS' in filepath:
        content = add_debug_gradientbfs(content)
    
    with open(filepath, 'w') as f:
        f.write(content)

def add_debug_babyratrunner(content):
    if 'import Hermesv8.util.DebugLogger;' not in content:
        content = content.replace('import Hermesv8.strategy.*;', 'import Hermesv8.strategy.*;\nimport Hermesv8.util.DebugLogger;')
    
    content = re.sub(r'public static void run\(\) throws GameActionException \{', 
                     r'public static void run() throws GameActionException {\n        try {\n            DebugLogger.logRatStatus(Globals.myCheese, Globals.myHealth, RatState.getState().toString());\n            DebugLogger.logVision("run_start", Globals.nearbyAllies != null ? Globals.nearbyAllies.length : 0, Globals.nearbyMapInfos != null ? Globals.nearbyMapInfos.length : 0);', content)
    
    content = re.sub(r'CommArray\.update\(\);', 
                     r'CommArray.update();\n            DebugLogger.logAction("CommArray.update", true, "updated");', content)
    
    content = re.sub(r'Navigation\.scout\(\);', 
                     r'Navigation.scout();\n                DebugLogger.logAction("Navigation.scout", true, "scouted");', content)
    
    content = re.sub(r'RobotInfo carrying = Globals\.rc\.getCarrying\(\);', 
                     r'RobotInfo carrying = Globals.rc.getCarrying();\n            DebugLogger.logAction("check_carrying", carrying != null, carrying != null ? "carrying:" + carrying.getID() : "not_carrying");', content)
    
    content = re.sub(r'if \(Globals\.myCheese >= Constants\.CARRY_CAP\) \{', 
                     r'if (Globals.myCheese >= Constants.CARRY_CAP) {\n                DebugLogger.logCheese("at_cap", Globals.myCheese, Globals.myLoc);\n                DebugLogger.logState("BabyRatRunner", RatState.getState().toString(), "RETURN");', content)
    
    content = re.sub(r'Globals\.rc\.transferCheese\(([^,]+),\s*([^)]+)\);', 
                     r'DebugLogger.logDelivery("transfer_attempt", \2, \1, false);\n                Globals.rc.transferCheese(\1, \2);\n                DebugLogger.logDelivery("delivered", \2, \1, true);', content)
    
    content = re.sub(r'Globals\.rc\.pickUpCheese\(([^)]+)\);', 
                     r'DebugLogger.logCollection("pickup_attempt", \1, 0, false);\n                Globals.rc.pickUpCheese(\1);\n                DebugLogger.logCollection("picked_up", \1, Globals.myCheese, true);', content)
    
    content = re.sub(r'RatState\.update\(\);', 
                     r'RatStateType oldStateBeforeUpdate = RatState.getState();\n            RatState.update();\n            RatStateType newStateAfterUpdate = RatState.getState();\n            if (oldStateBeforeUpdate != newStateAfterUpdate) {\n                DebugLogger.logState("RatState.update", oldStateBeforeUpdate.toString(), newStateAfterUpdate.toString());\n            }', content)
    
    content = re.sub(r'Navigator\.navigateTo\(([^)]+)\);', 
                     r'DebugLogger.logNav("navigateTo", \1, null, false, "called");\n                Navigator.navigateTo(\1);', content)
    
    content = re.sub(r'Globals\.rc\.move\(([^)]+)\);', 
                     r'DebugLogger.logNav("move", null, \1, true, "moved");\n                Globals.rc.move(\1);', content)
    
    content = re.sub(r'Globals\.rc\.attack\(([^,]+),\s*([^)]+)\);', 
                     r'DebugLogger.logCombat("attack", null, \2, true);\n                Globals.rc.attack(\1, \2);', content)
    
    content = re.sub(r'Globals\.rc\.throwRat\(\);', 
                     r'DebugLogger.logCombat("throw_rat", null, 0, true);\n                Globals.rc.throwRat();', content)
    
    content = re.sub(r'Ratnapper\.tryRatnap\(([^)]+)\);', 
                     r'DebugLogger.logCombat("ratnap_attempt", \1, 0, false);\n                Ratnapper.tryRatnap(\1);\n                DebugLogger.logCombat("ratnap_success", \1, 0, true);', content)
    
    if '} catch (GameActionException' not in content and '} catch (Exception' not in content:
        content = re.sub(r'(\s+)(break;\s*}\s*})', 
                         r'\1}\n        } catch (GameActionException e) {\n            DebugLogger.logException("BabyRatRunner.run", e, "main_loop");\n            throw e;\n        } catch (Exception e) {\n            DebugLogger.logException("BabyRatRunner.run", e, "unexpected_error");\n        }', content, count=1)
    
    return content

def add_debug_kingmanager(content):
    if 'import Hermesv8.util.DebugLogger;' not in content:
        content = content.replace('import Hermesv8.strategy.*;', 'import Hermesv8.strategy.*;\nimport Hermesv8.util.DebugLogger;')
    
    content = re.sub(r'public static void manage\(\) throws GameActionException \{', 
                     r'public static void manage() throws GameActionException {\n        try {\n            DebugLogger.logKingStatus(Globals.rc.getAllCheese(), Globals.myHealth, Constants.MAX_ROUNDS - Globals.roundNum);', content)
    
    content = re.sub(r'CommArray\.setMode\(([^)]+)\);', 
                     r'int oldMode = CommArray.getMode();\n            CommArray.setMode(\1);\n            DebugLogger.logMode(oldMode, \1, "mode_change");', content)
    
    content = re.sub(r'Globals\.rc\.buildRat\(([^)]+)\);', 
                     r'DebugLogger.logSpawn("buildRat", \1, Globals.getSpawnCost(), false, "attempting");\n                Globals.rc.buildRat(\1);\n                DebugLogger.logSpawn("buildRat", \1, Globals.getSpawnCost(), true, "success");', content)
    
    content = re.sub(r'GradientBFS\.computeDistHome\(([^)]+)\);', 
                     r'int bytecodesBefore = Clock.getBytecodeNum();\n                GradientBFS.computeDistHome(\1);\n                int bytecodesAfter = Clock.getBytecodeNum();\n                DebugLogger.logGradient("computeDistHome", \1, true, bytecodesAfter - bytecodesBefore);', content)
    
    content = re.sub(r'GradientBFS\.computeDistTarget\(([^)]+)\);', 
                     r'int bytecodesBefore = Clock.getBytecodeNum();\n                GradientBFS.computeDistTarget(\1);\n                int bytecodesAfter = Clock.getBytecodeNum();\n                DebugLogger.logGradient("computeDistTarget", \1, true, bytecodesAfter - bytecodesBefore);', content)
    
    if '} catch (GameActionException' not in content:
        content = re.sub(r'(\s+)(return;\s*})', 
                         r'\1}\n        } catch (GameActionException e) {\n            DebugLogger.logException("KingManager.manage", e, "main_loop");\n            throw e;\n        } catch (Exception e) {\n            DebugLogger.logException("KingManager.manage", e, "unexpected_error");\n        }', content, count=1)
    
    return content

def add_debug_ratstate(content):
    if 'import Hermesv8.util.DebugLogger;' not in content:
        content = content.replace('import Hermesv8.comms.*;', 'import Hermesv8.comms.*;\nimport Hermesv8.util.DebugLogger;')
    
    content = re.sub(r'setState\(RatStateType\.([^)]+)\);', 
                     r'RatStateType oldStateDebug = state;\n            setState(RatStateType.\1);\n            if (oldStateDebug != state) {\n                DebugLogger.logState("RatState", oldStateDebug.toString(), state.toString());\n            }', content)
    
    return content

def add_debug_commarray(content):
    if 'import Hermesv8.util.DebugLogger;' not in content:
        content = content.replace('import Hermesv8.*;', 'import Hermesv8.*;\nimport Hermesv8.util.DebugLogger;')
    
    content = re.sub(r'Globals\.rc\.writeSharedArray\(([^,]+),\s*([^)]+)\);', 
                     r'DebugLogger.logComm("writeSharedArray", \1, \2, true);\n            Globals.rc.writeSharedArray(\1, \2);', content)
    
    content = re.sub(r'Globals\.rc\.readSharedArray\(([^)]+)\);', 
                     r'int value = Globals.rc.readSharedArray(\1);\n            DebugLogger.logComm("readSharedArray", \1, value, true);\n            value', content)
    
    return content

def add_debug_gradientbfs(content):
    if 'import Hermesv8.util.DebugLogger;' not in content:
        content = content.replace('import Hermesv8.fast.FastQueue;', 'import Hermesv8.fast.FastQueue;\nimport Hermesv8.util.DebugLogger;')
    
    return content

for root, dirs, files in os.walk('src/Hermesv8'):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            try:
                add_comprehensive_debug(filepath)
                print(f"Processed {filepath}")
            except Exception as e:
                print(f"Error processing {filepath}: {e}")

print("Done adding comprehensive debug logging!")
