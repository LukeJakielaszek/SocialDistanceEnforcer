# SocialDistanceEnforcer

## Overview
An Android application which uses bluetooth to detect for nearby devices and notify users if social distancing guidelines are violated. 

## Methodology
A constructed dataset mapping RSSI values to devices with known classes has been stored within the application. Devices are discovered using bluetooth's discovery phase.
The RSSI value along with major/minor classes of detected devices are tracked. These RSSI values are compared to the constructed dataset allowing for a distance estimate (KNN approach). If a device is detected within 6 feet, the application will sound an alarm to notify the user.

## Dataset Creation
The dataset was construct in a controlled environment using several devices. RSSI measurements were recorded at 1, 3, 6, 10, and 15 feet. Collection of data was done through the Java Application. Additional data visualization and analysis is done through the included Python scripts.
