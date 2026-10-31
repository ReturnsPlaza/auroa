package dev.aurora.hud;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.aurora.AuroraClient;
import dev.aurora.module.impl.AmethystChunkFinder;
import dev.aurora.module.impl.BlockEsp;
import dev.aurora.module.impl.EspModule;
import dev.aurora.module.impl.StorageEsp;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MappableRingBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** GPU-batched 3D ESP renderer based on Donut Client's smooth outline renderer. */
public final class EspRenderer {
    private static final RenderPipeline FILL = pipeline("fill", false);
    private static final RenderPipeline LINES = pipeline("lines", true);
    private static final Vector4f WHITE = new Vector4f(1, 1, 1, 1);
    private static final Vector3f ZERO = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final int[][] FACES = {
            {0, 4, 6, 2}, {5, 1, 3, 7}, {4, 0, 1, 5},
            {2, 6, 7, 3}, {1, 0, 2, 3}, {4, 5, 7, 6}
    };
    private static final Batch fillBatch = new Batch(FILL);
    private static final Batch lineBatch = new Batch(LINES);
    private static boolean registered;

    private EspRenderer() { }

    private static RenderPipeline pipeline(String name, boolean lines) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.of(AuroraClient.ID, "pipeline/esp_" + name))
                .withVertexShader(lines ? "core/rendertype_lines" : "core/position_color")
                .withFragmentShader("core/position_color")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(lines ? DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH
                        : DefaultVertexFormat.POSITION_COLOR,
                        lines ? VertexFormat.DrawMode.LINES : VertexFormat.DrawMode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(!lines)
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        if (lines) {
            builder.withUniform("Globals", UniformType.UNIFORM_BUFFER)
                    .withUniform("Fog", UniformType.UNIFORM_BUFFER);
        }
        return RenderPipelines.register(builder.build());
    }

    public static void register() {
        if (registered) return;
        registered = true;
        WorldRenderEvents.END_MAIN.register(EspRenderer::renderWorld);
    }

    private static void renderWorld(WorldRenderContext context) {
        if (AuroraClient.modules == null) return;
        StorageEsp storage = AuroraClient.modules.get(StorageEsp.class);
        BlockEsp blocks = AuroraClient.modules.get(BlockEsp.class);
        AmethystChunkFinder amethyst = AuroraClient.modules.get(AmethystChunkFinder.class);
        if (!storage.isEnabled() && !blocks.isEnabled() && !amethyst.isEnabled()) return;

        Vec3d camera = context.worldState().cameraRenderState.pos;
        MatrixStack.Entry pose = context.matrices().peek();
        if (blocks.isEnabled()) drawModule(context, pose, camera, blocks);
        if (storage.isEnabled()) drawModule(context, pose, camera, storage);
        if (amethyst.isEnabled()) drawModule(context, pose, camera, amethyst);
        fillBatch.flush();
        lineBatch.flush();
    }

    private static void drawModule(WorldRenderContext context, MatrixStack.Entry pose,
                                  Vec3d camera, EspModule module) {
        var targets = module.targets();
        double maxSq = module.maxDistance() * module.maxDistance();
        int drawn = 0;
        for (EspTarget target : targets) {
            if (drawn >= 36) break;
            BlockPos pos = target.pos();
            Vec3d center = Vec3d.ofCenter(pos);
            if (center.squaredDistanceTo(camera) > maxSq) continue;
            Box worldBox = new Box(pos).expand(0.002);
            if (!context.frustum().isVisible(worldBox)) continue;
            Box box = worldBox.offset(-camera.x, -camera.y, -camera.z);

            int rgb = target.color() & 0xFFFFFF;
            if (module.fill() && module.fillAlpha() > 0) {
                filled(fillBatch.begin(), pose.getPositionMatrix(), box, (module.fillAlpha() << 24) | rgb);
            }
            if (module.outline()) {
                outlined(lineBatch.begin(), pose, box, 0xD0000000 | rgb, module.thickness());
            }
            drawn++;
        }
    }

    private static void filled(BufferBuilder buffer, Matrix4fc pose, Box b, int color) {
        for (int[] face : FACES) {
            for (int corner : face) {
                buffer.addVertex(pose, x(b, corner), y(b, corner), z(b, corner)).setColor(color);
            }
        }
    }

    private static void outlined(BufferBuilder buffer, MatrixStack.Entry pose,
                                 Box box, int color, float width) {
        for (int corner = 0; corner < 8; corner++) {
            for (int axis = 1; axis <= 4; axis *= 2) {
                if ((corner & axis) != 0) continue;
                int other = corner | axis;
                float nx = axis == 1 ? 1 : 0;
                float ny = axis == 2 ? 1 : 0;
                float nz = axis == 4 ? 1 : 0;
                buffer.addVertex(pose, x(box, corner), y(box, corner), z(box, corner))
                        .setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
                buffer.addVertex(pose, x(box, other), y(box, other), z(box, other))
                        .setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
            }
        }
    }

    private static float x(Box b, int c) { return (float) ((c & 1) == 0 ? b.minX : b.maxX); }
    private static float y(Box b, int c) { return (float) ((c & 2) == 0 ? b.minY : b.maxY); }
    private static float z(Box b, int c) { return (float) ((c & 4) == 0 ? b.minZ : b.maxZ); }

    /** GUI line helper retained for rain streaks and the HUD's entity tracers. */
    public static void line(DrawContext ctx, double x1, double y1, double x2, double y2, int color, int thick) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (Math.abs(dx) >= Math.abs(dy)) {
            int ya = (int) Math.floor(Math.min(y1, y2));
            int yb = (int) Math.floor(Math.max(y1, y2));
            for (int y = ya; y <= yb; y++) {
                double t0 = dy == 0 ? 0 : (y - y1) / dy;
                double t1 = dy == 0 ? 1 : (y + 1 - y1) / dy;
                t0 = Math.max(0, Math.min(1, t0)); t1 = Math.max(0, Math.min(1, t1));
                int xs = (int) Math.floor(Math.min(x1 + dx * t0, x1 + dx * t1));
                int xe = (int) Math.ceil(Math.max(x1 + dx * t0, x1 + dx * t1));
                ctx.fill(xs, y, Math.max(xs + 1, xe), y + thick, color);
            }
        } else {
            int xa = (int) Math.floor(Math.min(x1, x2));
            int xb = (int) Math.floor(Math.max(x1, x2));
            for (int x = xa; x <= xb; x++) {
                double t0 = dx == 0 ? 0 : (x - x1) / dx;
                double t1 = dx == 0 ? 1 : (x + 1 - x1) / dx;
                t0 = Math.max(0, Math.min(1, t0)); t1 = Math.max(0, Math.min(1, t1));
                int ys = (int) Math.floor(Math.min(y1 + dy * t0, y1 + dy * t1));
                int ye = (int) Math.ceil(Math.max(y1 + dy * t0, y1 + dy * t1));
                ctx.fill(x, ys, x + thick, Math.max(ys + 1, ye), color);
            }
        }
    }

    /** Reusable CPU allocator and rotating GPU vertex buffer, as in Donut Client's renderer. */
    private static final class Batch {
        private final RenderPipeline pipeline;
        private final ByteBufferBuilder allocator = new ByteBufferBuilder(64 * 1024);
        private BufferBuilder builder;
        private MappableRingBuffer vertices;

        private Batch(RenderPipeline pipeline) { this.pipeline = pipeline; }

        private BufferBuilder begin() {
            if (builder == null) builder = new BufferBuilder(allocator,
                    pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
            return builder;
        }

        private void flush() {
            if (builder == null) return;
            MeshData mesh = builder.buildOrThrow();
            builder = null;
            try (mesh) {
                int bytes = mesh.vertexBuffer().remaining();
                if (vertices == null || vertices.size() < bytes) {
                    if (vertices != null) vertices.close();
                    vertices = new MappableRingBuffer(() -> "67 Client ESP", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, bytes);
                }
                try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder()
                        .mapBuffer(vertices.currentBuffer().slice(0, bytes), false, true)) {
                    MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
                }

                GpuBuffer indices;
                VertexFormat.IndexType indexType;
                if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
                    mesh.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
                    indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(mesh.indexBuffer());
                    indexType = mesh.drawState().indexType();
                } else {
                    RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
                    indices = sequential.getBuffer(mesh.drawState().indexCount());
                    indexType = sequential.type();
                }

                GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
                        .writeTransform(RenderSystem.getModelViewMatrix(), WHITE, ZERO, IDENTITY);
                var framebuffer = MinecraftClient.getInstance().getFramebuffer();
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "67 Client ESP", framebuffer.getColorAttachmentView(), OptionalInt.empty(),
                        framebuffer.getDepthAttachmentView(), OptionalDouble.empty())) {
                    pass.setPipeline(pipeline);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transform);
                    pass.setVertexBuffer(0, vertices.currentBuffer());
                    pass.setIndexBuffer(indices, indexType);
                    pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
                }
                vertices.rotate();
            }
        }
    }
}

