/**
 * MIMOlite device type for garage door button, including power failure indicator.  Be sure mimolite has jumper removed before
 * including the device to your hub, and tap Config to ensure power alarm is subscribed.
 *
 *  Author: Many ST community members
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "My MIMOlite - Garage Car Door v4", namespace: "jscgs350", author: "jscgs350") {
		capability "Configuration"
        capability "Momentary"        
		capability "Polling"
		capability "Switch"
		capability "Refresh"
        capability "Sensor"        
		capability "Contact Sensor"
		capability "Actuator"
		capability "Door Control"
		capability "Garage Door Control"
        capability "garageDoorControl"
        
		attribute "powered", "string"
        attribute "contactState", "string"
        attribute "powerState", "string"        
        
		command "on"
		command "off"

        fingerprint deviceId: "0x1000", inClusters: "0x72,0x86,0x71,0x30,0x31,0x35,0x70,0x85,0x25,0x03"
	}

	// UI tile definitions 
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
            	attributeState "doorClosed", label: "Closed", action: "on", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821", nextState:"openingdoor"
            	attributeState "doorOpen", label: "Open", action: "on", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e", nextState:"closingdoor"
                attributeState "closingdoor", label:'Closing', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffd700"
                attributeState "openingdoor", label:'Opening', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffd700"
//            	attributeState "on", label: "Actuate", action: "on", icon: "st.doors.garage.garage-closed", backgroundColor: "#53a7c0"
			}
		}
        standardTile("contact", "device.contact", inactiveLabel: false) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        standardTile("powered", "device.powered", width: 2, height: 2, inactiveLabel: false) {
			state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ffa81e"
		}
		standardTile("configure", "device.configure", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		main (["switch", "contact"])
		details(["switch", "powered", "refresh", "configure"])
    }
}

def parse(String description) {
log.debug "description is: ${description}"

	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1])
    
    log.debug "command value is: $cmd.CMD"
    
    if (cmd.CMD == "7105") {				//Mimo sent a power loss report
    	log.debug "Device lost power"
    	sendEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
    } else {
    	sendEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
    }
    
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText}"
    
	return result
}

def sensorValueEvent(Short value) {
	if (value) {
        sendEvent(name: "contact", value: "open")
        sendEvent(name: "switch", value: "doorOpen")
	} else {
        sendEvent(name: "contact", value: "closed")
        sendEvent(name: "switch", value: "doorClosed")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
//	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
//	def doorState = device.currentValue('contact')
//    if ( doorState == "closed")
//		[name: "switch", value: cmd.value ? "on" : "doorOpening", type: "digital"]
//    else
//    	[name: "switch", value: cmd.value ? "on" : "doorClosing", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd)
{
    log.debug "We lost power" //we caught this up in the parse method. This method not used.
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	push()
}

def off() {
	push()
}

def push() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def poll() {
	refresh()
}

def refresh() {
	log.debug "Executing Refresh/Poll for garage car door per user request"
	delayBetween([
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
		zwave.alarmV1.alarmGet().format() 
	],100)
}

def configure() {
	log.debug "Configuring...." //setting up to monitor power alarm and actuator duration
	delayBetween([
		zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(),
        zwave.configurationV1.configurationSet(configurationValue: [25], parameterNumber: 11, size: 1).format(),
        zwave.configurationV1.configurationGet(parameterNumber: 11).format()
	])
}
