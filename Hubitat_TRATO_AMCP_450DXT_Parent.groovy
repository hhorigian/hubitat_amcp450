/**
 *  // MR450DXT-Parent.groovy - Driver principal
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
 *            --- Driver para AMCP 4.5 - Parent
 *           v.1  30/04/2025 - BETA. 
 *
 */
metadata {
    definition (
        name: "MR450DXT Parent",
        namespace: "TRATO",
        author: "VH"
    ) {
        capability "Initialize"
        
        command "createChildDevices"
        command "sendCommand", ["string"]
        command "deleteAllChildDevices"
        attribute "connection", "string"
    }
    
    preferences {
        input name: "ipAddress", type: "text", title: "IP Address", required: true
        input name: "port", type: "number", title: "Port", required: true, defaultValue: 4999
        input name: "reconnectInterval", type: "number", title: "Reconnect Interval (seconds)", required: true, defaultValue: 60
        input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def installed() {
    log.info "MR450DXT Parent installed"
    initialize()
}

def updated() {
    log.info "MR450DXT Parent updated"
    initialize()
}

def initialize() {
    log.info "Initializing MR450DXT Parent"
    
    state.lastCommandSent = 0
    state.commandDelay = 200 // ms between commands
    
    // Close any existing connection
    interfaces.rawSocket.close()
    
    // Open new connection
    try {
        interfaces.rawSocket.connect(ipAddress, port.toInteger(), byteInterface: true)
        sendEvent(name: "connection", value: "connected")
        log.info "Connected to ${ipAddress}:${port}"
    } catch (Exception e) {
        log.error "Connection failed: ${e.message}"
        sendEvent(name: "connection", value: "disconnected")
        runIn(reconnectInterval, initialize)
    }
}

def uninstalled() {
    interfaces.rawSocket.close()
}

def socketStatus(String message) {
    if (message.contains("status: CLOSED")) {
        log.warn "Connection closed"
        sendEvent(name: "connection", value: "disconnected")
        runIn(reconnectInterval, initialize)
    } else if (message.contains("status: ERROR")) {
        log.error "Connection error"
        sendEvent(name: "connection", value: "error")
        runIn(reconnectInterval, initialize)
    }
}

def parse(String message) {
    if (debugLogging) log.debug "Received: ${message}"
}

def sendCommand(String cmd) {
    if (now() - state.lastCommandSent < state.commandDelay) {
        pauseExecution(state.commandDelay - (now() - state.lastCommandSent))
    }

    try {
        // For iTach devices, we need to use sendserialhex format
        def formattedCmd = cmd + "\r"
        
        if (debugLogging) log.debug "Sending iTach command: ${formattedCmd}"
        
        interfaces.rawSocket.sendMessage(formattedCmd)
        state.lastCommandSent = now()
    } catch (Exception e) {
        log.error "Failed to send command: ${e.message}"
        sendEvent(name: "connection", value: "error")
        runIn(reconnectInterval, initialize)
    }
}


def deleteAllChildDevices() {
    if (debugLogging) log.debug "Deleting all child devices"
    
    try {
        // Delete zone children (1-6)
        for (int i = 1; i <= 6; i++) {
            def childDni = "${device.deviceNetworkId}:zone${i}"
            def child = getChildDevice(childDni)
            if (child) {
                deleteChildDevice(childDni)
                if (debugLogging) log.debug "Deleted zone child ${i}"
            }
        }
        
        // Delete source children (1-6)
        for (int i = 1; i <= 6; i++) {
            def childDni = "${device.deviceNetworkId}:source${i}"
            def child = getChildDevice(childDni)
            if (child) {
                deleteChildDevice(childDni)
                if (debugLogging) log.debug "Deleted source child ${i}"
            }
        }
        
        log.info "Successfully deleted all child devices"
    } catch (Exception e) {
        log.error "Error deleting child devices: ${e}"
    }
}

def createChildDevices() {
    log.info "Creating child devices for MR 4.50 D-XT"
    
    // Create zone devices (1-6)
    for (int i = 1; i <= 6; i++) {
        def childDni = "${device.deviceNetworkId}:zone${i}"
        def child = getChildDevice(childDni)
        
        if (!child) {
            try {
                child = addChildDevice(
                    "TRATO",
                    "MR450DXT-Zone",
                    childDni,
                    [
                        name: "${device.displayName} Zone ${i}",
                        label: "${device.displayName} Zone ${i}",
                        isComponent: false
                    ]
                )
                log.info "Created Zone ${i} child device"
            } catch (Exception e) {
                log.error "Error creating Zone ${i} child: ${e}"
            }
        }
    }
    
    // Create source devices (1-6)
    for (int i = 1; i <= 6; i++) {
        def childDni = "${device.deviceNetworkId}:source${i}"
        def child = getChildDevice(childDni)
        
        if (!child) {
            try {
                child = addChildDevice(
                    "TRATO",
                    "MR450DXT-Source",
                    childDni,
                    [
                        name: "${device.displayName} Source ${i}",
                        label: "${device.displayName} Source ${i}",
                        isComponent: false
                    ]
                )
                child.setSourceId(i)
                log.info "Created Source ${i} child device"
            } catch (Exception e) {
                log.error "Error creating Source ${i} child: ${e}"
            }
        }
    }
}
