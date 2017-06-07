/*
 *  uRDF Store MQTT Advisor
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
package com.siemens.ct.urdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.siemens.ct.urdf.exi.Converter;

public class Advisor implements IMqttMessageListener {

	private static final String MAIN_TOPIC = "urdf/amaa/";
	
	private final MqttClient mClient;
	
	public Advisor(String uri) throws MqttException {
		mClient = new MqttClient(uri, "advisor");
	}
	
	public void connect() throws MqttSecurityException, MqttException {
		mClient.connect();
		mClient.subscribe(MAIN_TOPIC + "question", this);
		mClient.subscribe(MAIN_TOPIC + "urdf", this);
	}
	
	public void disconnect() throws MqttException {
		mClient.disconnect();
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			System.out.println(String.format("%s <- %s", topic, message.getPayload()));
			
			ByteArrayInputStream in = new ByteArrayInputStream(message.getPayload());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			String forwardTopic = MAIN_TOPIC;
			if (topic.contains("question")) {
				Converter.toEXI(in, out);
				forwardTopic += "usparql";
			} else if (topic.contains("urdf")) {
				Converter.toXML(in, out);
				forwardTopic += "answer";
				
				String rdf = out.toString();
				rdf = rdf.replaceAll("xsi:type=\"[^\"]*\"", "");
				in = new ByteArrayInputStream(rdf.getBytes());
				
				Model m = ModelFactory.createDefaultModel();
				m.read(in, "http://amaa.me/", "RDF/XML");
				String datalog = toDatalog(m);
				out = new ByteArrayOutputStream();
				out.write(datalog.getBytes());
			}
	
			MqttMessage forwardMessage = new MqttMessage(out.toByteArray());
			mClient.publish(forwardTopic, forwardMessage);
	
			System.out.println(String.format("%s -> %s", forwardTopic, forwardMessage.getPayload()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String toDatalog(Model m) {
		StringBuilder builder = new StringBuilder();
		
		StmtIterator it = m.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();
			String indivName = stmt.getSubject().getURI();
			if (stmt.getPredicate().equals(RDF.type)) {
				String className = stmt.getObject().asResource().getLocalName();
				builder.append(className + "(" + indivName + ")");
			} else {
				String propName = stmt.getPredicate().getLocalName();
				String valName = "_";
				if (stmt.getObject().isURIResource()) {
					valName = stmt.getObject().asResource().getURI();
				} else if (stmt.getObject().isLiteral()) {
					valName = stmt.getObject().asLiteral().getLexicalForm();
				}
				builder.append(propName + "(" + indivName + ", " + valName + ")");
			}
			if (it.hasNext()) {
				builder.append(", ");
			}
		}
		
		return builder.toString();
	}

	public static void main(String[] args) throws Exception {
		Advisor advisor = new Advisor("tcp://localhost:1883");
		advisor.connect();
		
		System.out.println("Press <Enter> to quit...");
		System.in.read();
		
		advisor.disconnect();
	}

}
