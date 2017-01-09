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
package org.dkpro.meta.core;

import static org.dkpro.meta.vocab.ToolCategories.roleNames;
import static groovy.io.FileType.FILES;
import groovy.json.*;
import groovy.text.XmlTemplateEngine;
import groovy.transform.Field;
import groovy.util.XmlParser;
import org.dkpro.meta.core.maven.ContextHolder
import org.dkpro.meta.core.model.FormatModel;
import org.dkpro.meta.core.model.EngineModel;
import org.dkpro.meta.core.model.MetadataModel
import org.dkpro.meta.vocab.ToolCategories;

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

class MetadataAggregator {
    def loadTypeSystemMapping(File aDirectory) {
      return new File(aDirectory, "dkpro-core-doc/src/main/script/mappings/typesystemmapping.yaml").withInputStream { 
        new Yaml().load(it) };
    }
    
    private File locatePom(path) {
        def pom = new File(path, "pom.xml");
        if (pom.exists()) {
            return pom;
        }
        else if (path.getParentFile() != null) {
            return locatePom(path.getParentFile());
        }
        else {
            return null;
        }
    }
    
    def addFormat(Map<String, FormatModel> aTarget, aFormat, aKind, aPom, aSpec, aClazz) {
        if (!aTarget[aFormat]) {
            aTarget[aFormat] = new FormatModel();
            aTarget[aFormat].with {
                name = aFormat;
                pom = aPom;
                groupId = aPom.groupId ? aPom.groupId.text() : aPom.parent.groupId.text();
                artifactId = aPom.artifactId.text();
                version = aPom.version ? aPom.version.text() : aPom.parent.version.text();
            }
        }
        aTarget[aFormat][aKind+'Class'] = aClazz;
        aTarget[aFormat][aKind+'Spec'] = aSpec;
    }
    
    /**
     * Get a short tool type identifier for the given component. This may be used to resolve tagset
     * mappings, model identifiers, etc.
     */
    private String getTool(componentName, spec) {
        def outputs = spec.analysisEngineMetaData?.capabilities?.collect { 
            it.outputs?.collect { it.name } }.flatten().sort().unique()
        
        def baseComponentName = componentName.endsWith("Trainer") ? 
            componentName[0..-8] : componentName;
            
        switch (baseComponentName) {
        case { 'de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain' in outputs }: 
            return ToolCategories.COREF;
        case { 'de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity' in outputs }: 
            return ToolCategories.NER;
        case { 'de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.GrammarAnomaly' in outputs ||
               'de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly' in outputs }: 
            return ToolCategories.CHECKER;
        case { 'de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures' in outputs }: 
            return ToolCategories.MORPH;
        case { 'de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemanticArgument' in outputs ||
               'de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg' in outputs}: 
            return "srl";
        case { 'de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem' in outputs }: 
            return "stem";
        case { 
                it.endsWith("Transformer") || 
                it.endsWith("Normalizer") ||
                it.endsWith("ApplyChangesAnnotator") ||
                it.endsWith("Backmapper") ||
                spec.annotatorImplementationName.contains('.textnormalizer.transformation.') }: 
            return ToolCategories.TRANSFORMER;
        case { it.endsWith("Chunker") }: 
            return ToolCategories.CHUNKER;
        case { it.endsWith("LanguageIdentifier") || it.contains("LanguageDetector") }: 
            return ToolCategories.LANGDETECT;
        case { it.endsWith("NamedEntityRecognizer") }: 
            return ToolCategories.NER;
        case { it.endsWith("Tagger") }: 
            return ToolCategories.TAGGER;
        case { it.endsWith("Parser") }: 
            return ToolCategories.PARSER;
        case { 
                it.endsWith("Segmenter") || 
                it.endsWith("Tokenizer") ||
                it.endsWith("Sentence") ||
                it.endsWith("Token") ||
                spec.annotatorImplementationName.contains('.tokit.')}: 
            return ToolCategories.SEGMENTER;
        case { it.endsWith("Lemmatizer") }: 
            return ToolCategories.LEMMATIZER;
        case { it.endsWith("PhoneticTranscriptor") }: 
            return ToolCategories.TRANSCRIPTOR;
        case { it.contains("TopicModel") }:
            return ToolCategories.TOPICMODEL;
        case { it.contains("DictionaryAnnotator") }:
            return ToolCategories.GAZETEER;
        case { it.contains("Embeddings") }:
            return ToolCategories.EMBEDDINGS;
        case { it.contains("DependencyConverter") }:
            return ToolCategories.PARSER;
        default:
            return ToolCategories.OTHER;
        }
    }
    
