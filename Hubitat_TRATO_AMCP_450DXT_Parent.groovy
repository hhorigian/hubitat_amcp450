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
        command "powerOnAllZones"
        command "powerOffAllZones"
        command "muteAllZones"
        command "unmuteAllZones"        
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
        interfaces.rawSocket.connect(ipAddress, port.toInteger())
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
    // Handle responses if needed
}

def sendCommand(String cmd) {
    if (now() - state.lastCommandSent < state.commandDelay) {
        pauseExecution(state.commandDelay - (now() - state.lastCommandSent))
    }
    
    if (debugLogging) log.debug "Sending command: ${cmd}"
    
    try {
        // Convert hex string to bytes
        def bytes = []
        cmd.split().each { part ->
            bytes.add(Integer.parseInt(part, 16))
        }
        
        interfaces.rawSocket.sendMessage(bytes)
        state.lastCommandSent = now()
    } catch (Exception e) {
        log.error "Failed to send command: ${e.message}"
        sendEvent(name: "connection", value: "error")
        runIn(reconnectInterval, initialize)
    }
}

def on(){
powerOnAllZones()
}

def off(){
powerOffAllZones()
}

// Power Control for All Zones
def powerOnAllZones() {
    (1..4).each { zone ->
        sendCommand("02 A1 45 3${zone} 4C 80 30 30 0d") // Power ON command
        getChildDevice("${device.deviceNetworkId}:zone${zone}")?.sendEvent(name: "switch", value: "on")
    }
}

def powerOffAllZones() {
    (1..4).each { zone ->
        sendCommand("02 A1 45 3${zone} 44 80 5A 58 0d") // Power OFF command
        getChildDevice("${device.deviceNetworkId}:zone${zone}")?.sendEvent(name: "switch", value: "off")
    }
}

// Mute Control for All Zones
def muteAllZones() {
    (1..4).each { zone ->
        sendCommand("02 A1 45 3${zone} 4D 80 30 30 0d") // Mute command
        getChildDevice("${device.deviceNetworkId}:zone${zone}")?.sendEvent(name: "mute", value: "muted")
    }
}

def unmuteAllZones() {
    (1..4).each { zone ->
        sendCommand("02 A1 45 3${zone} 55 80 30 30 0d") // Unmute command
        getChildDevice("${device.deviceNetworkId}:zone${zone}")?.sendEvent(name: "mute", value: "unmuted")
    }
}


def deleteAllChildDevices() {
    if (debugLogging) log.debug "Deleting all child devices"
    
    try {
        // Delete zone children (1-4)
        for (int i = 1; i <= 4; i++) {
            def childDni = "${device.deviceNetworkId}:zone${i}"
            def child = getChildDevice(childDni)
            if (child) {
                deleteChildDevice(childDni)
                if (debugLogging) log.debug "Deleted zone child ${i}"
            }
        }
        
        // Delete source children (1-4)
        for (int i = 1; i <= 4; i++) {
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
    
    // Create zone devices (1-4)
    for (int i = 1; i <= 4; i++) {
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
    
    // Create source devices (only 1-4 now)
    for (int i = 1; i <= 4; i++) {
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
