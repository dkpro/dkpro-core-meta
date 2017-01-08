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
@Grab('org.dkpro.script:dkpro-script-groovy:0.1.0')
import groovy.transform.BaseScript
import org.dkpro.script.groovy.DKProCoreScript;
@BaseScript DKProCoreScript baseScript

version '${version}'

def input = args[0]
def output = args[1]
def paramList = [:];

println "${type}"
println args;

if (args.length < 3){
	println "Not enough params";
	exit();
}
for (pos = 3; pos < args.length; pos += 2) {
	def key = args[pos].replace("-","");
	if(args[pos+1] == "true"){
		paramList[key] = true
	}else {
		if(args[pos+1] == "false"){
			paramList[key] = false
		}else{
			paramList[key] = "\""+args[pos+1]+"\"";
		}
	}
}

<%
if(type=="reader"){
%>
read '${format.name}' from input params(paramList)

write 'Xmi' to output params([
	overwrite: true])
<%	
}else{
%>
read 'Xmi' from input

write '${format.name}' to output params(paramList)

<%
}

%>