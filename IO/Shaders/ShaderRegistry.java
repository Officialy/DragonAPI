package Reika.DragonAPI.IO.Shaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.Util;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonAPIInit;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Exception.RegistrationException;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Libraries.IO.ReikaRenderHelper;
import Reika.DragonAPI.Libraries.Java.ReikaObfuscationHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ShaderRegistry {

	private ShaderRegistry() {throw new RuntimeException("The class "+this.getClass()+" cannot be instantiated!");}

	private static final HashMap<String, ShaderProgram> shaders = new HashMap();
	private static final EnumMap<ShaderDomain, ArrayList<ShaderProgram>> shaderSets = new EnumMap(ShaderDomain.class);

	private static final int GLSL_VERSION = 120;

	private static ShaderProgram currentlyRunning;
	private static HashSet<String> errors = new HashSet();

	private static String BASE_DATA;

	public static ShaderProgram createShader(DragonAPIMod mod, String id, Class root, String pathPre, ShaderDomain dom) {
		if (!OpenGlHelper.shadersSupported)
			return null;
		if (shaders.containsKey(id))
			error(mod, "Shader id "+id+" is already in use!", null);
		ShaderProgram sh = new ShaderProgram(mod, root, pathPre, id, dom);
		try {
			sh.load();
		}
		catch (IOException e) {
			error(mod, "Shader program data could not be loaded!", null, e);
		}
		shaders.put(sh.identifier, sh);
		addShaderToSet(dom, sh);
		DragonAPICore.log("Registered "+mod.getTechnicalName()+" shader "+sh);
		return sh;
	}

	private static void addShaderToSet(ShaderDomain dom, ShaderProgram s) {
		ArrayList<ShaderProgram> li = shaderSets.get(dom);
		if (li == null) {
			li = new ArrayList();
			shaderSets.put(dom, li);
		}
		li.add(s);
		Collections.sort(li);
	}

	private static void removeShaderFromSet(ShaderDomain dom, ShaderProgram s) {
		ArrayList<ShaderProgram> li = shaderSets.get(dom);
		if (li != null) {
			li.remove(s);
		}
	}

	public static void reloadShader(String id) throws IOException {
		DragonAPICore.log("Reloading shader "+id);
		shaders.get(id).load();
		errors.remove(id);
	}

	public static void runShader(String id) {
		runShader(shaders.get(id));
	}

	public static boolean runShader(ShaderProgram sh) {
		if (!OpenGlHelper.shadersSupported || sh == null)
			return false;
		if (GuiScreen.isCtrlKeyDown() && GuiScreen.isShiftKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_X))
			errors.clear();
		if (errors.contains(sh.identifier))
			return false;
		if (currentlyRunning != null && currentlyRunning != sh)
			error(sh.owner, "Cannot start one shader while another is running!", null);
		if (GuiScreen.isCtrlKeyDown() && GuiScreen.isShiftKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_X)) {
			try {
				reloadShader(sh.identifier);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (GuiScreen.isCtrlKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_LMENU) && Keyboard.isKeyDown(Keyboard.KEY_C)) {
			return false;
		}
		currentlyRunning = sh;
		return sh.run();
	}

	public static void completeShader() {
		if (!OpenGlHelper.shadersSupported)
			return;
		if (currentlyRunning == null)
			error(DragonAPIInit.instance, "Cannot stop a shader when none is running!", null);
		GL20.glUseProgram(0);
		if (ReikaObfuscationHelper.isDeObfEnvironment()) {
			int res = GL11.glGetError();
			if (res != GL11.GL_NO_ERROR) {
				DragonAPICore.logError("Shader "+currentlyRunning+" threw error: "+Util.translateGLErrorString(res)+"!");
				errors.add(currentlyRunning.identifier);
			}
		}
		currentlyRunning = null;
	}

	static int constructShader(DragonAPIMod mod, InputStream data, ShaderTypes type) throws IOException {
		if (data == null)
			error(mod, "Shader has null program data!", type);
		int id = GL20.glCreateShader(type.glValue);

		if (id == 0)
			error(mod, "Shader was not able to be assigned an ID!", type);

		if (BASE_DATA == null) {
			BASE_DATA = readData(DragonAPICore.class.getResourceAsStream("Resources/shaderbase.txt"));
		}
		String sdata = "#version "+GLSL_VERSION+"\n";
		if (type == ShaderTypes.FRAGMENT) {
			sdata = sdata+"uniform sampler2D bgl_RenderedTexture;\n";
		}
		sdata = sdata+BASE_DATA+"\n";
		sdata = sdata+readData(data);
		GL20.glShaderSource(id, sdata);
		GL20.glCompileShader(id);

		if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
			error(mod, "Shader was not able to be constructed: "+ShaderRegistry.parseError(id), type);

		return id;
	}

	static void error(DragonAPIMod mod, String msg, ShaderTypes type) {
		error(mod, msg, type, null);
	}

	static void error(DragonAPIMod mod, String msg, ShaderTypes type, Exception e) {
		String t = type != null ? type.name() : "";
		if (DragonAPICore.hasGameLoaded()) { //do not crash game if already running and shader is being reloaded
			mod.getModLogger().logError(t+" shader error: "+msg);
			if (e != null)
				e.printStackTrace();
		}
		else {
			throw new RegistrationException(mod, t+" "+msg, e);
		}
	}

	private static String readData(InputStream data) {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> li = ReikaFileReader.getFileAsLines(data, true);
		for (String s : li) {
			sb.append(s);
			sb.append("\n");
		}
		return sb.toString();
	}

	public static void runShaderDomain(Framebuffer fb, int w, int h, ShaderDomain sd) {
		ArrayList<ShaderProgram> li = shaderSets.get(sd);
		if (li != null) {
			//Matrix4f model = ReikaRenderHelper.getModelviewMatrix();
			//Matrix4f proj = ReikaRenderHelper.getProjectionMatrix();
			for (ShaderProgram s : li) {
				ReikaRenderHelper.renderFrameBufferToItself(fb, w, h, s);
			}
		}
	}

	public static String parseError(int programID) {
		return GL20.glGetShaderInfoLog(programID, GL20.glGetShaderi(programID, GL20.GL_INFO_LOG_LENGTH));
	}

	public static enum ShaderDomain {
		WORLD,
		TESR,
		ENTITY,
		GUI,
		GLOBAL,
		GLOBALNOGUI();
	}

	public static enum ShaderTypes {
		FRAGMENT(GL20.GL_FRAGMENT_SHADER, "frag"),
		VERTEX(GL20.GL_VERTEX_SHADER, "vert"),
		TESSELLATION(GL40.GL_TESS_EVALUATION_SHADER, "tess");

		public final int glValue;
		public final String extension;

		private ShaderTypes(int id, String s) {
			glValue = id;
			extension = s;
		}
	}

}
