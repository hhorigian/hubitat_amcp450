/**
 *  // MR450DXT-Zone.groovy
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
 *            --- Driver para AMCP 4.5 - Zone
 *           v.1  30/04/2025 - BETA. 
 *
 */
metadata {
    definition (
        name: "MR450DXT-Zone",
        namespace: "TRATO",
        author: "VH"
    ) {
        capability "Switch"
        capability "AudioVolume"
        capability "Refresh"
        
        attribute "bass", "number"
        attribute "treble", "number"
        attribute "currentSource", "number"
        
        command "setBass", [[name: "level", type: "NUMBER", description: "Bass level (-7 to +7)"]]
        command "setTreble", [[name: "level", type: "NUMBER", description: "Treble level (-7 to +7)"]]
        command "setSource", [[name: "sourceId", type: "NUMBER", description: "Source ID (1-6)"]]
        command "volumeUp"
        command "volumeDown"
    }
    
    preferences {
        input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() {
    log.info "MR450D-XT Zone installed"
    sendEvent(name: "bass", value: 0)
    sendEvent(name: "treble", value: 0)
    sendEvent(name: "currentSource", value: 1)
    sendEvent(name: "volume", value: 50)
}

def updated() {
    log.info "MR450D-XT Zone updated"
}

def parse(String description) {
    if (debugLogging) log.debug "Parse: ${description}"
}

def on() {
    // Power on using volume up command (as specified)
    volumeUp()
    sendEvent(name: "switch", value: "on")
}

def off() {
    // Use mute command for off (as specified)
    mute()
    sendEvent(name: "switch", value: "off")
}

def setVolume(level) {
    level = Math.max(0, Math.min(100, level.toInteger()))
    if (debugLogging) log.debug "Setting absolute volume to ${level} in zone ${getZoneNumber()}"
    
    // Convert 0-100 scale to hex value (mapping from documentation)
    def hexValues = [
        0:"CE", 10:"C5", 20:"BB", 30:"B1", 40:"A6", 
        50:"9D", 60:"93", 70:"89", 80:"80", 90:"75", 100:"6B"
    ]
    
    // Find closest match
    def closest = hexValues.keySet().min { Math.abs(it.toInteger() - level) }
    def hexLevel = hexValues[closest]
    
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 56 ${hexLevel} 30 30 0D")
    sendEvent(name: "volume", value: closest.toInteger())
}

def volumeUp() {
    if (debugLogging) log.debug "Volume up in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 56 2B 30 30 0D")
    
    // Update volume state (approximate)
    def currentVol = device.currentValue("volume") ?: 50
    sendEvent(name: "volume", value: Math.min(100, currentVol + 5))
    sendEvent(name: "switch", value: "on") // Treat as power on
}

def volumeDown() {
    if (debugLogging) log.debug "Volume down in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 56 2D 30 30 0D")
    
    // Update volume state (approximate)
    def currentVol = device.currentValue("volume") ?: 50
    sendEvent(name: "volume", value: Math.max(0, currentVol - 5))
}

def mute() {
    if (debugLogging) log.debug "Muting (turning off) zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 4D 80 30 30 0D")
    sendEvent(name: "mute", value: "muted")
    sendEvent(name: "switch", value: "off")
}

def unmute() {
    if (debugLogging) log.debug "Unmuting (turning on) zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 55 80 30 30 0D")
    sendEvent(name: "mute", value: "unmuted")
    sendEvent(name: "switch", value: "on")
}

def setBass(level) {
    level = Math.max(-7, Math.min(7, level.toInteger()))
    if (debugLogging) log.debug "Setting bass to ${level} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexLevel = Integer.toHexString(0x80 + level).toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 47 ${hexLevel} 30 30 0D")
    sendEvent(name: "bass", value: level)
}

def setTreble(level) {
    level = Math.max(-7, Math.min(7, level.toInteger()))
    if (debugLogging) log.debug "Setting treble to ${level} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexLevel = Integer.toHexString(0x80 + level).toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 41 ${hexLevel} 30 30 0D")
    sendEvent(name: "treble", value: level)
}

def setSource(sourceId) {
    sourceId = Math.max(1, Math.min(6, sourceId.toInteger()))
    
    if (debugLogging) log.debug "Setting source to ${sourceId} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexSource = Integer.toHexString(0x80 + sourceId).toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 45 ${hexSource} 30 30 0D")
    sendEvent(name: "currentSource", value: sourceId)
}

def refresh() {
    if (debugLogging) log.debug "Refreshing zone ${getZoneNumber()} status"
    // The MR 4.50 D-XT doesn't support status queries, so we just update our local state
}

private getZoneNumber() {
    return device.deviceNetworkId.split(":zone")[1]
}