    /**
     * Scan the UIMA type system descriptors.
     */
    private List scanUimaTypeSystemDescriptors(File aDirectory) {
        def ts = []
        aDirectory.eachFileRecurse(FILES) {
            if (
                it.name.endsWith('.xml') &&
                // No testing module
                !it.path.contains('/dkpro-core-testing-asl/') &&
                // For the typesystem descriptors
                it.path.contains('/src/main/resources/')
            ) {
                try {
                    ts << getXMLParser().parseTypeSystemDescription(
                        new XMLInputSource(it.path));
                }
                catch (org.apache.uima.util.InvalidXMLException e) {
                    // Ignore
                }
            }
        }
        return ts;
    }
    
    /**
     * Scan the UIMA component descriptors.
     */
    def scanUimaComponentDescriptors(File aDirectory, Map<String, String> aRoleNames) {
        def docModuleDir = new File(aDirectory, 'de.tudarmstadt.ukp.dkpro.core.doc-asl');
        Map<String, EngineModel> es = [:];
        Map<String, FormatModel> fs = [:];
        aDirectory.eachFileRecurse(FILES) {
            if (
                it.name.endsWith('.xml') &&
                // No testing module
                !it.path.contains('/dkpro-core-testing-asl/') &&
                // For the analysis engine and reader descriptions
                it.path.contains('/target/classes/')
            ) {
                try {
                    def spec = createResourceCreationSpecifier(it.path, null);
                    if (spec instanceof AnalysisEngineDescription) {
                        // println "AE " + it;
                        def implName = spec.annotatorImplementationName;
                        def uniqueName = implName.substring(implName.lastIndexOf('.')+1);
                        def pomFile = locatePom(it);
                        def pom = new XmlParser().parse(pomFile);
                        def module = it.path[docModuleDir.path.length()+4..-1];
                        module = module[0..module.indexOf('/')-1]
        
                        if (!implName.contains('$')) {
                            if (implName.endsWith('Writer')) {
                                def format = uniqueName[0..-7];
                                addFormat(fs, format, 'writer', pom, spec, spec.annotatorImplementationName);
                            }
                            else {
                                EngineModel engine = new EngineModel();
                                engine.name = uniqueName;
                                engine.implName = implName;
                                engine.pom = pom;
                                engine.spec = spec;
                                engine.module = module;
                                engine.role = aRoleNames[getTool(uniqueName, spec)];

                                // Maven information for easy access
                                engine.groupId = pom.groupId ? pom.groupId.text() : pom.parent.groupId.text();
                                engine.artifactId = pom.artifactId.text();
                                engine.version = pom.version ? pom.version.text() : pom.parent.version.text();
                                engine.tool = getTool(uniqueName, spec);
                                
                                // UIMA information for easy access
                                engine.inputs = engine.spec.analysisEngineMetaData?.capabilities?.collect { 
                                        it.inputs?.collect { it.name  } }.flatten().sort().unique();
                                engine.outputs = engine.spec.analysisEngineMetaData?.capabilities?.collect { 
                                        it.outputs?.collect { it.name  } }.flatten().sort().unique();
                                engine.languages = engine.spec.analysisEngineMetaData?.capabilities?.collect { 
                                        it.languagesSupported }.flatten().sort().unique();
                                engine.allLanguages = engine.languages.collect();
                                es[uniqueName] = engine;
                            }
                        }
                    }
                    else if (spec instanceof CollectionReaderDescription) {
                        def implName = spec.implementationName;
                        if (implName.endsWith('Reader') && !implName.contains('$')) {
                            def uniqueName = implName.substring(implName.lastIndexOf('.')+1);
                            def pomFile = locatePom(it);
                            def pom = new XmlParser().parse(pomFile);
                            def format = uniqueName[0..-7];
                            addFormat(fs, format, 'reader', pom, spec, implName);
                        }
                    }
                    else {
                        // println "?? " + it;
                    }
                }
                catch (org.apache.uima.util.InvalidXMLException e) {
                    // Ignore
                }
            }
        }
        return [es, fs];
    }
    
