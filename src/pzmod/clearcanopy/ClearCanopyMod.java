package pzmod.clearcanopy;

import java.util.ArrayList;

import zombie.ZomboidFileSystem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;

public class ClearCanopyMod {

    // Public/volatile: read from advice inlined into IsoTree's access
    // context; Lua-less mod, render thread is the only writer.
    public static volatile int cacheFrameCount = Integer.MIN_VALUE;
    public static volatile int cachePlayerIndex = Integer.MIN_VALUE;
    public static volatile boolean cacheActive = false;

    private static volatile boolean phantomTraceLogged = false;

    // Per-frame memo around isModActive so the mod-list scan runs once
    // per frame, not once per rendered tree.
    public static boolean isActiveForCurrentFrame() {
        int fCount = IsoCamera.frameState.frameCount;
        int pIdx = IsoCamera.frameState.playerIndex;
        if (fCount == cacheFrameCount && pIdx == cachePlayerIndex) {
            return cacheActive;
        }
        cacheFrameCount = fCount;
        cachePlayerIndex = pIdx;
        cacheActive = isModActive();
        return cacheActive;
    }

    // Self-check against ZombieBuddy advice persistence (filed as
    // zed-0xff/ZombieBuddy#13): once ZB scans this mod's @Patch class,
    // the advice stays in the global registry for the JVM lifetime even
    // after ClearCanopy is removed from the active mod set. PZ shares
    // one JVM across world reloads, so gate on live mod-set membership
    // via getModIDs. Returns true on detection failure so legitimate
    // sessions never lose the fix when getModIDs throws.
    private static boolean isModActive() {
        boolean active = true;
        try {
            ArrayList<String> modIds = ZomboidFileSystem.instance.getModIDs();
            if (modIds != null) {
                active = false;
                for (int i = 0, n = modIds.size(); i < n; i++) {
                    if ("ClearCanopy".equals(modIds.get(i))) {
                        active = true;
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            active = true;
        }
        if (!active && !phantomTraceLogged) {
            phantomTraceLogged = true;
            trace("ClearCanopy not in active mod set — patch yields (ZB advice persists across mod-list changes within the same JVM)");
        }
        return active;
    }

    public static boolean isCameraPlayerIndoor() {
        IsoGridSquare camSq = IsoCamera.frameState.camCharacterSquare;
        return camSq != null && camSq.isInARoom();
    }

    public static void trace(String msg) {
        System.out.println("[ClearCanopy] " + msg);
    }
}
