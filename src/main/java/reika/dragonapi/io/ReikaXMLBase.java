/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.io;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReikaXMLBase {

	private ReikaXMLBase() {
		throw new RuntimeException("The class " + this.getClass() + " cannot be instantiated!");
	}

	public static Document getXMLDocument(InputStream in) throws SAXException, IOException {
		if (in == null)
			throw new IllegalArgumentException("XML input stream cannot be null");
		ArrayList<String> li = ReikaFileReader.getFileAsLines(in, true, StandardCharsets.UTF_8);
		if (li.isEmpty())
			throw new SAXException("Cannot parse an empty XML document");
		String first = li.get(0);
		if (!first.isEmpty() && first.charAt(0) == '\uFEFF')
			li.set(0, first.substring(1));
		int declarationIndex = -1;
		for (int i = 0; i < li.size(); i++) {
			String line = li.get(i).stripLeading();
			if (line.startsWith("<?xml") && line.length() > 5 && Character.isWhitespace(line.charAt(5))) {
				declarationIndex = i;
				break;
			}
		}
		if (declarationIndex >= 0) {
			// Legacy handbook files put a copyright comment before their XML declaration.
			// The declaration must be the first content in the stream, so retain the
			// historical behavior of discarding that preamble and normalize its encoding.
			if (declarationIndex > 0)
				li.subList(0, declarationIndex).clear();
			li.set(0, getHeader(li.get(0)));
		}
		else {
			li.add(0, getHeader(null));
		}
		in = ReikaFileReader.convertLinesToStream(li, true, StandardCharsets.UTF_8);
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource is = new InputSource(in);
			is.setEncoding(StandardCharsets.UTF_8.name());
			return builder.parse(is);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Could not initialize XML Parser!", e);
		}
	}

	private static String getHeader(String s) {
		return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";
	}

	public static Node getNamedNode(String name, NodeList li) {
		if (name == null || li == null)
			return null;
		for (int i = 0; i < li.getLength(); i++) {
			Node n = li.item(i);
			if (n.getNodeName().equals(name))
				return n;
		}
		return null;
	}

	public static String getNodeNameTree(Node n) {
		StringBuilder sb = new StringBuilder();
		List<Node> parents = new ArrayList<Node>();
		Node p;
		Node c;
		c = n;
		while ((p = c.getParentNode()) != null) {
			if (p.getNodeType() == Node.ELEMENT_NODE) {
				parents.add(p);
			}
			c = p;
		}
		for (int i = parents.size() - 1; i >= 0; i--) {
			sb.append(parents.get(i).getNodeName());
			if (i > 0)
				sb.append(":");
		}
		//sb.append(n.getNodeName());
		return sb.toString();
	}

}
