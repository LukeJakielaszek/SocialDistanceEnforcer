# SocialDistanceEnforcer

## Overview
An Android application which uses bluetooth to detect for nearby devices and notify users if social distancing guidelines are violated. 

## Methodology
A constructed dataset mapping RSSI values to devices with known classes has been stored within the application. Devices are discovered using bluetooth's discovery phase.
The RSSI value along with major/minor classes of detected devices are tracked. These RSSI values are compared to the constructed dataset allowing for a distance estimate. 
If a device is detected within 6 feet, the application will sound an alarm to notify the user.
