/**
 *  // MR450DXT-Source.groovy
 *
 *  Copyright 2025 VH 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *            --- Driver para AMCP 4.5 - Source
 *           v.1  30/04/2025 - BETA. 
 *
 */
metadata {
    definition (
        name: "MR450DXT-Source",
        namespace: "TRATO",
        author: "VH"
    ) {
        capability "Actuator"
        
        attribute "sourceId", "number"
    }
    
    preferences {
        input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() {
    log.info "MR450DXT Source installed"
}

def updated() {
    log.info "MR450DXT Source updated"
}

def parse(String description) {
    if (debugLogging) log.debug "Parse: ${description}"
}

def setSourceId(id) {
    // Allow source IDs 1-6
    state.sourceId = Math.max(1, Math.min(6, id.toInteger()))
    sendEvent(name: "sourceId", value: state.sourceId)
}

def getSourceId() {
    return state.sourceId ?: 1
}
