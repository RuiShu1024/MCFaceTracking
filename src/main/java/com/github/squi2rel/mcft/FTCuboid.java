package com.github.squi2rel.mcft;

import com.github.squi2rel.mcft.mixin.client.CubeDefinitionAccessor;
import com.github.squi2rel.mcft.mixin.client.CubeDeformationAccessor;
import com.github.squi2rel.mcft.tracking.EyeTrackingRect;
import com.github.squi2rel.mcft.tracking.MouthTrackingRect;
import com.github.squi2rel.mcft.tracking.Rect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Set;
import java.util.UUID;

@SuppressWarnings("SameParameterValue")
public class FTCuboid extends ModelPart.Cube {
    public static final Vector3fc face = Direction.NORTH.getUnitVec3f();
    public static UUID player;
    private static final Vector3f position = new Vector3f(), normal = new Vector3f(), tmp = new Vector3f();
    private static int light, overlay, color;

    private FTCuboid(
            int u, int v,
            float x, float y, float z,
            float sizeX, float sizeY, float sizeZ,
            float extraX, float extraY, float extraZ,
            boolean mirror, float textureWidth, float textureHeight,
            Set<Direction> visibleFaces
    ) {
        super(u, v, x, y, z, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, visibleFaces);
    }

    @Override
    public void compile(PoseStack.Pose entry, VertexConsumer buffer, int l, int o, int c) {
        light = l;
        overlay = o;
        color = c;
        Matrix4f posMat = entry.pose();
        for (ModelPart.Polygon quad : this.polygons) {
            entry.transformNormal(quad.normal(), normal);
            if (isFace(quad.normal())) {
                FTModel m = MCFTClient.uuidToModel.get(player);
                if (m != null && m.active()) {
                    drawFace(m, entry, buffer);
                    continue;
                }
            }
            for (ModelPart.Vertex vertex : quad.vertices()) {
                posMat.transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), position);
                buffer.addVertex(
                        position.x, position.y, position.z,
                        color, vertex.u(), vertex.v(), overlay, light,
                        normal.x, normal.y, normal.z
                );
            }
        }
    }

    private static boolean isFace(Vector3fc dir) {
        return dir.z() < -0.5f && Math.abs(dir.x()) < 0.5f && Math.abs(dir.y()) < 0.5f;
    }

    /** Smallest opening, in skin pixels, that still reads as a mouth once rasterised. */
    private static final float MOUTH_MIN = 0.4f;

    private void drawFace(FTModel model, PoseStack.Pose entry, VertexConsumer buffer) {
        model.update(MCFTClient.fps);
        Matrix4f posMat = entry.pose();
        EyeTrackingRect r = model.eyeR;
        EyeTrackingRect l = model.eyeL;
        MouthTrackingRect m = model.mouth;

        // The mouth animation opens to a fraction of a skin pixel for ordinary speech and pinches itself
        // to zero width once fully open, so there is nothing left to rasterise. Clamp what gets drawn to
        // something that survives; the tracked values themselves stay untouched.
        boolean hasMouth = !model.isFlat && sized(m) && m.h > 0f;
        float mh = 0f, mw = 0f, mx = 0f, my = 0f;
        if (hasMouth) {
            mh = Math.min(m.ih, Math.max(m.h, MOUTH_MIN));
            mw = Math.min(m.iw, Math.max(m.w, MOUTH_MIN));
            // Both clamps grow the opening away from the edge the animation holds still, so it stays in
            // the cell that was marked for it. MouthTrackingRect keeps y - h at iy - ih and slides the
            // bottom edge down, so the extra height belongs below the top edge; measuring back from the
            // bottom edge instead lifted a barely open mouth clean out of its row, up between the eyes.
            mx = Mth.clamp(m.x + (m.w - mw) / 2f, 0f, 8f - mw);
            my = Mth.clamp(m.y - m.h, 0f, 8f - mh) + mh;
        }

        // Punch one opening per feature and let tileFace() fill everything else with plain skin.
        // The old hand written tiling assumed eyeR sat left of eyeL and the mouth between them; with any
        // other layout its tiles overlapped the eyes and one of them came out with a negative width,
        // which is what tore the face apart and left the skin's own eyes showing through.
        int holes = 0;
        if (sized(r)) holes = addHole(holes, r.x, r.y - r.ih, r.w, r.ih);
        if (sized(l)) holes = addHole(holes, l.x, l.y - l.ih, l.w, l.ih);
        if (hasMouth) holes = addHole(holes, mx, my - mh, mw, mh);
        tileFace(posMat, buffer, holes);

        if (model.isFlat) {
            drawEyeFlat(entry, buffer, r, m);
            drawEyeFlat(entry, buffer, l, m);
            return;
        }
        drawEye(entry, buffer, r);
        drawEye(entry, buffer, l);
        if (hasMouth) drawMouth(entry, buffer, m, mx, my, mw, mh);
    }

    /**
     * The mouth is a shallow cavity sitting behind the opening punched in the face.
     * <p>
     * Its back wall is a whole pixel inside the head, which the rim of the opening hides as soon as the
     * camera leaves dead centre, so the cavity also gets a backing quad right behind the surface. That is
     * what {@link #drawEye} already does for the eye socket, and why the eyes kept showing while the
     * mouth was covered up.
     */
    private void drawMouth(PoseStack.Pose entry, VertexConsumer buffer, MouthTrackingRect m, float x, float y, float w, float h) {
        drawCube(entry, buffer, x - 4, y - h - 8, -3, m.u1, m.v1, x + w - 4, y - 8, -4, m.u2, m.v2, true, true, false);
        drawQuad(entry.pose(), buffer, x - 4, y - 8, m.u1, m.v2, x + w - 4, y - h - 8, m.u2, m.v1, -3.9f);
    }

    private static boolean sized(Rect rect) {
        return rect.iw > 0.05f && rect.ih > 0.05f;
    }

    /** Up to three openings, stored flat as x1, y1, x2, y2. */
    private final float[] holeBounds = new float[12];
    private final float[] bandEdges = new float[8];

    private int addHole(int count, float x, float y, float w, float h) {
        int i = count * 4;
        holeBounds[i] = Mth.clamp(x, 0f, 8f);
        holeBounds[i + 1] = Mth.clamp(y, 0f, 8f);
        holeBounds[i + 2] = Mth.clamp(x + w, 0f, 8f);
        holeBounds[i + 3] = Mth.clamp(y + h, 0f, 8f);
        return count + 1;
    }

    /**
     * Fills the face with plain skin, leaving the registered openings empty.
     * <p>
     * The face is cut into horizontal bands at every opening edge; inside a band each opening is a
     * simple horizontal gap. That holds for any arrangement of regions, so the tiles always line up
     * with the holes no matter where the eyes and mouth were marked.
     */
    private void tileFace(Matrix4f posMat, VertexConsumer buffer, int count) {
        int edges = 0;
        bandEdges[edges++] = 0f;
        bandEdges[edges++] = 8f;
        for (int i = 0; i < count; i++) {
            bandEdges[edges++] = holeBounds[i * 4 + 1];
            bandEdges[edges++] = holeBounds[i * 4 + 3];
        }
        java.util.Arrays.sort(bandEdges, 0, edges);
        for (int i = 0; i + 1 < edges; i++) {
            float top = bandEdges[i];
            float bottom = bandEdges[i + 1];
            if (bottom - top < 0.001f) continue;
            float cursor = 0f;
            while (cursor < 8f) {
                int next = -1;
                float nextX = Float.MAX_VALUE;
                for (int h = 0; h < count; h++) {
                    if (holeBounds[h * 4 + 1] > top + 0.001f || holeBounds[h * 4 + 3] < bottom - 0.001f) continue;
                    if (holeBounds[h * 4 + 2] <= cursor + 0.001f) continue;
                    if (holeBounds[h * 4] < nextX) {
                        nextX = holeBounds[h * 4];
                        next = h;
                    }
                }
                if (next < 0) {
                    drawSkin(posMat, buffer, cursor, top, 8f - cursor, bottom - top);
                    break;
                }
                float gapStart = Math.max(holeBounds[next * 4], cursor);
                if (gapStart > cursor) drawSkin(posMat, buffer, cursor, top, gapStart - cursor, bottom - top);
                cursor = Math.max(cursor, holeBounds[next * 4 + 2]);
            }
        }
    }

    private void drawEye(PoseStack.Pose entry, VertexConsumer buffer, EyeTrackingRect e) {
        Matrix4f posMat = entry.pose();
        drawCube(entry, buffer, e.x - 4, e.y - e.ih - 8, -2, e.inner.u1, e.inner.v2, e.x + e.w - 4, e.y - 8, -4f, e.inner.u2, e.inner.v1, true, true, true);
        drawQuad(posMat, buffer, e.x - 4, e.y - 8, e.inner.u1, e.inner.v2, e.x + e.w - 4, e.y - e.ih - 8, e.inner.u2, e.inner.v1, -3.9f);
        drawCube(entry, buffer, e.x + (e.w - e.ball.w) / 2 + e.ball.x - 4, e.y - (e.ih + e.ball.h) / 2 + e.ball.y - 8, -3, e.ball.u1, e.ball.v1, e.x + (e.w + e.ball.w) / 2 + e.ball.x - 4, e.y - (e.ih - e.ball.h) / 2 + e.ball.y - 8, -3.95f, e.ball.u2, e.ball.v2, false, false, true);
        drawQuad(posMat, buffer, e.x - 4, e.y - e.ih - 8, e.lid.u1, e.lid.v2, e.x + e.w - 4, e.y - e.h - 8, e.lid.u2, e.lid.v1);
        drawCube(entry, buffer, e.x - 4, e.y - e.h - 0.1f - 8, -4, e.lid.u1, e.lid.v2 - 0.0001f, e.x + e.w - 4, e.y - e.h - 8, -4.1f, e.lid.u2, e.lid.v2, false, false, true);
    }

    private void drawEyeFlat(PoseStack.Pose entry, VertexConsumer buffer, EyeTrackingRect e, MouthTrackingRect brow) {
        Matrix4f posMat = entry.pose();
        drawQuad(posMat, buffer, clamp4(e.x - 4 - 0.05f), clamp8(e.y - 8 + 0.05f), e.inner.u1, e.inner.v2, clamp4(e.x + e.w - 4 + 0.05f), clamp8(e.y - e.ih - 8 - 0.05f), e.inner.u2, e.inner.v1, -3.99f);
        drawQuad(posMat, buffer, e.x + (e.w - e.ball.w) / 2 + e.ball.x - 4, e.y - (e.ih - e.ball.h) / 2 + e.ball.y - 8, e.ball.u1, e.ball.v2, e.x + (e.w + e.ball.w) / 2 + e.ball.x - 4, e.y - (e.ih + e.ball.h) / 2 + e.ball.y - 8, e.ball.u2, e.ball.v1, -3.995f);
        drawQuad(posMat, buffer, e.x - 4, e.y - e.h - 8, e.lid.u1, Mth.lerp(1 - e.h / Math.max(e.ih, 0.0001f), e.lid.v1, e.lid.v2), e.x + e.w - 4, e.y - e.ih - 8, e.lid.u2, e.lid.v1, -4f);
        drawQuad(posMat, buffer, e.x - 4, e.y - e.h - brow.h - 1 - 8, brow.u1, brow.v2, e.x + e.w - 4, e.y - e.h - brow.h - 2 - 8, brow.u2, brow.v1, -4.005f);
    }

    private void drawCube(PoseStack.Pose entry, VertexConsumer buffer, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, boolean inner, boolean skipFront, boolean skipBack) {
        Matrix4f posMat = entry.pose();
        if (!skipFront) drawQuad(posMat, buffer, x1, y1, z2, u1, v1, x1, y2, z2, u1, v2, x2, y2, z2, u2, v2, x2, y1, z2, u2, v1, entry.transformNormal((inner ? Direction.SOUTH : Direction.NORTH).getUnitVec3f(), tmp));
        if (!skipBack) drawQuad(posMat, buffer, x1, y1, z1, u1, v1, x1, y2, z1, u1, v2, x2, y2, z1, u2, v2, x2, y1, z1, u2, v1, entry.transformNormal((inner ? Direction.NORTH : Direction.SOUTH).getUnitVec3f(), tmp));
        drawQuad(posMat, buffer, x1, y1, z1, u1, v1, x1, y1, z2, u2, v1, x1, y2, z2, u2, v2, x1, y2, z1, u1, v2, entry.transformNormal((inner ? Direction.EAST : Direction.WEST).getUnitVec3f(), tmp));
        drawQuad(posMat, buffer, x2, y1, z1, u1, v1, x2, y1, z2, u2, v1, x2, y2, z2, u2, v2, x2, y2, z1, u1, v2, entry.transformNormal((inner ? Direction.WEST : Direction.EAST).getUnitVec3f(), tmp));
        drawQuad(posMat, buffer, x1, y2, z1, u1, v1, x1, y2, z2, u1, v2, x2, y2, z2, u2, v2, x2, y2, z1, u2, v1, entry.transformNormal((inner ? Direction.DOWN : Direction.UP).getUnitVec3f(), tmp));
        drawQuad(posMat, buffer, x1, y1, z1, u1, v1, x2, y1, z1, u2, v1, x2, y1, z2, u2, v2, x1, y1, z2, u1, v2, entry.transformNormal((inner ? Direction.UP : Direction.DOWN).getUnitVec3f(), tmp));
    }

    /** Draws one tile of untouched head skin; UVs come straight from the head-front area of the skin. */
    private void drawSkin(Matrix4f posMat, VertexConsumer buffer, float x, float y, float w, float h) {
        if (w <= 0.001f || h <= 0.001f) return;
        drawQuad(posMat, buffer, x - 4, y - 8, 0.125f + x / 64, 0.125f + y / 64, x + w - 4, y + h - 8, 0.125f + (x + w) / 64, 0.125f + (y + h) / 64);
    }

    private void drawQuad(Matrix4f posMat, VertexConsumer buffer, float x1, float y1, float u1, float v1, float x2, float y2, float u2, float v2) {
        drawQuad(posMat, buffer, x1, y1, u1, v1, x1, y2, u1, v2, x2, y2, u2, v2, x2, y1, u2, v1);
    }

    private void drawQuad(Matrix4f posMat, VertexConsumer buffer, float x1, float y1, float u1, float v1, float x2, float y2, float u2, float v2, float z) {
        drawQuad(posMat, buffer, x1, y1, z, u1, v1, x2, y1, z, u2, v1, x2, y2, z, u2, v2, x1, y2, z, u1, v2, normal);
    }

    private void drawQuad(
            Matrix4f posMat, VertexConsumer buffer,
            float x1, float y1, float u1, float v1, float x2, float y2, float u2, float v2,
            float x3, float y3, float u3, float v3, float x4, float y4, float u4, float v4
    ) {
        drawQuad(posMat, buffer, x1, y1, -4, u1, v1, x2, y2, -4, u2, v2, x3, y3, -4, u3, v3, x4, y4, -4, u4, v4, normal);
    }

    private void drawQuad(
            Matrix4f posMat, VertexConsumer buffer,
            float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3, float x4, float y4, float z4, float u4, float v4,
            Vector3f normal
    ) {
        float nx = normal.x;
        float ny = normal.y;
        float nz = normal.z;
        posMat.transformPosition(x1 / 16.0F, y1 / 16.0F, z1 / 16.0F, position);
        buffer.addVertex(position.x, position.y, position.z, color, u1, v1, overlay, light, nx, ny, nz);
        posMat.transformPosition(x2 / 16.0F, y2 / 16.0F, z2 / 16.0F, position);
        buffer.addVertex(position.x, position.y, position.z, color, u2, v2, overlay, light, nx, ny, nz);
        posMat.transformPosition(x3 / 16.0F, y3 / 16.0F, z3 / 16.0F, position);
        buffer.addVertex(position.x, position.y, position.z, color, u3, v3, overlay, light, nx, ny, nz);
        posMat.transformPosition(x4 / 16.0F, y4 / 16.0F, z4 / 16.0F, position);
        buffer.addVertex(position.x, position.y, position.z, color, u4, v4, overlay, light, nx, ny, nz);
    }

    public static float clamp4(float v) {
        return Mth.clamp(v, -4, 4);
    }

    public static float clamp8(float v) {
        return Mth.clamp(v, -8, 0);
    }

    /**
     * Rebuilds the given cube definition as an {@link FTCuboid}. 26.1 exposes {@code CubeDefinition.bake},
     * so the replacement cube can be created through the real constructor instead of copying the fields of an
     * already baked cube via reflection.
     */
    public static FTCuboid newInstance(CubeDefinition definition, int textureWidth, int textureHeight) {
        CubeDefinitionAccessor def = (CubeDefinitionAccessor) (Object) definition;
        CubeDeformationAccessor grow = (CubeDeformationAccessor) (Object) def.getGrow();
        Vector3fc origin = def.getOrigin();
        Vector3fc dimensions = def.getDimensions();
        UVPair texCoord = def.getTexCoord();
        UVPair texScale = def.getTexScale();
        return new FTCuboid(
                (int) texCoord.u(), (int) texCoord.v(),
                origin.x(), origin.y(), origin.z(),
                dimensions.x(), dimensions.y(), dimensions.z(),
                grow.getGrowX(), grow.getGrowY(), grow.getGrowZ(),
                def.isMirror(),
                textureWidth * texScale.u(), textureHeight * texScale.v(),
                def.getVisibleFaces()
        );
    }
}
