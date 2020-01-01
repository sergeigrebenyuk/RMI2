# Ratiometric Imaging plugin for uManager

This is Java application, designed for CCD/CMOS camera-based fluorescence measurements.

## Features

* Designed with ratiometric imaging tasks in mind.
* Automatic management of experimental data/labnotes storage. All naming, saving, comment templating is done automatically.
* Online and offline analysis of fluorescence in ROIs with automatic background correction.
* Works on Windows, expected (but not tested) to work on MacOS.

Coming soon:
* Automated experimental flow. Describe your drug application order and timing once. Then, put your sample, press Start and wait. (Requires remotely controlled valves)

## Requirements
	
* Works within uManager 2.0 framework (https://micro-manager.org/) as a plugin (based on API 2.0)
* Requires either LPT port or other digital state device (e.g. NI card with digital outs) to control Sutter filter wheel and camera shutter.

## Instructions to compile and run

1. JDK 1.6 (for uManager plugins) and Netbeans 8+/JDK1.7+ installed. This is a Netbeans project, and is expected to compile without problems.
1. If you don’t have experience in writing plugins for uManager, please read [this](https://micro-manager.org/wiki/Micro-Manager_Programming%20Guide) and [this](https://micro-manager.org/wiki/Version_2.0#Important_Note_Regarding_Netbeans) before trying to compile the project. 
1. In order to use the plugin, place RMI2.jar to \Micro-Manager-2.0beta\mmplugins
1. Create \Micro-Manager-2.0beta\mmplugins\lib folder and place [org-jdesktop-layout.jar](https://github.com/sergeigrebenyuk/RMI2/blob/master/org-jdesktop-layout.jar) and [swing-layout-1.0.4.jar](https://github.com/sergeigrebenyuk/RMI2/blob/master/swing-layout-1.0.4.jar) to it.

Please, also see the Wiki for some details

Alternatively, simply download precompiled RMI.jar (along with putting 
org-jdesktop-layout.jar and swing-layout-1.0.4.jar to \mmplugins\lib 
folder) and run uManager

### Enjoy and contribute!

