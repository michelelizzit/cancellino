/*  
 *  Cancellino - code for the RFID active tag
 *  Author: Michele Lizzit <michele@lizzit.it> - lizzit.it
 *  v1.1 - 23/4/2017
 *  
 *  Please go to https://lizzit.it/cancellino for more informations about the project
 *  
 *  
 *  Copyright (C) 2017  Michele Lizzit
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published
 *  by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "Config.h"

#include "nRF24L01_mini.h"
#include <avr/power.h>
#include <avr/sleep.h>
#include <avr/wdt.h>
#include "LowPower.h"

#ifdef DEBUG
unsigned long timeOn;
#endif

void setup() {
  //if serial debug is enabled initialize the serial port and send a character
  #ifdef DEBUG
    Serial.begin(115200);
    Serial.println("Initializing...");
    timeOn = micros();
  #endif

  delayMicroseconds(50000);
  
  initializeNRF24L01(); //initialize the nRF24L01+
  powerDownNRF(); //put the nRF24L01+ into power down mode (0.1mA power consumption)
  //powerSaveMode(); //for some strange and unknown reason uses more power instead of less

  //WARNING: if the battery is low, tension will drop and the code 
  //will hang here, resetting the MCU and triggering an infinite loop
  //that will rapidly discarge the remaining battery
  //TODO: prevent complete battery discarge

  /*powerDownNRF(); //put the nRF24L01+ into power down mode (0.1mA power consumption)
  enterSleepMode(); //put the MCU into sleep mode
  wakeUpRoutine(); //function called when exiting sleep*/

  //if debug blink is enabled set pin 13 to output
  #ifdef DEBUG_BLINK13
    pinMode(13, OUTPUT);
  #endif
}

byte authKey[] = {'P', 'L', 'U', 'T', 'O', 'N', 'I', 'U', 'M', '0'}; //the static packet to be transmitted
void loop() {
   powerUpNRF(); //power up the nRF24L01+ from "power down" state
   delayMicroseconds(150); //time required for the nRF24L01+ to start up
   sendStringNRF(authKey, 10); //transmit the 10-byte packet
   powerDownNRF(); //put the nRF24L01+ into power down mode (0.1mA power consumption)
   
   enterSleepMode(); //put the MCU into sleep mode
   //now the MCU waits 2 seconds in sleep mode
   wakeUpRoutine(); //function called when exiting sleep
   
   #ifdef DEBUG //if serial debug enabled send a char on the serial port
     Serial.println("W");
     Serial.flush();
   #endif

   #ifdef DEBUG_BLINK13 //if debug blink enabled, send a high pulse on pin 13
     digitalWrite(13, HIGH);
     delay(250);
     digitalWrite(13, LOW);
   #endif
}

void enterSleepMode() {
  //power_spi_disable();
  LowPower.powerDown(SLEEP_2S, ADC_OFF, BOD_OFF);  
  #ifdef DEBUG
    Serial.print("MCU was ACTIVE for ");
    Serial.print(micros() - timeOn);
    Serial.println(" uS");
  #endif
}

void wakeUpRoutine() {
  //power_spi_enable();  
  #ifdef DEBUG
    timeOn = micros();
  #endif
}

void powerSaveMode() {
  //disable brown out detection
  noInterrupts();
  MCUCR = bit (BODS) | bit (BODSE);
  MCUCR = bit (BODS);
  interrupts();

  //duplicate of the code above
  sleep_bod_disable(); 

  //disable ADC
  power_adc_disable();

  //disable I2C
  power_twi_disable();

  //disables some timers
  power_timer0_disable();
  power_timer1_disable();
  power_timer2_disable();
}
