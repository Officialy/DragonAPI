/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.io;


import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.instantiable.data.maps.MultiMap;
import reika.dragonapi.interfaces.DataProvider;
import reika.dragonapi.io.ReikaFileReader;
import reika.dragonapi.io.ReikaXMLBase;
import net.minecraft.ChatFormatting;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class XMLInterface {

	public static final String NULL_VALUE = "#NULL!";
	private final boolean requireFile;
	private final LoadPoint loadData;
	private final LoadFormat format;
	private final String pathString;
	private final HashMap<String, String> data = new HashMap();
	private final MultiMap<String, String> tree = new MultiMap();
	private Document doc;
	private Class referenceClass;
	private boolean isEncrypted;
	private boolean hasLoaded;

	private XMLInterface(LoadFormat f, Object raw, String disp, boolean crashIfNull) {
		format = f;
		loadData = new LoadPoint();
		loadData.paths.add(raw);
		requireFile = crashIfNull;
		pathString = disp;
	}

	public XMLInterface(File path, boolean crashIfNull) {
		this(LoadFormat.FILE, path, ReikaFileReader.getRealPath(path), crashIfNull);
	}

	public XMLInterface(Class root, String path) {
		this(root, path, false);
	}

	public XMLInterface(Class root, String path, boolean crashIfNull) {
		this(LoadFormat.JARPATH, path, path + " relative to " + root.getName(), crashIfNull);
		referenceClass = root;
	}

	public XMLInterface(DataProvider p, boolean crashIfNull) {
		this(LoadFormat.CUSTOM, p, "Custom loader " + p, crashIfNull);
	}

	public void setFallback(String s) {
		loadData.addEntry(s);
	}

	public void setEncrypted() {
		isEncrypted = true;
	}

	public XMLInterface init() {
		try (InputStream in = loadData.getInputStream()) {
			try (InputStream in2 = isEncrypted ? ReikaFileReader.decryptInputStream(in, StandardCharsets.UTF_8) : in) {
				doc = ReikaXMLBase.getXMLDocument(in2);
				this.readFileToMap();
				hasLoaded = true;
			}
		} catch (FileNotFoundException e) {
			if (requireFile)
				throw new RuntimeException("XML not found at " + pathString, e);
			else
				e.printStackTrace();
		} catch (SAXException e) {
			if (requireFile)
				throw new RuntimeException("Could not parse XML at " + pathString, e);
			else
				e.printStackTrace();
		} catch (IOException e) {
			if (requireFile)
				throw new RuntimeException("Could not load XML at " + pathString, e);
			else
				e.printStackTrace();
		}
		return this;
	}

	public void reread() {
		if (format == LoadFormat.CUSTOM && !((DataProvider) loadData.paths.get(0)).canBeReloaded())
			throw new MisuseException("This data provider cannot be reloaded!");
		hasLoaded = false;
		data.clear();
		tree.clear();
		this.init();
	}

	private void readFileToMap() {
		this.recursiveRead("$TOP$", doc);
	}

	private void recursiveRead(String parent, Node n) {
		if (n == null)
			return;
		NodeList li = n.getChildNodes();
		int len = li.getLength();
		for (int i = 0; i < len; i++) {
			Node ch = li.item(i);
			String key = ReikaXMLBase.getNodeNameTree(ch);
			tree.addValue(parent, key);
			if (ch.getNodeType() == Node.ELEMENT_NODE) {
				//ReikaJavaLibrary.pConsole(ch.getNodeName());
				this.recursiveRead(key, ch);
			} else if (ch.getNodeType() == Node.TEXT_NODE) {
				String val = ch.getNodeValue();
				if (val != null) {
					if (val.equals("\n"))
						val = null;
					else {
						if (val.startsWith("\n"))
							val = val.substring(1);
						if (val.endsWith("\n"))
							val = val.substring(0, val.length() - 1);
					}
					if (val != null && val.equals("\n"))
						val = null;
				}
				if (val != null) {
					//ReikaJavaLibrary.pConsole("TREE: "+ReikaXMLBase.getNodeNameTree(ch));
					if (data.containsKey(key))
						;//throw new RuntimeException("Your input XML has multiple node trees with the EXACT same names! Resolve this!");
					data.put(key, this.cleanString(val));
				}
			}
		}
	}

	private String cleanString(String val) {
		val = val.replace("\t", "");
		while (!val.isEmpty() && val.endsWith("\\n")) {
			val = val.substring(0, val.length() - 2);
		}
		while (!val.isEmpty() && val.charAt(0) == ' ') {
			val = val.substring(1);
		}
		while (!val.isEmpty() && val.charAt(val.length() - 1) == ' ') {
			val = val.substring(0, val.length() - 1);
		}
		val = val.replaceAll("\\[i\\]", ChatFormatting.ITALIC.toString());
		val = val.replaceAll("\\[b\\]", ChatFormatting.BOLD.toString());
		val = val.replaceAll("\\[u\\]", ChatFormatting.UNDERLINE.toString());
		val = val.replaceAll("\\[s\\]", ChatFormatting.STRIKETHROUGH.toString());
		val = val.replaceAll("\\[/i\\]", ChatFormatting.RESET.toString());
		val = val.replaceAll("\\[/b\\]", ChatFormatting.RESET.toString());
		val = val.replaceAll("\\[/u\\]", ChatFormatting.RESET.toString());
		val = val.replaceAll("\\[/s\\]", ChatFormatting.RESET.toString());
		return val;
	}

	public String getValueAtNode(String name) {
		if (!hasLoaded)
			throw new MisuseException("You cannot query an XML data set before reading it from disk!");
		String dat = data.get(name);
		if (dat == null)
			dat = NULL_VALUE;
		return dat;
	}

	public boolean nodeExists(String name) {
		if (!hasLoaded)
			throw new MisuseException("You cannot query an XML data set before reading it from disk!");
		return data.containsKey(name);
	}

	/**
	 * Only returns "tree" nodes, not text ones.
	 */
	public Collection<String> getNodesWithin(String name) {
		if (!hasLoaded)
			throw new MisuseException("You cannot query an XML data set before reading it from disk!");
		return name == null ? this.getTopNodes() : tree.get(name);
	}

	public Collection<String> getTopNodes() {
		if (!hasLoaded)
			throw new MisuseException("You cannot query an XML data set before reading it from disk!");
		return tree.get("$TOP$");
	}

	@Override
	public String toString() {
		if (!hasLoaded)
			return "NOT LOADED";
		return data.toString();
	}

	private enum LoadFormat {
		JARPATH(),
		FILE(),
		CUSTOM();

		private InputStream getInputStream(Object... data) throws IOException {
			switch (this) {
				case FILE:
					return new FileInputStream((File) data[0]);
				case JARPATH:
					InputStream ret = ((Class) data[0]).getResourceAsStream((String) data[1]);
					if (ret == null) {
						String s = ((Class) data[0]).getCanonicalName();
						throw new FileNotFoundException(s + " >> " + data[1]);
					}
					return ret;
				case CUSTOM:
					return ((DataProvider) data[0]).getDataStream();
			}
			return null; //never happens
		}

	}

	private class LoadPoint {

		private final ArrayList<Object> paths = new ArrayList<>();

		public void addEntry(String s) {
			Object add = s;
			switch (format) {
				case FILE:
					add = new File(s);
					break;
				case JARPATH:
					break;
				case CUSTOM:
					break;
			}
			paths.add(add);
		}

		private InputStream getInputStream() throws IOException {
			IOException ex = null;
			for (int i = 0; i < paths.size(); i++) {
				try {
					switch (format) {
						case FILE:
							return format.getInputStream(paths.get(i));
						case JARPATH:
							return format.getInputStream(referenceClass, paths.get(i));
						case CUSTOM:
							return format.getInputStream(paths.get(i));
					}
				} catch (IOException e) {
					if (i == 0) {
						ex = e;
					} else if (i == paths.size() - 1) {

					}
				}
			}
			throw ex;
		}

	}
}
