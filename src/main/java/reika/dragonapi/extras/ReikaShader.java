package reika.dragonapi.extras;

/*
public class ReikaShader implements ShaderHook, TickRegistry.TickHandler {
	public static final ReikaShader instance = new ReikaShader();

	private ShaderProgram stencilShader;
	private ShaderProgram effectShader;
	private ReikaRenderHelper.ScratchFramebuffer stencil;

	private final ArrayList<ShaderPoint> points = new ArrayList<>();
	private boolean rendering;
	private float intensity;

	private ReikaShader() {

	}

	public void register() {
		stencilShader = ShaderRegistry.createShader(DragonAPI.getInstance(), "reika_stencil", DragonAPI.class, "resources/", ShaderDomain.ENTITY).setEnabled(false);
		effectShader = ShaderRegistry.createShader(DragonAPI.getInstance(), "reika_effect", DragonAPI.class, "resources/", ShaderDomain.GLOBALNOGUI).setEnabled(false);
		TickRegistry.instance.registerTickHandler(this);
		stencil = new ReikaRenderHelper.ScratchFramebuffer(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), true);
		stencil.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
		effectShader.setHook(this);
		stencilShader.setHook(this);
	}

	public void updatePosition(Player ep) {
		if (rendering)
			return;
		if (ep == Minecraft.getInstance().player)
			return;
		boolean flag = true;
		long time = ep.level.getGameTime();
		Iterator<ShaderPoint> it = points.iterator();
		while (it.hasNext()) {
			ShaderPoint p = it.next();
			if (p.position.equals(ep.getX(), ep.getY(), ep.getZ())) {
				p.refresh(time);
				flag = false;
			}
			if (p.tick(time)) {
				it.remove();
			}
		}
		if (flag) {
			ShaderPoint p = new ShaderPoint(ep, points.isEmpty() ? null : points.get(0));
			points.add(0, p);
		}
	}

	public void prepareRender(Player ep, PoseStack poseStack) {
		Minecraft mc = Minecraft.getInstance();
		if (ep == mc.player)
			return;
		if (points.isEmpty())
			return;

		poseStack.pushPose();
        assert mc.player != null;
        double dist = ep.distanceToSqr(mc.player);
		if (mc.options.keyTogglePerspective > 0) {
			dist += ReikaRenderHelper.thirdPersonDistance;
		}
		float ptick = ReikaRenderHelper.getPartialTickTime();
		double px = ep.lastTickPosX+(ep.posX-ep.lastTickPosX)*ptick;
		double py = ep.lastTickPosY+(ep.posY-ep.lastTickPosY)*ptick;
		double pz = ep.lastTickPosZ+(ep.posZ-ep.lastTickPosZ)*ptick;



		poseStack.translate(RenderManager.renderPosX-ep.getX(), RenderManager.renderPosY-ep.getY(), RenderManager.renderPosZ-ep.getZ());
		poseStack.translate(0, -0.8, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		rendering = true;
		boolean flag = false;
		for (ShaderPoint pt : points) {
			float f = pt.getIntensity();
			if (f > 0) {
				HashMap<String, Object> map = new HashMap();
				map.put("distance", dist);
				map.put("age", pt.age/(float)pt.LIFESPAN);
				DecimalPosition p = pt.position;
				map.put("dx", p.xCoord-px);
				map.put("dy", p.yCoord-py);
				map.put("dz", p.zCoord-pz);
				//stencilShader.addFocus(p.xCoord, p.yCoord, p.zCoord);
				stencilShader.addFocus(ep);
				stencilShader.modifyLastCompoundFocus(f, map);
				flag = true;
			}
		}
		stencilShader.setEnabled(flag);
		effectShader.setEnabled(flag);
		if (flag && points.get(0).speed > 0.08) {
			intensity = Math.min(1, intensity*1.02F+0.005F);
		}
		else {
			intensity = Math.max(0, intensity*0.99F-0.02F);
		}
		effectShader.setIntensity(intensity);
		rendering = false;
		poseStack.popPose();
	}

	public void render(Minecraft mc) {
		stencil.clear();
		stencil.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
		stencil.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
		ReikaRenderHelper.renderFrameBufferToItself(stencil, mc.displayWidth, mc.displayHeight, stencilShader);
		ReikaRenderHelper.setRenderTarget(mc.getFramebuffer());
		if (stencilShader.isEnabled()) {
			//stencil.renderWithAlpha(mc.displayWidth, mc.displayHeight);
		}

		stencilShader.clearFoci();
		stencilShader.setEnabled(false);
	}

	public Framebuffer getStencil() {
		return stencil;
	}

	@Override
	public void onPreRender(ShaderProgram s) {
		if (s == effectShader) {
			int base = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
			int offset = 2;
			RenderSystem.setShaderTexture(offset, stencil); // Texture unit 1
			s.setField("stencilTex", offset);
			s.setField("stencilVal", (float)offset);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, stencil.framebufferTexture);
			GL13.glActiveTexture(base); // Texture unit 0
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, Minecraft.getInstance().getFramebuffer().framebufferTexture);
		}
	}

	@Override
	public void onPostRender(ShaderProgram s) {
		//if (!effectShader.hasOngoingFoci()) {
		if (s == effectShader)
			s.setEnabled(false);
		//	effectShader.clearFoci();
		//}
	}

	@Override
	public void updateEnabled(ShaderProgram s) {

	}

	private static class ShaderPoint {

		private static final int LIFESPAN = 30;

		private final DecimalPosition position;
		private final double speed;

		private long creation;
		private int age;

		private ShaderPoint(Player ep, ShaderPoint last) {
			position = new DecimalPosition(ep);
			creation = ep.level.getGameTime();
			double vx = ep.motionX;
			double vy = ep.motionY+0.0784000015258789;
			double vz = ep.motionZ;
			double v = ReikaMathLibrary.py3d(vx, vy, vz);
			if (last != null)
				v += last.position.getDistanceTo(position);
			speed = v;
		}

		public void refresh(long world) {
			age = 0;
			creation = world;
		}

		public boolean tick(long world) {
			age++;
			long val = Math.max(age, world-creation);
			return val >= LIFESPAN;
		}

		private float getAgeFactor() {
			if (true) {
				return 1F-age/(float)LIFESPAN;
			}
			if (age < 4) {
				return 0;
			}
			else if (age < 10) {
				return (age-4)/6F;
			}
			return 1F-(age-10)/(float)(LIFESPAN-10);
		}

		public float getIntensity() {
			return Math.min(1, (float)Math.pow(this.getAgeFactor()*speed*5, 1.5));
		}

	}

	@Override
	public void tick(TickRegistry.TickType type, Object... tickData) {
		Player ep = (Player)tickData[0];
		if (ep.level.isClientSide() && ep.getUUID() == DragonAPI.Reika_UUID /*ReikaPlayerAPI.isReika(ep)*//*) {
			this.updatePosition(ep);
		}
	}

	@Override
	public EnumSet<TickRegistry.TickType> getType() {
		return EnumSet.of(TickRegistry.TickType.PLAYER);
	}

	@Override
	public boolean canFire(TickEvent.Phase p) {
		return p == TickEvent.Phase.END;
	}

	@Override
	public String getLabel() {
		return "reikashader";
	}

}
*/