//package reika.dragonapi.instantiable.rendering;
//
//import com.mojang.blaze3d.shaders.BlendMode;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.entity.ItemRenderer;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.BlockGetter;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.Blocks;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraftforge.api.distmarker.Dist;
//import reika.dragonapi.DragonAPI;
//import reika.dragonapi.instantiable.data.blockstruct.FilledBlockArray;
//import reika.dragonapi.instantiable.data.immutable.BlockKey;
//import reika.dragonapi.instantiable.data.maps.ItemHashMap;
//
//import java.util.HashMap;
//import java.util.HashSet;
//
//public class StructureRenderer {
//
//    private static final ItemRenderer itemRender = Minecraft.getInstance().getItemRenderer();
//
//    private double rx;
//    private double ry;
//    private double rz;
//
//    private int secY;
//
//    protected final FilledBlockArray array;
//    protected final RenderAccess access;
//    protected final RenderBlocks renderer;
//
//    private final HashMap<BlockPos, BlockChoiceHook> overrides = new HashMap();
//    private final ItemHashMap<ItemStack> itemOverrides = new ItemHashMap();
//    private final HashMap<Block, BlockChoiceHook> choiceHooks = new HashMap();
//    private final ItemHashMap<BlockRenderHook> renderHooks = new ItemHashMap();
//    private final HashMap<BlockPos, EntityRender> entities = new HashMap();
//
//    private static RenderAccess staticRenderAccess = null;
//    private static boolean tileRendering = false;
//    private static boolean tileRenderingReal = false;
//
//    private static double renderRotationX = 0;
//    private static double renderRotationY = 0;
//    private static double renderRotationZ = 0;
//
//    public static boolean isRenderingTiles() {
//        return tileRendering;
//    }
//
//    public static boolean isRenderingRealTiles() {
//        return tileRenderingReal;
//    }
//
//    public static double getRenderRX() {
//        return renderRotationX;
//    }
//
//    public static double getRenderRY() {
//        return renderRotationY;
//    }
//
//    public static double getRenderRZ() {
//        return renderRotationZ;
//    }
//
//    public StructureRenderer(FilledBlockArray structure) {
//        this(structure, null);
//    }
//
//    public StructureRenderer(FilledBlockArray structure, HashSet<BlockPos> alpha) {
//        array = structure;
//        access = new RenderAccess(array, alpha);
//        renderer = new RenderBlocks(access);
//        this.reset();
//    }
//
//    public void resetRotation() {
//        rx = -30;
//        ry = 45;
//        rz = 0;//180;
//    }
//
//    public void rotate(double x, double y, double z) {
//        rx += x;
//        ry += y;
//        rz += z;
//
//        renderRotationX = rx;
//        renderRotationY = ry;
//        renderRotationZ = rz;
//    }
//
//    public void reset() {
//        this.resetRotation();
//        this.resetStepY();
//    }
//
//    public void resetStepY() {
//        secY = 0;
//    }
//
//    public void incrementStepY() {
//        if (secY < array.getSizeY()-1) {
//            secY++;
//        }
//    }
//
//    public void decrementStepY() {
//        if (secY > 0) {
//            secY--;
//        }
//    }
//
//    public void addOverride(int x, int y, int z, ItemStack is) {
//        overrides.put(new BlockPos(x, y, z), new SingleBlockChoice(is));
//    }
//
//    public void addOverride(int x, int y, int z, BlockChoiceHook bc) {
//        overrides.put(new BlockPos(x, y, z), bc);
//    }
//
//    public void addOverride(ItemStack is, ItemStack render) {
//        itemOverrides.put(is, render);
//    }
//
//    public void addBlockHook(Block b, BlockChoiceHook brh) {
//        choiceHooks.put(b, brh);
//    }
//
//    public void addRenderHook(ItemStack is, BlockRenderHook brh) {
//        renderHooks.put(is, brh);
//    }
//
//    public void addEntityRender(int x, int y, int z, EntityRender e) {
//        entities.put(new BlockPos(x-array.getMidX(), y-array.getMidY(), z-array.getMidZ()), e);
//    }
//
//    private ItemStack getRenderStack(BlockPos pos) {
//        ItemStack is = array.getDisplayAt(pos.xCoord, pos.yCoord, pos.zCoord);
//        BlockChoiceHook call = overrides.get(pos);
//        if (call != null) {
//            is = call.getBlock(pos);
//        }
//        if (is != null && is.getItem() != null) {
//            ItemStack over = itemOverrides.get(is);
//            if (over != null)
//                is = over;
//        }
//        if (is != null && is.getItem() != null) {
//            Block b = Block.getBlockFromItem(is.getItem());
//            if (b != null) {
//                BlockChoiceHook bc = choiceHooks.get(b);
//                if (bc != null) {
//                    is = bc.getBlock(pos);
//                }
//            }
//        }
//        return is;
//    }
//
//    private BlockKey getRenderBlock(BlockPos pos, BlockKey is) {
//        BlockChoiceHook over = overrides.get(pos);
//        if (over != null) {
//            ItemStack at = over.getBlock(pos);
//            is = at != null ? BlockKey.fromItem(at) : null;
//        }
//        if (is != null && is.blockID != null) {
//            BlockChoiceHook bc = choiceHooks.get(is.blockID);
//            if (bc != null) {
//                is = BlockKey.fromItem(bc.getBlock(pos));
//            }
//        }
//        return is;
//    }
//
//    public void drawSlice(int j, int k, Font fr) {
//        double s = 1;
//        int max = Math.max(array.getSizeX(), array.getSizeZ());
//        double dd = max > 16 ? Math.max(12, 28-max) : 14;
//        if (max >= 20) {
//            s -= 0.05*(max-20);
//            dd -= 0.625*(max-20);
//        }
//        GL11.glPushMatrix();
//        GL11.glScaled(s, s, s);
//        int y = array.getMinY()+secY;
//        int ox = 120;
//        int oy = 105;
//        for (int x = array.getMinX(); x <= array.getMaxX(); x++) {
//            for (int z = array.getMinZ(); z <= array.getMaxZ(); z++) {
//                ItemStack is = this.getRenderStack(new BlockPos(x, y, z));
//                if (is != null && is.getItem() != null) {
//                    double dx = (x-array.getMidX())*dd;
//                    double dz = (z-array.getMidZ())*dd;
//                    ReikaGuiAPI.instance.drawItemStackWithTooltip(itemRender, fr, is, (int)((j+dx+ox)/s), (int)((k+dz+oy)/s));
//                }
//            }
//        }
//        GL11.glPopMatrix();
//    }
//    /*
//    public void draw3D(int j, int k) {
//        int dd = 12;
//        int ddy = 12;
//        HashMap<Vector3f, CoordStack> render = new HashMap();
//        Matrix4f rot = new Matrix4f();
//        ReikaVectorHelper.euler213Sequence(rot, rx, ry, rz);
//        if (array.isEmpty())
//            return;
//        for (int y = array.getMinY(); y <= array.getMaxY(); y++) {
//            for (int x = array.getMinX(); x <= array.getMaxX(); x++) {
//                for (int z = array.getMinZ(); z <= array.getMaxZ(); z++) {
//                    ItemStack is = this.getRenderStack(new BlockPos(x, y, z));
//                    if (is != null && is.getItem() != null) {
//                        int dx = x-array.getMidX();
//                        int dy = y-array.getMidY();
//                        int dz = z-array.getMidZ();
//                        Vector3f in = new Vector3f(dx, dy, dz);
//                        Vector3f vec = ReikaVectorHelper.multiplyVectorByMatrix(in, rot);
//                        int px = Math.round(vec.x*dd+vec.z*dd);
//                        int py = Math.round(-vec.x*dd/2+vec.z*dd/2-vec.y*ddy);
//                        int pz = 0;//250;
//                        render.put(vec, new CoordStack(is, px, py, pz));
//                    }
//                }
//            }
//        }
//        double max = Math.max(array.getSizeY()*1, Math.sqrt(Math.pow(array.getSizeX(), 2)+Math.pow(array.getMaxZ(), 2)));
//        //ReikaJavaLibrary.pConsole(max);
//        GL11.glPushMatrix();
//        double d = 2;
//        if (max >= 18) {
//            d = 0.675;
//        }
//        else if (max >= 14) {
//            d = 0.8;
//        }
//        else if (max >= 12) {
//            d = 0.95;
//        }
//        else if (max >= 10) {
//            d = 1.2;
//        }
//        else if (max >= 8) {
//            d = 1.5;
//        }
//        else if (max >= 4) {
//            d = 1.75;
//        }
//        GL11.glScaled(d, d, 1);
//        int ox = (int)((j+122)/d);
//        int oy = (int)((k+92)/d);
//        if (d > 1)
//            ox -= 5;
//        if (d > 1)
//            oy -= 5;
//        ArrayList<Vector3f> keys = new ArrayList(render.keySet());
//        Collections.sort(keys, visibility);
//        for (Vector3f vec : keys) {
//            CoordStack is = render.get(vec);
//            if (is.item != null && is.item.getItem() != null) {
//                GL11.glPushMatrix();
//                stack.translate(0, 0, is.coord.zCoord);
//                double scale = 1;
//                int ox2 = 0;
//                int oy2 = 0;
//                BlockRenderHook brh = renderHooks.get(is.item);
//                if (brh != null) {
//                    scale = brh.getScale();
//                    ox2 = brh.getOffsetX();
//                    oy2 = brh.getOffsetY();
//                }
//                GL11.glScaled(scale, scale, 1);
//                ReikaGuiAPI.instance.drawItemStack(itemRender, is.item, (int)((is.coord.xCoord+ox)/scale)+ox2, (int)((is.coord.yCoord+oy)/scale)+oy2);
//                GL11.glPopMatrix();
//            }
//        }
//        GL11.glPopMatrix();
//    }
//     */
//    public void draw3D(int j, int k, float ptick, boolean transl) {
//
//        if (array.isEmpty())
//            return;
//
//        double max = Math.max(array.getSizeY()*1, Math.sqrt(Math.pow(array.getSizeX(), 2)+Math.pow(array.getMaxZ(), 2)));
//        //ReikaJavaLibrary.pConsole(max);
//        GL11.glPushMatrix();
//        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
//        RenderSystem.enableBlend();
//        GL11.glEnable(GL11.GL_DEPTH_TEST);
//
//        //GL11.glFrontFace(GL11.GL_CW);
//
//        if (transl) {
//            int sc = ReikaRenderHelper.getGUIScale();
//            GuiScreen scr = Minecraft.getInstance().currentScreen;
//            stack.translate(j*0+scr.width/2D+16/sc, k*0+scr.height/2D+16/sc, 256);
//
//            double s = 12;
//
//            double d = 2;
//            if (max >= 24) {
//                d = 0.5;
//            }
//            else if (max >= 21) {
//                d = 0.625;
//            }
//            else if (max >= 18) {
//                d = 0.675;
//            }
//            else if (max >= 14) {
//                d = 0.8;
//            }
//            else if (max >= 12) {
//                d = 0.95;
//            }
//            else if (max >= 10) {
//                d = 1.2;
//            }
//            else if (max >= 8) {
//                d = 1.5;
//            }
//            else if (max >= 4) {
//                d = 1.75;
//            }
//
//            //double drx = (array.getMidX()-array.getMinX());//-2.75/d;
//            //double dry = (array.getMidY()-array.getMinY());//-2.75/d;
//            //double drz = (array.getMidZ()-array.getMinZ());//-2.75/d;
//
//            double dr = -5.75*d;
//            //stack.translate(drx, dry, drz);
//            stack.translate(dr, dr, dr);
//            GL11.glRotated(rx, 1, 0, 0);
//            GL11.glRotated(ry, 0, 1, 0);
//            GL11.glRotated(rz, 0, 0, 1);
//            stack.translate(-dr, -dr, -dr);
//            //stack.translate(-drx, -dry, -drz);
//
//            GL11.glScaled(-d*s, -d*s, -d*s);
//        }
//
//        //stack.translate(-array.getMinX(), -array.getMinY(), -array.getMinZ());
//
//        tileRendering = true;
//        tileRenderingReal = !transl;
//        staticRenderAccess = access;
//
//        ReikaTextureHelper.bindTerrainTexture();
//        Tessellator.instance.startDrawingQuads();
//        for (int x = array.getMinX(); x <= array.getMaxX(); x++) {
//            for (int y = array.getMinY(); y <= array.getMaxY(); y++) {
//                for (int z = array.getMinZ(); z <= array.getMaxZ(); z++) {
//                    PositionData p = access.getData(x, y, z);
//                    if (p.isAlpha)
//                        continue;
//                    //ReikaJavaLibrary.pConsole(p+" @ "+x+","+y+","+z);
//                    if (p.block.blockID != Blocks.air) {
//                        BlockKey bk = this.getRenderBlock(new BlockPos(x, y, z), p.block);
//                        if (bk == null)
//                            continue;
//                        if (!bk.equals(p.block)) {
//                            access.data[x-array.getMinX()][y-array.getMinY()][z-array.getMinZ()] = new PositionData(bk.blockID, bk.metadata, p.tile);
//                        }
//                        renderer.renderBlockByRenderType(bk.blockID, x, y, z);
//                        //ReikaJavaLibrary.pConsole("Rendering "+bk+" @ "+x+","+y+","+z);
//                    }
//                }
//            }
//        }
//        Tessellator.instance.draw();
//
//        if (access.hasAnyAlpha) {
//            BlendMode.ADDITIVE2.apply();
//
//            ReikaTextureHelper.bindTerrainTexture();
//            Tessellator.instance.startDrawingQuads();
//            for (int x = array.getMinX(); x <= array.getMaxX(); x++) {
//                for (int y = array.getMinY(); y <= array.getMaxY(); y++) {
//                    for (int z = array.getMinZ(); z <= array.getMaxZ(); z++) {
//                        PositionData p = access.getData(x, y, z);
//                        if (!p.isAlpha)
//                            continue;
//                        if (p.block.blockID != Blocks.air) {
//                            //ReikaJavaLibrary.pConsole(p+" @ "+x+","+y+","+z);
//                            BlockKey bk = this.getRenderBlock(new BlockPos(x, y, z), p.block);
//                            if (bk == null)
//                                continue;
//                            if (!bk.equals(p.block)) {
//                                access.data[x-array.getMinX()][y-array.getMinY()][z-array.getMinZ()] = new PositionData(bk.blockID, bk.metadata, p.tile);
//                            }
//                            renderer.renderBlockByRenderType(bk.blockID, x, y, z);
//                            //ReikaJavaLibrary.pConsole("Rendering "+bk+" @ "+x+","+y+","+z);
//                        }
//                    }
//                }
//            }
//            Tessellator.instance.draw();
//        }
//
//        RenderSystem.defaultBlendFunc();
//
//        for (int x = array.getMinX(); x <= array.getMaxX(); x++) {
//            for (int y = array.getMinY(); y <= array.getMaxY(); y++) {
//                for (int z = array.getMinZ(); z <= array.getMaxZ(); z++) {
//                    PositionData p = access.getData(x, y, z);
//                    if (p.tile != null && p.useTESR) {
//                        try {
//                            p.tile.level = Minecraft.getInstance().theWorld;
//                            BlockEntityRendererDispatcher.instance.renderBlockEntityAt(p.tile, x, y, z, ptick);
//                        }
//                        catch (Exception e) {
//                            DragonAPI.LOGGER.error("Error rendering structure BlockEntity @ "+x+", "+y+", "+z+": "+p.tile);
//                            e.printStackTrace();
//                            p.useTESR = false;
//                        }
//                    }
//                }
//            }
//        }
//
//        tileRendering = tileRenderingReal = false;
//        staticRenderAccess = null;
//
//        for (BlockPos c : entities.keySet()) {
//            EntityRender e = entities.get(c);
//            e.renderer.doRender(e.entity, c.getX()+0.5, c.getY()+0.5+0.375, c.getZ()+0.5, 0, 0);
//            e.entity.onUpdate();
//        }
//
//        GL11.glPopMatrix();
//        GL11.glPopAttrib();
//    }
//
//    public static RenderAccess getRenderAccess() {
//        return staticRenderAccess;
//    }
//
//    protected static class PositionData {
//
//        private final BlockKey block;
//        private final BlockEntity tile;
//        private boolean useTESR;
//
//        public boolean isAlpha;
//
//        private PositionData(Block b) {
//            this(b, null);
//        }
//
//        protected PositionData(Block b, BlockEntity te) {
//            block = new BlockKey(b);
//            tile = te;
//            useTESR = tile != null && BlockEntityRendererDispatcher.instance.getSpecialRenderer(tile) != null;
//        }
//
//        @Override
//        public String toString() {
//            return block.toString()+"|"+tile;
//        }
//
//    }
//
//    protected static class RenderAccess implements BlockGetter {
//
//        protected final PositionData[][][] data;
//        protected final BlockPos negativeCorner;
//        protected final BlockPos offset;
//
//        private boolean hasAnyAlpha = false;
//
//        private RenderAccess(FilledBlockArray arr, HashSet<BlockPos> alpha) {
//            offset = new BlockPos(-arr.getMidX(), -arr.getMidY(), -arr.getMidZ());
//            arr.offset(offset.getX(), offset.getY(), offset.getZ());
//
//            data = new PositionData[arr.getSizeX()][arr.getSizeY()][arr.getSizeZ()];
//            negativeCorner = new BlockPos(arr.getMinX(), arr.getMinY(), arr.getMinZ());
//
//            int axo = Integer.MAX_VALUE;
//            int ayo = Integer.MAX_VALUE;
//            int azo = Integer.MAX_VALUE;
//            int bxo = Integer.MIN_VALUE;
//            int byo = Integer.MIN_VALUE;
//            int bzo = Integer.MIN_VALUE;
//            if (alpha != null) {
//                for (BlockPos c : alpha) {
//                    axo = Math.min(axo, c.getX());
//                    ayo = Math.min(ayo, c.getY());
//                    azo = Math.min(azo, c.getZ());
//                    bxo = Math.max(bxo, c.getX());
//                    byo = Math.max(byo, c.getY());
//                    bzo = Math.max(bzo, c.getZ());
//                }
//                int cxo = axo+(bxo-axo+1)/2;
//                int cyo = ayo+(byo-ayo+1)/2;
//                int czo = azo+(bzo-azo+1)/2;
//
//                axo = cxo-arr.getMidX();
//                ayo = cyo-arr.getMidY();
//                azo = czo-arr.getMidZ();
//            }
//
//            for (int i = 0; i < data.length; i++) {
//                for (int j = 0; j < data[i].length; j++) {
//                    for (int k = 0; k < data[i][j].length; k++) {
//                        int x = i+negativeCorner.getX();
//                        int y = j+negativeCorner.getY();
//                        int z = k+negativeCorner.getZ();
//                        Block b = arr.getBlockAt(x, y, z);
//                        //ReikaJavaLibrary.pConsole(x+", "+y+", "+z+"  >  "+b);
//                        BlockEntity te2 = arr.getBlockEntityAt(x, y, z);
//                        BlockEntity te = b != null ? te2 != null ? te2 : b.createBlockEntity(Minecraft.getInstance().level) : null;
//                        if (te != null) {
//                            te.getBlockPos().offset(x, y, z); //todo see if this actually replaces the pos, or adds to it
//                        }
//                        data[i][j][k] = b != null ? new PositionData(b, te) : new PositionData(Blocks.AIR);
//                        data[i][j][k].isAlpha = b != null && alpha != null && alpha.contains(new BlockPos(x+axo, y+ayo, z+azo));
//                        hasAnyAlpha |= data[i][j][k].isAlpha;
//                    }
//                }
//            }
//
//            //ReikaJavaLibrary.pConsole(arr);
//        }
//
//        @Override
//        public Block getBlock(BlockPos pos) {
//            return this.getData(pos).block.blockID;
//        }
//
//        @Override
//        public BlockEntity getBlockEntity(BlockPos pos) {
//            return this.getData(pos).tile;
//        }
//
//        @Override
//        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int side) {
//            return 0;
//        }
//
//        @Override
//        public int getBlockMetadata(int x, int y, int z) {
//            return this.getData(x, y, z).block.metadata;
//        }
//
//        @Override
//        public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
//            return 0;
//        }
//
//        @Override
//        public boolean isAirBlock(int x, int y, int z) {
//            return false;
//        }
//
//        @Override
//
//        public Biome getBiomeGenForCoords(int x, int z) {
//            return Biome.ocean;
//        }
//
//        @Override
//
//        public int getHeight() {
//            return 0;
//        }
//
//        @Override
//
//        public boolean extendedLevelsInChunkCache() {
//            return false;
//        }
//
//        @Override
//        public boolean isSideSolid(int x, int y, int z, Direction side, boolean _default) {
//            return this.getData(x, y, z).block.blockID.isSideSolid(this, x, y, z, side);
//        }
//
//        private PositionData getData(BlockPos pos) {
//
//            x -= negativeCorner.xCoord;
//            y -= negativeCorner.yCoord;
//            z -= negativeCorner.zCoord;
//			/*
//			ReikaJavaLibrary.pConsole(x+","+y+","+z+" > "+this.inBounds(x, y, z));
//			if (this.inBounds(x, y, z))
//				ReikaJavaLibrary.pConsole(" > "+data[x][y][z]);
//			 */
//            return this.inBounds(x, y, z) ? data[x][y][z] : new PositionData(Blocks.air);
//        }
//
//        private boolean inBounds(int x, int y, int z) {
//            return x >= 0 && y >= 0 && z >= 0 && x < data.length && y < data[0].length && z < data[0][0].length;
//        }
//
//    }
//
//    public interface BlockRenderHook {
//
//        public double getScale();
//        public int getOffsetX();
//        public int getOffsetY();
//
//    }
//
//    public interface BlockChoiceHook {
//
//        public ItemStack getBlock(BlockPos pos);
//
//    }
//
//    public static final class SingleBlockChoice implements BlockChoiceHook {
//
//        private final ItemStack item;
//
//        public SingleBlockChoice(ItemStack is) {
//            item = is.copy();
//        }
//
//        @Override
//        public ItemStack getBlock(BlockPos pos) {
//            return item;
//        }
//
//    }
//
//    public static class EntityRender {
//
//        public final Entity entity;
//        public final Render renderer;
//
//        public EntityRender(Entity e) {
//            this(e, ReikaEntityHelper.getEntityRenderer(e.getClass()));
//        }
//
//        public EntityRender(Entity e, Render r) {
//            entity = e;
//            renderer = r;
//        }
//
//    }
//
//    public static interface StructureRenderingParticleSpawner {
//
//        public void tickFX();
//
//    }
//}
