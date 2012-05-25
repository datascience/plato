/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.scape_project.pw.planning.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import eu.scape_project.planning.xml.SchemaResolver;
import eu.scape_project.planning.xml.StrictDefaultHandler;
import eu.scape_project.planning.xml.ValidatingParserFactory;

public class SchemaValidationTest {
	
	private ValidatingParserFactory validatingParserFactory = new ValidatingParserFactory(); 


	protected SAXParser getNonValidatingParser() throws ParserConfigurationException, SAXException {
		SAXParserFactory f = SAXParserFactory.newInstance();
		return f.newSAXParser();
	}
	
	@Test
	public void parsePlanWithoutValidation() throws ParserConfigurationException, SAXException, IOException {
		InputStream inPlan = getClass().getClassLoader().getResourceAsStream("plans/Archiving_Digital_Photographs.xml");
		SAXParser parser = getNonValidatingParser();
		parser.parse(inPlan, new DefaultHandler());
	}
	
	@Test(expected=SAXException.class)
	public void parseNonWellformedXml() throws ParserConfigurationException, SAXException, IOException {
		SAXParser parser = getNonValidatingParser();
		
		parser.parse(new InputSource(new StringReader("<test><open></test>")), new DefaultHandler());
	}
	
	@Test(expected=SAXException.class)
	public void parseXmlInvalidOrder() throws ParserConfigurationException, SAXException, IOException {
		InputStream inPlan = getClass().getClassLoader().getResourceAsStream("simple/simpleInvalidOrder.xml");
		
		SAXParser parser = validatingParserFactory.getValidatingParser();
		//parser.setProperty(JAXP_SCHEMA_SOURCE, "file:///home/kraxner/workspace/planningsuite/planning-core/src/test/resources/simple/simple.xsd");
		
		parser.parse(inPlan, new StrictDefaultHandler(new SchemaResolver().addSchemaLocation("http://simple.org/simple/V1.0.0/simple.xsd", "simple/simple.xsd")));
	}
	
	@Test
	public void parseValidXml() throws ParserConfigurationException, SAXException, IOException {
		InputStream inPlan = getClass().getClassLoader().getResourceAsStream("simple/simple.xml");
		
		SAXParser parser = validatingParserFactory.getValidatingParser();
		parser.setProperty(ValidatingParserFactory.JAXP_SCHEMA_SOURCE, "http://simple.org/simple/V1.0.0/simple.xsd");
		parser.parse(inPlan, new StrictDefaultHandler(new SchemaResolver().addSchemaLocation("http://simple.org/simple/V1.0.0/simple.xsd", "simple/simple.xsd")));
	}
	

}