    /**
     * Scan the build.xml files used for packaging models.
     */
    private List scanModelBuilderFiles(File aDirectory, def aEngines) {
        def ms = [];
        aDirectory.eachFileRecurse(FILES) {
            if (it.path.endsWith('/src/scripts/build.xml')) {
                def buildXml = new XmlSlurper().parse(it);
                def modelXmls = buildXml.'**'.findAll{ node -> node.name() in [
                    'install-stub-and-upstream-file', 'install-stub-and-upstream-folder',
                    'install-upstream-file', 'install-upstream-folder', 'install-model-stub' ]};
                
                // Extract package
                def pack = buildXml.'**'.find { it.name() == 'property' && it.@name == 'outputPackage' }.@value as String;
                if (pack.endsWith('/')) {
                    pack = pack[0..-2];
                }
                if (pack.endsWith('/lib')) {
                    pack = pack[0..-5];
                }
                pack = pack.replaceAll('/', '.');
                
                // Auto-generate some additional attributes for convenience!
                modelXmls.each { model ->
                    def shortBase = model.@artifactIdBase.text().tokenize('.')[-1];
                    model.@shortBase = shortBase as String;
                    model.@shortArtifactId = "${shortBase}-model-${model.@tool}-${model.@language}-${model.@variant}" as String;
                    model.@artifactId = "${model.@artifactIdBase}-model-${model.@tool}-${model.@language}-${model.@variant}" as String;
                    model.@package = pack as String;
                    
                    if (model.name() in ['install-model-stub']) {
                        model.@version = "${model.@version}" as String;
                    }
                    else {
                        model.@version = "${model.@upstreamVersion}.${model.@metaDataVersion}" as String;
                    }
                    
                    def engine = aEngines.values()
                        .findAll { engine ->
                            def clazz = engine.spec.annotatorImplementationName;
                            def enginePack = clazz.substring(0, clazz.lastIndexOf('.'));
                            enginePack == pack;
                        }
                        .find { engine ->
                            // There should be only one tool matching here - at least we don't have models
                            // yet that apply to multiple tools... I believe - REC
                            switch (model.@tool as String) {
                            case 'token':
                                return engine.tool == ToolCategories.SEGMENTER;
                            case 'sentence':
                                return engine.tool == ToolCategories.SEGMENTER;
                            // Special handling for langdetect models which use wrong tool designation
                            case 'languageidentifier':
                                return engine.tool == ToolCategories.LANGDETECT;
                            // Special handling for MateTools models which use wrong tool designation
                            case 'morphtagger':
                                return engine.tool == ToolCategories.MORPH;
                            // Special handling for ClearNLP lemmatizer because dictionary is actually
                            // used in multiple places
                            case 'dictionary':
                                return engine.tool == ToolCategories.LEMMATIZER;
                            // Required to handle CoreNLP "depparser" models because depparser component
                            // is categorized as "parser" not as "depparser"
                            case 'depparser':
                                return engine.tool == ToolCategories.PARSER;
                            default:
                                return engine.tool == (model.@tool as String);
                            }
                        };
                    if (engine) {
                        model.@engine = engine.name;
                    }
                    else {
                        ContextHolder.log.warn("No engine found for model ${model.@shortArtifactId}");
                    }
                }
                ms.addAll(modelXmls);
            }
        }
        
        ms = ms.sort { a,b ->
            (a.@language as String) <=> (b.@language as String) ?: 
            (a.@tool as String) <=> (b.@tool as String) ?: 
            (a.@engine as String) <=> (b.@engine as String) ?: 
            (a.@variant as String) <=> (b.@variant as String)  
        }; 
    
        return ms;
    }
    
    /**
     * Scan DKPro Core tagset mappings
     */
    private Map scanTagsetMappings(File aDirectory) {
        def typesystems = [:];
        
        aDirectory.eachFileRecurse(FILES) {
            if (
                it.path.contains('/src/main/resources/') &&
                it.path.contains('/tagset/') &&
                it.path.endsWith('.map') &&
                !it.name.startsWith('TEMPLATE')
            ) {
                def config = new PropertiesConfiguration();
                config.setFile(it);
                config.setEncoding("UTF-8");
                config.setListDelimiter(0 as char);
                config.load();
                
                // Remove .map and split
                def parts = it.name[0..-5].tokenize('-');
        
                // Skip legacy default mappings that were only layer + language.
                if (parts.size <= 2) {
                    return;
                }
                
                def lang = parts[0];
                def name = parts[1..-2].join('-');
                def tool = parts[-1];
                
                // Skip the morphological features mapping for now because the files have completely
                // different semantics from the other mapping files.
                if (tool == ToolCategories.MORPH) {
                    return;
                }
                
                // Fix the currently bad practice of naming mappings for constituent parse types
                if (tool == "constituency") {
                    tool = "constituent"
                }
                
                // Try extracting the long tagset name
                def longName = config.layout.getCanonicalHeaderComment(true);
                if (longName) {
                    def lines = longName.split('\n');
                    if (lines.size() > 0) {
                        longName = lines[0];
                    }
                    
                    if (longName.startsWith('#')) {
                        longName = longName.length() > 1 ? longName[1..-1].trim() : '';
                    }
                }
                
                typesystems["${lang}-${name}-${tool}"] = [
                    id: "${lang}-${name}-${tool}",
                    lang: lang,
                    name: name,
                    longName: longName ?: name,
                    tool: tool,
                    mapping: config,
                    source: it,
                    url: 'https://github.com/dkpro/dkpro-core/edit/master/' +
                        it.canonicalPath[aDirectory.canonicalPath.length()..-1]
                    ];
            }
        }
        
        typesystems = typesystems.sort { a,b ->
            (a.value.tool as String) <=> (b.value.tool as String) ?:
            (a.value.lang as String) <=> (b.value.lang as String) ?:
            (a.value.tagset as String) <=> (b.value.tagset as String)
        };
        
        return typesystems;
    }
    
