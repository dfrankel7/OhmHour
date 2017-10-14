/**
 *  Ohm Hour
 *
 *  Copyright 2017 Daniel Frankel
 *  Version 1.0 10/13/17
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
 */
definition(name: "OhmHour", namespace: "", author: "Daniel Frankel",
    description: "Runs phrases when a ohmHour start/ends", category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences 
{
    page(name: "getPrefrences")
}

def getPrefrences()
{
    dynamicPage(name: "getPrefrences", title: "Enter your OhmConnect ID", install:true, uninstall: true)
    {
        section("Configure your Ohmconnect 8 digit ID (end of the URL at the bottom of the subscriptions section on the settings page)")
        {
            input "ohmID", "text", title: "Ohm Connect ID", required: true
        }
   
        def phrases = location.helloHome?.getPhrases()*.label
        
        if (phrases) 
        {
            phrases.sort()
            
            section("Perform this phrase when...") 
            {
                log.trace phrases
                input "phrase_start", "enum", title: "Ohm Hour starts", required: true, options: phrases
                input "phrase_end", "enum", title: "Ohm Hour ends", required: true, options: phrases
            }
        }
        
        section("Don't perform if in these modes...")
        {
            input "ignoreModes", "mode", title: "Select the mode(s)", multiple: true, required: false
        }
        
        section( "Notifications" ) 
        {
            input("recipients", "contact", title: "Send notifications to") 
            {
                input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
                input "phoneNumber", "phone", title: "Send a text message?", required: false
            }
        }
    }
}

page(name: "pageAbout", title: "About ${textAppName()}") 
{
    section
    {
        paragraph "${textVersion()}\n${textCopyright()}\n\n${textLicense()}\n"
    }
    
    section("Instructions")
    {
        paragraph textHelp()
    }
}

def installed() 
{
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()
{
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() 
{
    state.isOhmHourStarted = false
    runEvery1Minute(updateOhmHourStatus)
}

def updateOhmHourStatus() 
{
    def mode = location.mode
    
    if (settings.ignoreModes == null || settings.ignoreModes?.contains(mode) == false)
    {
        //log.debug "Checking for OhmHour"
        def params = [uri:  'https://login.ohmconnect.com/verify-ohm-hour/', path: ohmID]

        try 
        {
            httpGet(params) 
            {
                resp ->

                if(resp.data.active == "True")
                {
                    if (state.isOhmHourStarted != true)
                    {
                        startOhmHour()
                    }
                }
                else
                {
                    if (state.isOhmHourStarted == true)
                    {
                        endOhmHour()
                    }
                }
            }
        }
        catch (e)
        {
            log.error "something went wrong: $e"
        }
    }
}

def startOhmHour()
{
    log.debug "Ohm Hour is active. Run the start phrase."
    state.isOhmHourStarted = true;
    location.helloHome.execute(settings.phrase_start)
    
    sendNotifications "An OhmHour has started. Running start routine."
}

def endOhmHour() 
{
    log.debug "Ohm Hour is over. Run the end phrase."
    state.isOhmHourStarted = false;
    location.helloHome.execute(settings.phrase_end)
    
    sendNotifications "An OhmHour has ended. Running end routine."
}

def sendNotifications(message)
{
    if (settings.sendPushMessage == "Yes")
    {
        log.debug("sending push message")
        sendPush(message)
    }

    if (settings.phoneNumber)
    {
        log.debug("sending text message")
        sendSms(phoneNumber, message)
    }
}

//Version/Copyright/Information/Help

private def textAppName() 
{
    def text = "Ohm Connect - Ohm Hour"
}    

private def textVersion() 
{
    def text = "Version 1.0 (10/02/17)"
}

private def textCopyright() 
{
    def text = "Copyright © 2017 Daniel Frankel"
}

private def textLicense() 
{
    def text =
        "Licensed under the Apache License, Version 2.0 (the 'License'); "+
        "you may not use this file except in compliance with the License. "+
        "You may obtain a copy of the License at"+
        "\n\n"+
        "    http://www.apache.org/licenses/LICENSE-2.0"+
        "\n\n"+
        "Unless required by applicable law or agreed to in writing, software "+
        "distributed under the License is distributed on an 'AS IS' BASIS, "+
        "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
        "See the License for the specific language governing permissions and "+
        "limitations under the License."
}

private def textHelp() 
{
    def text =
        "Ohm Connect is a service that notifies you during peak power grid demand so you can reduce your consumption.(referral link: https://ohmc.co/e4416cb)" +
        "This app pings the Ohm Connect server every minute to see if there is an Ohm Hour Event. " +
        "When there is, it will run a Hello, Home phrase to help reduce your power use. After the Ohm Hour ends it will run an additional Hello, Home phrase." 
}