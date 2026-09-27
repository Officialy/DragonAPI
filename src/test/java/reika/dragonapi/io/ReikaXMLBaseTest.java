package reika.dragonapi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class ReikaXMLBaseTest {

	@Test
	void acceptsLegacyPreambleBeforeDeclaration() throws Exception {
		String xml = """
				<!--
				  Copyright 2018
				  All rights reserved.
				-->
				<?xml version="1.0"?>
				<categories><info>Loaded</info></categories>
				""";

		assertEquals("categories", ReikaXMLBase.getXMLDocument(stream(xml)).getDocumentElement().getNodeName());
	}

	@Test
	void acceptsDeclarationFreeDocument() throws Exception {
		assertEquals("root", ReikaXMLBase.getXMLDocument(stream("<root/>")).getDocumentElement().getNodeName());
	}

	@Test
	void rejectsEmptyDocumentClearly() {
		assertThrows(SAXException.class, () -> ReikaXMLBase.getXMLDocument(stream("")));
	}

	private static ByteArrayInputStream stream(String data) {
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}

}
