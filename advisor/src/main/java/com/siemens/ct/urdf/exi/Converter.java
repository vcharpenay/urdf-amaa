/*
 *  uRDF Store MQTT Advisor (EXI Converter)
 *  Copyright (C) 2017  Victor Charpenay (victor.charpenay@siemens.com)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.siemens.ct.urdf.exi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.FidelityOptions;
import com.siemens.ct.exi.GrammarFactory;
import com.siemens.ct.exi.api.sax.EXIResult;
import com.siemens.ct.exi.api.sax.EXISource;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.helpers.DefaultEXIFactory;

public class Converter {
	
	protected static final EXIFactory EXI_FACTORY = DefaultEXIFactory.newInstance();
	protected static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

	static {
		GrammarFactory factory = GrammarFactory.newInstance();
		InputStream schema = ClassLoader.getSystemResourceAsStream("basic_rdf_query_v02.xsd");
		// TODO clean id resolution
		try {
			EXI_FACTORY.setGrammars(factory.createGrammars(schema, new XMLEntityResolver() {
				public XMLInputSource resolveEntity(XMLResourceIdentifier id) throws XNIException, IOException {
					if (id.getNamespace().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#")) {
						InputStream is = ClassLoader.getSystemResourceAsStream("basic_rdf_for_exi_v03.xsd");
						return new XMLInputSource("", "", "", is, "UTF-8");
					}
					return null;
				}
			}));
			
			FidelityOptions opts = FidelityOptions.createStrict();
			EXI_FACTORY.setFidelityOptions(opts);
			EXI_FACTORY.setCodingMode(CodingMode.BIT_PACKED);
			
			// EXI Profile suggested configuration
			EXI_FACTORY.setLocalValuePartitions(false);
			EXI_FACTORY.setMaximumNumberOfBuiltInElementGrammars(0);
			EXI_FACTORY.setMaximumNumberOfBuiltInProductions(0);
			EXI_FACTORY.setValuePartitionCapacity(0);
			EXI_FACTORY.setValueMaxLength(-1);
		} catch (EXIException e) {
			throw new RuntimeException(e);
		}
	}

	public static void toEXI(InputStream xml, OutputStream exi) throws EXIException, IOException, SAXException {
		EXIResult result = new EXIResult(EXI_FACTORY);
		result.setOutputStream(exi);

		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setContentHandler(result.getHandler());
		reader.parse(new InputSource(xml));
	}
	
	public static void toXML(ByteArrayInputStream exi, OutputStream xml) throws EXIException, TransformerConfigurationException, TransformerException {
		SAXSource source = new EXISource(EXI_FACTORY);
		source.setInputSource(new InputSource(exi));
		Result sink = new StreamResult(xml);
		TRANSFORMER_FACTORY.newTransformer().transform(source, sink);
	}
	
}
