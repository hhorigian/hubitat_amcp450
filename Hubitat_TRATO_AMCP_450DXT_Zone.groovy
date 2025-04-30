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
}

def updated() {
    log.info "MR450D-XT Zone updated"
}

def parse(String description) {
    if (debugLogging) log.debug "Parse: ${description}"
}

def on() {
    if (debugLogging) log.debug "Turning on zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 4C 80 30 30 0d")
    sendEvent(name: "switch", value: "on")
}

def off() {
    if (debugLogging) log.debug "Turning off zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 44 80 5A 58 0d")
    sendEvent(name: "switch", value: "off")
}

def setVolume(level) {
    if (debugLogging) log.debug "Setting volume to ${level} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexLevel = Integer.toHexString(256 - Math.round(level * 2.55)).padLeft(2, '0').toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 56 ${hexLevel} 30 30 0d")
    sendEvent(name: "volume", value: level)
}

def mute() {
    if (debugLogging) log.debug "Muting zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 4D 80 30 30 0d")
    sendEvent(name: "mute", value: "muted")
}

def unmute() {
    if (debugLogging) log.debug "Unmuting zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    parent.sendCommand("02 A1 45 3${zone} 55 80 30 30 0d")
    sendEvent(name: "mute", value: "unmuted")
}

def setBass(level) {
    level = Math.max(-7, Math.min(7, level.toInteger()))
    if (debugLogging) log.debug "Setting bass to ${level} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexLevel = Integer.toHexString(0x80 + level).toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 47 ${hexLevel} 30 30 0d")
    sendEvent(name: "bass", value: level)
}

def setTreble(level) {
    level = Math.max(-7, Math.min(7, level.toInteger()))
    if (debugLogging) log.debug "Setting treble to ${level} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexLevel = Integer.toHexString(0x80 + level).toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 41 ${hexLevel} 30 30 0d")
    sendEvent(name: "treble", value: level)
}

def setSource(sourceId) {
    // Only allow source IDs 1-4
    sourceId = Math.max(1, Math.min(4, sourceId.toInteger()))
    
    if (debugLogging) log.debug "Setting source to ${sourceId} in zone ${getZoneNumber()}"
    def zone = getZoneNumber()
    def hexSource = Integer.toHexString(0x80 + sourceId).toUpperCase()
    parent.sendCommand("02 A1 45 3${zone} 45 ${hexSource} 30 30 0d")
    sendEvent(name: "currentSource", value: sourceId)
}

def refresh() {
    if (debugLogging) log.debug "Refreshing zone ${getZoneNumber()} status"
    // The MR 4.50 D-XT doesn't support status queries, so we just update our local state
}

private getZoneNumber() {
    return device.deviceNetworkId.split(":zone")[1]
}