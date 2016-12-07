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
package org.dkpro.meta.core.model;

public class EngineModel
{
    String name;
    String implName;
    String groupId;
    String artifactId;
    String version;
    String module;
    def pom;
    def spec;
    String role;
    String tool;
    List models = [];
    List<String> inputs = [];
    List<String> outputs = [];
    List<String> languages = [];
    List<String> allLanguages = [];
    
    @Override
    public String toString()
    {
        return name;
    }
}
