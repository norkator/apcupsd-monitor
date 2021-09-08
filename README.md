[![APCUPSD UPS Battery Monitoring Android application Promo Video](https://img.youtube.com/vi/N4PhGXOUyas/0.jpg)](https://www.youtube.com/watch?v=N4PhGXOUyas)

Upper image is a video link.

# APCUPSD Monitor

Android app for monitoring APC and EATON IPM UPS device statuses and view events.
Watch Youtube video for better description. 
I brought this project on Github so other people can add features for their own use. 
See license and contribution rules below. 


Table of contents
=================
* [Images](#images)
* [Features](#features)
* [UPS specific notes](#ups-specific-notes)
  * [APCUPSD](#apcupsd) 
  * [Eaton](#eaton)
* [Requirements](#requirements)
* [Contributions](#contributions)
* [Authors](#authors)
* [Contributors](#contributors)
* [License](#license)


Images
============
Example of dark theme widget             |  Example of events
:-------------------------:|:-------------------------:
![widget](graphics/widget.jpg)   |  ![events](graphics/events.jpg)


Features
============
* Basic status details viewer.
* Event logs viewer.
* Supports parsing data via SSH, APCUPSD Daemon NIS, Synology UPS, APC Network Management Card, Eaton IPM (Intelligent Power Manager).
* Firebase based status check triggering and monitoring with notifications.
* Front screen widget support.
* Light and dark themes automatically following Android OS system theme.  


UPS specific notes
============
Always be careful when opening UPS software interfaces to public internet. These interfaces seem to be interesting target 
for security researchers and bad people. Learn to create VPN tunnel from your device to your network instead and route 
traffic trough that. Or use only in local network via WLAN.

APCUPSD
-----
* No special notes.

Eaton
-----
* This app is develop towards IPM version -> `1.69.253`
* Use IPM with https (default) installation way, which means web UI opens from port 4680.
* This app requires valid https certificate so try have one with your IPM server. This is forced by Google in a way that app will get deleted from Google Play if it contains vulnerable workarounds. Possible solution for certificates https://certbot.eff.org/lets-encrypt/windows-other.html


Requirements
============
1. Download and install Android Studio: https://developer.android.com/studio
2. You need virtual or physical phone.
3. Follow IDE instructions.
4. Open issue if something is unclear.


Contributions
============

* Feel free to add new features and fix bugs. I review pull requests and make releases when required.
* If you are planning to make new feature or bigger change, open issue and let's talk about it first.
Because it needs to serve everyone.
* If you feel my code is horrible, feel free to tell me about it. No hard feelings, lets improve together.
* Have huge need to write tests? you are really welcome to do so. Jenkinsfile has unit tests disabled because we have none.


Authors
============

* **Norkator** - *Initial work* - [norkator](https://github.com/norkator)


Contributors
============
* [bo0tzz](https://github.com/bo0tzz) - [See commits](https://github.com/norkator/apcupsd-monitor/commits?author=bo0tzz)


License
============
See repo license section or license file.

Permission to release this app on any Play Store as a new app is forbidden!
