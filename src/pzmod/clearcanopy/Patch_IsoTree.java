package pzmod.clearcanopy;

import me.zed_0xff.zombie_buddy.Patch;

import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.objects.IsoTree;

// Indoor jumbo-tree hiding. The 42.20 jumbo trees (JUMBO / JUMBOXL /
// JUMBOXXL sprites) are tall enough to cover a building's interior
// from the outside — standing behind a crown, the room becomes hard
// to read and the stencil outline pass draws patchy outlines over
// the interior. While the camera player is indoors, skip rendering
// the jumbos whose sprite covers the player's screen position, so
// only the crowns actually intruding on the view disappear and the
// rest of the screen keeps its trees.
//
// Deliberately NOT cell.isInStencil: that tests the StencilArea
// bounding boxes, and the player mask's bbox (~2048px at tileScale
// 2) is far larger than its visible gradient — probing against it
// hid jumbos across most of the screen. The sprite's screen bbox is
// derived from the FBORenderChunk jumbo dimensions (crown extends
// H - FLOOR_HEIGHT above the base square) and compared against the
// camera character square's screen point plus a one-tile margin.
//
// ZB weaves by method name, so this also lands on the private
// render overload; the public wrapper is skipped first, making the
// inner weave a no-op.
public class Patch_IsoTree {

    @Patch(className = "zombie.iso.objects.IsoTree", methodName = "render")
    public static class Patch_render {

        @Patch.OnEnter(skipOn = true)
        public static boolean enter(@Patch.This IsoTree self) {
            try {
                if (!ClearCanopyMod.isActiveForCurrentFrame()) return false;
                if (!ClearCanopyMod.isCameraPlayerIndoor()) return false;
                if (self.getSprite() == null) return false;
                String name = self.getSprite().getName();
                if (name == null || !name.contains("JUMBO")) return false;
                IsoGridSquare sq = self.square;
                if (sq == null) return false;

                int w;
                int h;
                if (name.contains("JUMBOXXL")) {
                    w = FBORenderChunk.JUMBO_XXL_WIDTH;
                    h = FBORenderChunk.JUMBO_XXL_HEIGHT;
                } else if (name.contains("JUMBOXL")) {
                    w = FBORenderChunk.JUMBO_XL_WIDTH;
                    h = FBORenderChunk.JUMBO_XL_HEIGHT;
                } else {
                    w = FBORenderChunk.JUMBO_L_WIDTH;
                    h = FBORenderChunk.JUMBO_L_HEIGHT;
                }

                sq.IsOnScreen();
                float sx = sq.cachedScreenX - IsoCamera.frameState.offX;
                float sy = sq.cachedScreenY - IsoCamera.frameState.offY;

                float x0 = sx - (float) w / 2f;
                float x1 = sx + (float) w / 2f;
                float y1 = sy + (float) FBORenderChunk.FLOOR_HEIGHT;
                float y0 = y1 - (float) h;

                // isCameraPlayerIndoor already guaranteed non-null.
                IsoGridSquare camSq = IsoCamera.frameState.camCharacterSquare;
                camSq.IsOnScreen();
                float px = camSq.cachedScreenX - IsoCamera.frameState.offX;
                float py = camSq.cachedScreenY - IsoCamera.frameState.offY;

                float m = (float) FBORenderChunk.FLOOR_WIDTH;
                return px > x0 - m && px < x1 + m
                        && py > y0 - m && py < y1 + m;
            } catch (Throwable t) {
                return false;
            }
        }
    }
}
