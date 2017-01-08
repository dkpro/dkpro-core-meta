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
package org.dkpro.meta.example.galaxy

import static org.junit.Assert.*

import groovy.io.FileType

import org.codehaus.groovy.control.CompilerConfiguration
import org.dkpro.script.groovy.DKProCoreScript
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

class RunScript {
    private static String oldModelCache
    private static String oldGrapeCache
    
    @BeforeClass
    public static void before()
    {
        System.setProperty("groovy.grape.report.downloads", "true")
        oldModelCache = System.setProperty("dkpro.model.repository.cache", 
            "target/test-output/models")
        //oldGrapeCache = System.setProperty("grape.root", "target/test-output/grapes");
    }

    @AfterClass
    public static void after()
    {
        if (oldModelCache != null) {
            System.setProperty("dkpro.model.repository.cache", oldModelCache)
        }
        else {
            System.getProperties().remove("dkpro.model.repository.cache")
        }
        if (oldGrapeCache != null) {
            System.setProperty("grape.root", oldGrapeCache)
        }
        else {
            System.getProperties().remove("grape.root")
        }
    }
    
    @Test
    public void testTextReader()
    {
        run("target/galaxy/readers/Text.groovy", false, 
            "src/test/resources/text1.txt", "target/test-output/output.txt", "false", 
            "-logFreq", "1");
    }

    public void run(String aName, boolean aCaptureStdOut, String... aArgs) {
        PrintStream originalOut
        ByteArrayOutputStream capturedOut
        if (aCaptureStdOut) {
            // System.err.println "Capturing stdout...";
            originalOut = System.out
            capturedOut = new ByteArrayOutputStream()
            System.setOut(new PrintStream(capturedOut))
        }
        
        boolean error = true
        try {
            CompilerConfiguration cc = new CompilerConfiguration()
            cc.setScriptBaseClass(DKProCoreScript.name)
    
            File scriptFile = new File(aName);
            
            // Create a GroovyClassLoader explicitly here so that Grape can work in the script
            GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader(), cc)
            GroovyScriptEngine engine = new GroovyScriptEngine(scriptFile.path, gcl)
            engine.setConfig(cc)
            
            Binding binding = new Binding()
            binding.setVariable("args", aArgs)
            Script script = engine.createScript(scriptFile.name, binding)
            script.run()
            error = false
        }
        finally {
            if (aCaptureStdOut) {
                // System.err.println "Capturing complete.";
                System.setOut(originalOut)
            }
            if (capturedOut != null && error) {
                System.out.println(capturedOut.toString('UTF-8'))
            }
        }
    }
}