    /**
     * Scan DKPro Core dataset definitions
     */
    private Map scanDatasets(File aDirectory) {
        def datasets = [:];
        def dsd = 'dkpro-core-api-datasets-asl/src/main/resources/de/tudarmstadt/ukp/dkpro/core/api/datasets/lib';
        new File(aDirectory, dsd).eachFileRecurse(FILES) {
            if (it.path.endsWith('.yaml')) {
              def ds = it.withInputStream { new Yaml().load(it) };
              datasets[FilenameUtils.removeExtension(it.name)] = ds;
              datasets[FilenameUtils.removeExtension(it.name)]['githubUrl'] = 
                'https://github.com/dkpro/dkpro-core/edit/master/' + 
                it.canonicalPath[aDirectory.canonicalPath.length()..-1]
            }
        }
    
        datasets = datasets.sort { a,b ->
            (a.value.name as String) <=> (b.value.name as String)
        };
    
        return datasets;
    }
    
    private List collectInputOutputTypes(aEngines) {
        def inputOutputTypes = [];
        aEngines.each {
            it.value.spec.analysisEngineMetaData?.capabilities?.each { capability ->
                capability?.inputs.each { inputOutputTypes << it.name};
                capability?.outputs.each { inputOutputTypes << it.name};
            }
        }
        inputOutputTypes = inputOutputTypes.sort().unique();
        return inputOutputTypes;
    }
    
    private void linkEnginesAndModels(MetadataModel aModel)
    {
        aModel.models.each { model ->
            def engine = model.@engine as String;
            if (engine && aModel.engines[engine]) {
                if (!aModel.engines[engine].models) {
                    aModel.engines[engine].models = []
                }
                aModel.engines[engine].models.add(model)
                aModel.engines[engine].allLanguages << (model.@language as String);
                aModel.engines[engine].allLanguages = aModel.engines[engine].allLanguages.unique().sort();
            }
        }
        
        // aModel.engines.each { key, engine ->
        //     ContextHolder.log.info("${engine} -> ${engine?.allLanguages.size()}");
        // }
    }
    
    public MetadataModel build(File dkproCorePath) {
        MetadataModel model = new MetadataModel();
        
        ContextHolder.log.info("Running DKPro Core metadata processor ${dkproCorePath}...")
        
        model.typesystems = scanUimaTypeSystemDescriptors(dkproCorePath);
        ContextHolder.log.info("Found ${model.typesystems.size()} typesystems");
        
        def (engines, formats) = scanUimaComponentDescriptors(dkproCorePath, roleNames);
        model.engines = engines;
        model.formats = formats;
        ContextHolder.log.info("Found ${model.engines.size()} components");
        ContextHolder.log.info("Found ${model.formats.size()} formats");
        
        model.models = scanModelBuilderFiles(dkproCorePath, engines);
        ContextHolder.log.info("Found ${model.models.size()} models");
        
        linkEnginesAndModels(model);
        
        model.tagsets = scanTagsetMappings(dkproCorePath);
        ContextHolder.log.info("Found ${model.tagsets.size()} tagsets");
        
        model.inputOutputTypes = collectInputOutputTypes(engines);
        ContextHolder.log.info("Found ${model.inputOutputTypes.size()} input/output types");
        
        model.typesystemMappings = loadTypeSystemMapping(dkproCorePath);
        ContextHolder.log.info("Found ${model.typesystemMappings.size()} type mappings");
        
        model.datasets = scanDatasets(dkproCorePath);
        ContextHolder.log.info("Found ${model.datasets.size()} datasets");
        
        return model;
    }
}