/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dkpro.meta.example.galaxy;

import static groovy.io.FileType.FILES;
import groovy.io.FileType;
import groovy.json.*;
import groovy.text.XmlTemplateEngine
import groovy.transform.Field
import groovy.util.XmlParser
import java.nio.file.Files
import org.dkpro.meta.core.MetadataAggregator;
import org.dkpro.meta.core.model.MetadataModel;

import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.fit.factory.ResourceCreationSpecifierFactory.*;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.yaml.snakeyaml.Yaml;

class GalaxyToolBuilder {
    public static void main(String... args) {
        String dkproCoreDir = args[0];
        String targetDir = args[1];        
        
        new File(targetDir).mkdirs();
        
        File dkproCorePath = new File(dkproCoreDir);
        
        MetadataModel model = new MetadataAggregator().build(dkproCorePath);
            
        def xmlEngine = new groovy.text.XmlTemplateEngine(
            new XmlParser(false, true), this.class.classLoader);
        def toolTemplateEngine = xmlEngine.createTemplate(
            new File("src/main/resources/templates/toolEngine.xml").getText("UTF-8"));
		def toolTemplateFormat = xmlEngine.createTemplate(
			new File("src/main/resources/templates/toolFormat.xml").getText("UTF-8"));
		
        def textTemplate = new groovy.text.SimpleTemplateEngine(this.class.classLoader);
        def wrapperTemplateEngine = textTemplate.createTemplate(
            new File("src/main/resources/templates/wrapperEngine.groovy").getText("UTF-8"));
		def wrapperTemplateFormat= textTemplate.createTemplate(
			new File("src/main/resources/templates/wrapperFormat.groovy").getText("UTF-8"));
		
		def toolfileTemplate = xmlEngine.createTemplate(new File("src/main/resources/templates/toolFile.xml").getText("UTF-8"));
		
        model.engines.each { key, engine ->
            println "Processing Engine: ${engine.name}...";			
            def templateBinding = [
                engine: engine,
                engines: model.engines,
				version: engine.version,               
                models: model.models,
                datasets: model.datasets,
                tagsets: model.tagsets,
                typesystems: model.typesystems,
                typesystemMappings: model.typesystemMappings,
                inputOutputTypes: model.inputOutputTypes];

            def toolResult = toolTemplateEngine.make(templateBinding);
            def toolOutput = new File("${targetDir}/engines/${engine.name}.xml");
            toolOutput.parentFile.mkdirs();
            toolOutput.setText(toolResult.toString(), 'UTF-8');
            
            def wrapperResult = wrapperTemplateEngine.make(templateBinding);
            def wrapperOutput = new File("${targetDir}/engines/${engine.name}.groovy");
            wrapperOutput.parentFile.mkdirs();
            wrapperOutput.setText(wrapperResult.toString(), 'UTF-8');
        }
		
		model.formats.each { key, format ->
			println "Processing Reader ${format.name}...";						
//			format.readerSpec.metaData.configurationParameterDeclarations.configurationParameters.sort { println it.name }
			// check if metadata for both reader and writer 
			if(format.readerSpec){
				def templateBindingReader = [
					format: format,
					type: "reader",
					formats: model.formats,
					version: format.version,
					models: model.models,
					datasets: model.datasets,
					tagsets: model.tagsets,
					typesystems: model.typesystems,
					typesystemMappings: model.typesystemMappings,
					inputOutputTypes: model.inputOutputTypes];
				def toolResultReader = toolTemplateFormat.make(templateBindingReader);
				def toolOutputReader = new File("${targetDir}/readers/${format.name}.xml");
				toolOutputReader.parentFile.mkdirs();
				toolOutputReader.setText(toolResultReader.toString(), 'UTF-8');
				def wrapperResultReader = wrapperTemplateFormat.make(templateBindingReader);
				def wrapperOutputReader = new File("${targetDir}/readers/${format.name}.groovy");
				wrapperOutputReader.parentFile.mkdirs();
				wrapperOutputReader.setText(wrapperResultReader.toString(), 'UTF-8');
				
			}	
			
			if(format.writerSpec){
				def templateBindingWriter = [
					format: format,
					type: "writer",
					formats: model.formats,
					version: format.version,
					models: model.models,
					datasets: model.datasets,
					tagsets: model.tagsets,
					typesystems: model.typesystems,
					typesystemMappings: model.typesystemMappings,
					inputOutputTypes: model.inputOutputTypes];

				def toolResultWriter = toolTemplateFormat.make(templateBindingWriter);
				def toolOutputWriter = new File("${targetDir}/writers/${format.name}.xml");
				toolOutputWriter.parentFile.mkdirs();
				toolOutputWriter.setText(toolResultWriter.toString(), 'UTF-8');
				
				def wrapperResultWriter = wrapperTemplateFormat.make(templateBindingWriter);
				def wrapperOutputWriter = new File("${targetDir}/writers/${format.name}.groovy");
				wrapperOutputWriter.parentFile.mkdirs();
				wrapperOutputWriter.setText(wrapperResultWriter.toString(), 'UTF-8');
			}										
			
		}
		def engineFiles = []
		def readerFiles = []
		def writerFiles = []
		
		def dir = new File(targetDir)
		dir.eachFileRecurse (FileType.FILES) { file ->
			if(file.name.endsWith('.xml') && file.absolutePath.contains("engines")){
				engineFiles << file
			}
			if(file.name.endsWith('.xml') && file.absolutePath.contains("readers")){
				readerFiles << file
			}
			if(file.name.endsWith('.xml') && file.absolutePath.contains("writers")){
				writerFiles << file
			}
		}
		def templateBindingTool = [
			path: dir.absoluteFile,
			writerFiles :writerFiles,
			readerFiles: readerFiles,
			engineFiles: engineFiles];
		def toolResult = toolfileTemplate.make(templateBindingTool);
		def toolOutputReader = new File("${targetDir}/my_tools.xml");
		toolOutputReader.parentFile.mkdirs();
		toolOutputReader.setText(toolResult.toString(), 'UTF-8');
		
//		<?xml version='1.0' encoding='utf-8'?>
//		<toolbox monitor="true">
//		  <section id="DKPro" name="DKPro Components">
//			<tool file="/local_tools/dkpro/AnnotationByTextFilter.xml" />
//			<tool file="/local_tools/dkpro/BerkeleyParser.xml" />
//			<tool file="/local_tools/dkpro/StanfordSegmenter.xml" />
//		 </section>
//		</toolbox>
		
		
    }
}