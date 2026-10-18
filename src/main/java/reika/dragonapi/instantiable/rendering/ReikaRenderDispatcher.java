package reika.dragonapi.instantiable.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import reika.dragonapi.client.CapturedBlockGeometry;
import reika.dragonapi.interfaces.IBlockRenderer;

/** Additive native section geometry; resource/world reads occur at main-thread capture, never in meshing callbacks. */
public final class ReikaRenderDispatcher {
    private record Registry(Map<Block, IBlockRenderer> blocks, List<IBlockRenderer> general) {}
    private static volatile Registry registry = new Registry(Map.of(), List.of());
    private static boolean initialized;
    private ReikaRenderDispatcher() {}

    public static synchronized void init() {
        if (initialized) return;
        NeoForge.EVENT_BUS.addListener(ReikaRenderDispatcher::addSectionGeometry);
        initialized = true;
    }

    /** Original first-registration-wins behavior; a declining direct renderer falls back to the ordered general list. */
    public static synchronized void registerBlockRenderer(Block block, IBlockRenderer renderer) {
        Objects.requireNonNull(block, "block"); Objects.requireNonNull(renderer, "renderer");
        var current = registry;
        if (current.blocks.containsKey(block)) return;
        var blocks = new IdentityHashMap<>(current.blocks); blocks.put(block, renderer);
        registry = new Registry(Collections.unmodifiableMap(blocks), current.general);
    }

    public static synchronized void registerRenderer(IBlockRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        var current = registry; var renderers = new ArrayList<>(current.general); renderers.add(renderer);
        registry = new Registry(current.blocks, List.copyOf(renderers));
    }

    /** Release only this owner's registration; another renderer's entry is never removed. */
    public static synchronized boolean unregisterBlockRenderer(Block block, IBlockRenderer renderer) {
        var current = registry;
        if (!current.blocks.containsKey(block) || current.blocks.get(block) != renderer) return false;
        var blocks = new IdentityHashMap<>(current.blocks); blocks.remove(block);
        registry = new Registry(Collections.unmodifiableMap(blocks), current.general); return true;
    }

    public static synchronized boolean unregisterRenderer(IBlockRenderer renderer) {
        var current = registry; var renderers = new ArrayList<>(current.general);
        for (int i = 0; i < renderers.size(); i++) if (renderers.get(i) == renderer) {
            renderers.remove(i); registry = new Registry(current.blocks, List.copyOf(renderers)); return true;
        }
        return false;
    }

    static IBlockRenderer findFor(Block block, Predicate<IBlockRenderer> predicate) { return findFor(registry, block, predicate); }
    private static IBlockRenderer findFor(Registry current, Block block, Predicate<IBlockRenderer> predicate) {
        var direct = current.blocks.get(block);
        if (direct != null && predicate.test(direct)) return direct;
        for (var renderer : current.general) if (predicate.test(renderer)) return renderer;
        return null;
    }

    private static void addSectionGeometry(AddSectionGeometryEvent event) {
        var current = registry;
        if (current.blocks.isEmpty() && current.general.isEmpty()) return;
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("Section geometry must be captured on client main");
        var level = event.getLevel(); var origin = event.getSectionOrigin().immutable();
        if (!(level instanceof net.minecraft.client.renderer.block.BlockAndTintGetter view))
            throw new IllegalStateException("Custom terrain capture requires a client block/tint view");
        if (level.isOutsideBuildHeight(origin) || !level.hasChunk(origin.getX() >> 4, origin.getZ() >> 4)) return;
        var section = level.getChunkAt(origin).getSection(level.getSectionIndex(origin.getY()));
        if (section.hasOnlyAir() || (current.general.isEmpty() && !section.maybeHas(s -> current.blocks.containsKey(s.getBlock())))) return;
        var captured = new EnumMap<ChunkSectionLayer, List<CapturedBlockGeometry.Vertex>>(ChunkSectionLayer.class);
        for (BlockPos cursor : BlockPos.betweenClosed(origin, origin.offset(15, 15, 15))) {
            var state = level.getBlockState(cursor);
            if (state.isAir()) continue;
            var pos = cursor.immutable();
            for (var layer : ChunkSectionLayer.values()) {
                try {
                    var renderer = findFor(current, state.getBlock(), r -> r.shouldRender(state, view, pos, layer));
                    if (renderer == null) continue;
                    var pose = new PoseStack(); pose.translate(pos.getX()-origin.getX(), pos.getY()-origin.getY(), pos.getZ()-origin.getZ());
                    var vertices = new CapturedBlockGeometry();
                    renderer.renderBlock(state, pos, view, pose, vertices);
                    var mesh = vertices.finish();
                    if (!mesh.isEmpty()) captured.computeIfAbsent(layer, ignored -> new ArrayList<>()).addAll(mesh);
                } catch (Throwable error) {
                    var report = CrashReport.forThrowable(error, "Capturing DragonAPI terrain geometry");
                    var category = report.addCategory("Block being tessellated");
                    CrashReportCategory.populateBlockDetails(category, level, pos, state);
                    category.setDetail("Terrain layer", layer.toString());
                    throw new ReportedException(report);
                }
            }
        }
        if (captured.isEmpty()) return;
        var meshes = new EnumMap<ChunkSectionLayer, List<CapturedBlockGeometry.Vertex>>(ChunkSectionLayer.class);
        captured.forEach((layer, vertices) -> meshes.put(layer, List.copyOf(vertices)));
        var snapshot = Collections.unmodifiableMap(meshes);
        event.addRenderer(context -> snapshot.forEach((layer, vertices) -> {
            var buffer = context.getOrCreateChunkBuffer(layer);
            for (var vertex : vertices) vertex.emit(buffer);
        }));
    }
}
