package de.skyrising.casual;

import com.mojang.blaze3d.platform.GlStateManager;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.IntBoundingBox;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;

public class MarkerRenderer implements IRenderer {
    private final BufferRenderer renderer = new BufferRenderer();
    private final BufferBuilder bufferLines = new BufferBuilder(2097152);
    private final BufferBuilder bufferQuads = new BufferBuilder(2097152);

    @Override
    public void onRenderWorldLast(float partialTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        float viewDistance = client.gameRenderer.getViewDistance();
        Camera cam = client.gameRenderer.getCamera();
        BlockPos camPos = cam.getBlockPos();
        WorldDataTracker data = WorldDataTracker.getInstance(world);
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.lineWidth(2.0F);
        GlStateManager.disableTexture();
        GlStateManager.depthMask(false);
        GlStateManager.enablePolygonOffset();
        GlStateManager.polygonOffset(-3f, -3f);
        GlStateManager.disableCull();
        Vec3d camPosExact = cam.getPos();
        GlStateManager.translated(-camPosExact.x, -camPosExact.y, -camPosExact.z);
        BufferBuilder bufferLines = this.bufferLines;
        bufferLines.begin(GL_LINES, VertexFormats.POSITION_COLOR);
        BufferBuilder bufferQuads = this.bufferQuads;
        bufferQuads.begin(GL_QUADS, VertexFormats.POSITION_COLOR);
        Set<IntBoundingBox> renderedBoxes = new HashSet<>();
        for (Map.Entry<BlockPos, WorldDataTracker.Marker> markerEntry : data.markers.entrySet()) {
            BlockPos markerPos = markerEntry.getKey();
            int distX = Math.abs(markerPos.getX() - camPos.getX());
            int distZ = Math.abs(markerPos.getZ() - camPos.getZ());
            //System.out.printf("%s %d,%d\n", markerPos, distX, distZ);
            if (distX > viewDistance || distZ > viewDistance) continue;
            Color4f color = new Color4f(1, 0, 0, 1);
            WorldDataTracker.Marker marker = markerEntry.getValue();
            switch (marker.type) {
                case STRUCTURE: color = new Color4f(0, 1, 0); break;
                case CHEST: color = new Color4f(0, 1, 1); break;
                case SPAWNER: color = new Color4f(1, 0, 1); break;
                case MINESHAFT: color = new Color4f(1, 1, 0); break;
            }
            Color4f color2 = new Color4f(color.r, color.g, color.b, 0.2f);
            BlockPos prev = null;
            for (BlockPos pos : marker.blocks) {
                if (prev != null) {
                    int xDiff = pos.getX() - prev.getX();
                    int yDiff = pos.getY() - prev.getY();
                    int zDiff = pos.getZ() - prev.getY();
                    bufferLines.vertex(
                        prev.getX() + 0.5 * Math.signum(xDiff) + 0.5,
                        prev.getY() + 0.5 * Math.signum(yDiff) + 0.5,
                        prev.getZ() + 0.5 * Math.signum(zDiff) + 0.5
                    ).color(color.r, color.g, color.b, 0.5f).next();
                    bufferLines.vertex(
                        pos.getX() - 0.5 * Math.signum(xDiff) + 0.5,
                        pos.getY() - 0.5 * Math.signum(yDiff) + 0.5,
                        pos.getZ() - 0.5 * Math.signum(zDiff) + 0.5
                    ).color(color.r, color.g, color.b, 0.5f).next();
                }
                RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, color, 0, bufferLines);
                prev = pos;
            }
            GlStateManager.disableDepthTest();
            bufferLines.end();
            renderer.draw(bufferLines);
            GlStateManager.enableDepthTest();
            bufferLines.begin(GL_LINES, VertexFormats.POSITION_COLOR);
            for (ChunkPos chunk : marker.chunks) {
                RenderUtils.drawBoxAllEdgesBatchedLines(
                        chunk.x << 4, 0, chunk.z << 4,
                        (chunk.x << 4) + 16, 256, (chunk.z << 4) + 16,
                        color, bufferLines);
            }
            for (IntBoundingBox box : marker.boxes) {
                if (renderedBoxes.contains(box)) continue;
                RenderUtils.drawBox(box, color2, bufferQuads, bufferLines);
                renderedBoxes.add(box);
            }
        }
        bufferQuads.end();
        renderer.draw(bufferQuads);
        bufferLines.end();
        renderer.draw(bufferLines);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture();
        GlStateManager.enableCull();
        GlStateManager.disablePolygonOffset();
    }
}
