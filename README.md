# [Cancellino](https://lizzit.it/cancellino) - Open-source active-RFID automatic gate opening system
Raspberry Pi automatic RFID gate opening system with Telegram integration


    README.md
  
    Written by: Michele Lizzit <lizzit.it>, 25 Apr 2017
    Last update: 25 Apr 2017
    Version: 1.0

More informations are available at the project website: https://lizzit.it/cancellino  

## Description of the project
Cancellino is composed of some active tags and a main unit connected to the gate lock, when the main unit detects the presence of a tag in a set range (configurable from a few cm to 10m), it opens the gate.  
Cancellino does not implemet any form of encryption, the tag broadcasts a static key every two seconds, if the key is correct the main unit opens the gate. It was originally designed for applications where the security of the gate is not a concern (in my case the gate is 1m high, and can be easily opened with a piece of plastic).  
The main unit is composed of a Raspberry Pi connected to the gate lock and to an nRF24L01+ wireless module.  
Each tag is composed of a battery (CR2032), an MCU (Atmega328), and an nRF24L01+ wireless module.  

| ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/main3.jpg) | ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/overview.jpg) |
|---|---|
| The main unit in place | The gate |

## Install:

* connect the nRF24L01+ module to your RaspberryPI
* enable the SPI interface using raspi-config
* install telegram-cli and enable its daemon
* copy the folder "cancellino" to your RaspberryPI
* configure cancellino by editing "Cancello.java"
* place a script in /home/pi/open_gate_raspi.sh that opens the gate when called (usually a script that sends an high pulse on a GPIO that is connected to the lock via a solid state relay)
* configure the tag by editing Config.h
* upload the tag firmware using Arduino IDE


## Start:
```bash
javac -classpath .:classes:/opt/pi4j/lib/'*' Cancello.java;
sudo java -classpath .:classes:/opt/pi4j/lib/'*' Cancello
```
  
To automatically start the software at boot you can create an init file that executes only the last command (there is no need to compile the source code each time).  

## Tag
The tag can be built either by using the PCB or by using a prototype board, the advantage of the PCB version is its small size.  

| ![PCB](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/pcb_1.png) | ![PCB](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/pcb_2.png) |
|---|---|

The tag is powered by one CR2032 battery, with one battery the tag can work for about 6 months.  
The tag has an ICSP port which can be used to program the MCU (Atmega328P).  


| ![TAG](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/tag2.jpg) | ![TAG](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/tag3.jpg) |
|---|---|
| A "PCB version" tag | A "PCB version" tag |


The MCU is configured to use its internal (8MHz) oscillator in order to minimize the number of electronic components needed for a tag.  



Every two seconds the tag broadcasts a string and then puts the nRF24L01+ in "power down" mode, in this mode the nRF24L01+ draws about 100nA.  
The module draws 7.0mA while transmitting, but the total time the nRF24L01+ in not in power down mode is about 40ms every 2 seconds so the (theoretical) total power consumption is still very low.  
The actual average power consumption I measured is 50µA, which allows about 6 months of continuous operation with a single CR2032.  

| ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/main2.jpg) | ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/main1.jpg) |
|---|---|
| The nRF24L01+ module on the main unit | The main unit |

The receiver is placed on a Raspberry Pi near the gate (I also use the Raspberry Pi as a door phone), when the Raspberry Pi receives an authorized string (which presumably comes from an authorized tag) it opens the gate, plays an audio file and sends a Telegram message containing informations on the tag (who has opened the gate).  
In a future software release I plan to implement a feature that notifies the user when the battery is low via Telegram (without requiring a hardware modification). 

| ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/telegram_screenshot.jpg) |
|---|
| A Telegram message from Cancellino | 

| ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/tag1.jpg) | ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/tag_icsp.jpg) |
|---|---|
| The tag before soldering the nRF24L01+ module | An AVR programmer connected to a tag via ICSP |

| ![ICSP](https://raw.githubusercontent.com/michelelizzit/cancellino/master/pictures/tag_prototype.jpg) |
|---|
| A prototype of the tag on a perfboard in its 3D-printed case | 
