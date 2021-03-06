package com.futureplatforms.kirin.gwt.client.delegates.xml;

import com.futureplatforms.kirin.dependencies.xml.XMLException;
import com.futureplatforms.kirin.dependencies.xml.parser.Document;
import com.futureplatforms.kirin.dependencies.xml.parser.XMLParser;

public class GwtXMLParserImpl implements XMLParser {
	@Override
	public Document parse(String contents) throws XMLException {
		try {
			return new GwtDocumentImpl(com.google.gwt.xml.client.XMLParser.parse(contents));
		} catch (Exception e) {
			throw new XMLException(e);
		}
	}
}
