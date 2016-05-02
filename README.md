# Ratiometric Imaging plugin for uManager

This is Java application, designed for CCD/CMOS camera-based fluorescence measurements.

## Features

* Automated experimental flow. Describe experimental stages once. Then, put your sample, press Start and wait.
* Automatic management of experimental data/labnotes storage. All naming, saving, comment templating is done automatically.
* Designed with ratiometric imaging tasks in mind.
* Online and offline analysis of fluorescence in ROIs with automatic background correction.
* Works on Windows, expected (but not tested) to work on MacOS.

##Requirements
	
* Works within uManager 2.0 framework (https://micro-manager.org/) as a plugin (based on API 2.0)
* Requires either LPT port or other digital state device (e.g. NI card with digital outs) to control Sutter filter wheel.

##Instructions to compile and run

1. JDK 1.6 (for uManager plugins) and Netbeans 8+/JDK1.7+ installed. This is a Netbeans project, and is expected to compile without problems.
1. If you don’t have experience in writing plugins for uManager, please read [this](https://micro-manager.org/wiki/Micro-Manager_Programming%20Guide) and [this](https://micro-manager.org/wiki/Version_2.0#Important_Note_Regarding_Netbeans) before trying to compile the project. 
1. In order to use the plugin, place RMI2.jar to \Micro-Manager-2.0beta\mmplugins
1. Place org-jdesktop-layout.jar and swing-layout-1.0.4.jar to \Micro-Manager-2.0beta\plugins\Micro-Manager

###Enjoy and contribute!

