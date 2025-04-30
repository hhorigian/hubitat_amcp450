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
    // Only allow source IDs 1-4
    state.sourceId = Math.max(1, Math.min(4, id.toInteger()))
    sendEvent(name: "sourceId", value: state.sourceId)
}

def getSourceId() {
    return state.sourceId ?: 1
}