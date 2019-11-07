/*
 * Copyright (C) 2016 Dienst voor het kadaster en de openbare registers
 * 
 * This file is part of Imvertor.
 *
 * Imvertor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imvertor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Imvertor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.imvertor.YamlCompiler;

import org.apache.log4j.Logger;

import nl.imvertor.common.Step;
import nl.imvertor.common.Transformer;
import nl.imvertor.common.file.AnyFile;
import nl.imvertor.common.file.AnyFolder;
import nl.imvertor.common.file.JsonFile;
import nl.imvertor.common.file.XmlFile;
import nl.imvertor.common.file.YamlFile;
import nl.imvertor.common.xsl.extensions.ImvertorStripAccents;

public class YamlCompiler extends Step {

	protected static final Logger logger = Logger.getLogger(YamlCompiler.class);
	
	public static final String STEP_NAME = "YAMLCompiler";
	public static final String VC_IDENTIFIER = "$Id: YamlCompiler.java 7509 2016-04-25 13:30:29Z arjan $";

	/**
	 *  run the main translation
	 */
	public boolean run() throws Exception{
		
		// set up the configuration for this step
		configurator.setActiveStepName(STEP_NAME);
		prepare();
		
		String jsonschemarules = configurator.getJsonSchemarules();
	    if (jsonschemarules.equals("JSON-KINGBSM")) {
			generateKING();
		} else if (jsonschemarules.equals("JSON-IHWBSM")) {
			generateKING();
		} else
			runner.error(logger,"Schemarules not implemented: " + jsonschemarules);
		
		configurator.setStepDone(STEP_NAME);
		
		// save any changes to the work configuration for report and future steps
	    configurator.save();
	    
	    report();
	    
	    return runner.succeeds();
	}

	/**
	 * Generate YAML and Json based on intermediate EP file.
	 * 
	 * See https://code.google.com/archive/p/yamlbeans/ for validator
	 * 
	 * @throws Exception
	 */
	public boolean generateKING() throws Exception {
		
		// create a transformer
		Transformer transformer = new Transformer();
		// requires accent stripper
		transformer.setExtensionFunction(new ImvertorStripAccents());
						
		boolean succeeds = true;
		
		// Create the folder; it is not expected to exist yet.
		AnyFolder yamlFolder = new AnyFolder(configurator.getXParm("system/work-yaml-folder-path"));
		yamlFolder.mkdirs();
				
		AnyFolder yamlApplicationFolder = new AnyFolder(configurator.getXParm("properties/RESULT_YAML_APPLICATION_FOLDER"));
		yamlApplicationFolder.mkdirs();
		
		configurator.setXParm("system/yaml-folder-path", yamlApplicationFolder.toURI().toString());
	
		runner.debug(logger,"CHAIN","Generating YAML to " + yamlApplicationFolder);
		
		// the file that holds all usable model information as an Imvert XML.
		transformer.setXslParm("processable-base-file",configurator.getXParm("properties/WORK_EMBELLISH_FILE"));
		
		succeeds = succeeds && transformer.transformStep("properties/WORK_EMBELLISH_FILE","properties/ROUGH_OPENAPI_ENDPRODUCT_XML_FILE_PATH", "properties/IMVERTOR_METAMODEL_KINGBSM_ROUGH_OPENAPI_ENDPRODUCT_XML_XSLPATH");

		transformer.setXslParm("json-version","3.0");
		succeeds = succeeds && transformer.transformStep("properties/ROUGH_OPENAPI_ENDPRODUCT_XML_FILE_PATH","properties/RESULT_OPENAPI_ENDPRODUCT_XML_FILE_PATH", "properties/IMVERTOR_METAMODEL_KINGBSM_OPENAPI_ENDPRODUCT_XML_XSLPATH");

		succeeds = succeeds && transformer.transformStep("properties/RESULT_OPENAPI_ENDPRODUCT_XML_FILE_PATH","properties/RESULT_YAMLHEADER_FILE_PATH", "properties/IMVERTOR_METAMODEL_KING_YAMLHEADER_XSLPATH");
		succeeds = succeeds && transformer.transformStep("properties/RESULT_OPENAPI_ENDPRODUCT_XML_FILE_PATH","properties/RESULT_YAMLBODY_FILE_PATH", "properties/IMVERTOR_METAMODEL_KING_YAMLBODY_XSLPATH");

		transformer.setXslParm("json-version","2.0");
		succeeds = succeeds && transformer.transformStep("properties/RESULT_OPENAPI_ENDPRODUCT_XML_FILE_PATH","properties/RESULT_YAMLBODY_FILE_PATH2", "properties/IMVERTOR_METAMODEL_KING_YAMLBODY_XSLPATH");
		
		if (succeeds) {
			// concatenate
			YamlFile headerFile = new YamlFile(configurator.getXParm("properties/RESULT_YAMLHEADER_FILE_PATH"));
			JsonFile bodyFile = new JsonFile(configurator.getXParm("properties/RESULT_YAMLBODY_FILE_PATH"));
					 //bodyFile.prettyPrint();
			YamlFile yamlFile = new YamlFile(configurator.getXParm("properties/RESULT_YAML_FILE_PATH"));
			JsonFile bodyFile2 = new JsonFile(configurator.getXParm("properties/RESULT_YAMLBODY_FILE_PATH2"));
				 	 //bodyFile2.prettyPrint();
			
			// validate
			String hc = headerFile.getContent();
			succeeds = succeeds && YamlFile.validate(configurator, hc);
			succeeds = succeeds && bodyFile.convertToYaml(configurator, yamlFile);
			String bc = yamlFile.getContent();
			
			// in all cases copy results to app folder
			yamlFile.setContent(hc + "\n" + bc);
		
			//String schemaName = configurator.getXParm("appinfo/OpenAPI-schema-name");
		
			// copy to the app folder
			AnyFile appYamlFile = new AnyFile(yamlFolder,"openapi.yaml");
			AnyFile appJsonFile = new AnyFile(yamlFolder,"openapi.json");
			AnyFile appJson2File = new AnyFile(yamlFolder,"openapi_draft04.json");
			yamlFile.copyFile(appYamlFile);
			bodyFile.copyFile(appJsonFile);
			bodyFile2.copyFile(appJson2File);
		} 
		configurator.setXParm("system/yaml-created",succeeds);
		
		return succeeds;
	}
	
	/**
	 * Generate Json based on intermediate EP file.
	 * 
	 * See https://code.google.com/archive/p/yamlbeans/ for validator
	 * 
	 * @throws Exception
	 */
	public boolean generateKadaster() throws Exception {
		
		// create a transformer
		Transformer transformer = new Transformer();
		// requires accent stripper
		transformer.setExtensionFunction(new ImvertorStripAccents());
						
		boolean succeeds = true;
		
		// Create the folder; it is not expected to exist yet.
		AnyFolder yamlFolder = new AnyFolder(configurator.getXParm("system/work-yaml-folder-path"));
		yamlFolder.mkdirs();
				
		configurator.setXParm("system/yaml-folder-path", yamlFolder.toURI().toString());
	
		runner.debug(logger,"CHAIN","Generating YAML to " + yamlFolder);
		
		// Migrate between models. This is a stub stylesheet, which transforms any metamodel to the StUF defined & required metamodel
		succeeds = succeeds && transformer.transformStep("properties/WORK_EMBELLISH_FILE","properties/RESULT_METAMODEL_KINGBSM_OPENAPI_MIGRATE", "properties/IMVERTOR_METAMODEL_KINGBSM_OPENAPI_MIGRATE_XSLPATH");
		
		// now add stubs for messaging, that is NOT part of the Kadaster requirements 
		succeeds = succeeds && transformer.transformStep("properties/RESULT_METAMODEL_KINGBSM_OPENAPI_MIGRATE","properties/RESULT_METAMODEL_NEN3610_BSM_MIGRATE", "properties/IMVERTOR_METAMODEL_NEN3610_BSM_MIGRATE_XSLPATH");
		
		// now generated EP; pas the file that holds all usable model information as an Imvert XML.
		transformer.setXslParm("processable-base-file",configurator.getXParm("properties/RESULT_METAMODEL_NEN3610_BSM_MIGRATE"));
		succeeds = succeeds && transformer.transformStep("properties/RESULT_METAMODEL_NEN3610_BSM_MIGRATE","properties/ROUGH_OPENAPI_ENDPRODUCT_XML_FILE_PATH", "properties/IMVERTOR_METAMODEL_KINGBSM_ROUGH_OPENAPI_ENDPRODUCT_XML_XSLPATH");
		succeeds = succeeds && transformer.transformStep("properties/ROUGH_OPENAPI_ENDPRODUCT_XML_FILE_PATH","properties/RESULT_OPENAPI_ENDPRODUCT_XML_FILE_PATH", "properties/IMVERTOR_METAMODEL_KINGBSM_OPENAPI_ENDPRODUCT_XML_XSLPATH");
		
		transformer.setXslParm("json-version","3.0");
		succeeds = succeeds && transformer.transformStep("properties/RESULT_OPENAPI_ENDPRODUCT_XML_FILE_PATH","properties/RESULT_YAMLBODY_FILE_PATH", "properties/IMVERTOR_METAMODEL_KING_YAMLBODY_XSLPATH");

		JsonFile halJsonFile = new JsonFile(configurator.getXParm("properties/RESULT_YAMLBODY_FILE_PATH"));
		// Debug: test if json is okay
		succeeds = succeeds && halJsonFile.validate(configurator);
		
		// STUB: transform json to XML, remove HAL, and serialize back to json.
		if (succeeds) {
			halJsonFile.prettyPrint();
			
			XmlFile  halXmlFile = new XmlFile(configurator.getXParm("properties/WORK_JSON_HAL_FILE_PATH"));
			XmlFile nohalXmlFile = new XmlFile(configurator.getXParm("properties/WORK_JSON_NOHAL_FILE_PATH"));
			
			halJsonFile.jsonToXml(halXmlFile);
			
			succeeds = succeeds && transformer.transformStep("properties/WORK_JSON_HAL_FILE_PATH","properties/WORK_JSON_NOHAL_FILE_PATH", "properties/IMVERTOR_JSON_NOHAL_XSLPATH");
		
			JsonFile jsonFile = new JsonFile(configurator.getXParm("properties/RESULT_JSON_NOHAL_FILE_PATH"));
			nohalXmlFile.xmlToJson(jsonFile);
			jsonFile.prettyPrint();
			
			// strip the root
			jsonFile.stripJSONroot();
			
			// validate
			succeeds = succeeds && jsonFile.validate(configurator);
		
			String schemaName = configurator.getXParm("appinfo/OpenAPI-schema-name");
		
			// copy to the app folder
			AnyFile appJsonFile = new AnyFile(yamlFolder,schemaName + ".json");
			jsonFile.copyFile(appJsonFile);
		}
		configurator.setXParm("system/json-created",succeeds);
		
		return succeeds;
	}
	
}
