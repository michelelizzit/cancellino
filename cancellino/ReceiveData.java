/*
 *  nRF24L01+ Java library for RaspberryPi - adapted version for "cancellino"
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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import java.io.*;

public class ReceiveData {

	public static SpiDevice spi = null;
	private byte WRITE_CMD = 0x40;
	private byte READ_CMD  = 0x41;
	final GpioController gpio = GpioFactory.getInstance();
	final GpioPinDigitalOutput CE_nRF24L01 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06); //utilizza la numerazione wiringPI che e' molto strana

	@SuppressWarnings("unused")
	public void start() {
		try {
			spi = SpiFactory.getInstance(SpiChannel.CS0,
					500000,//SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
					SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0
		} catch (IOException e) {
			e.printStackTrace();
		}
		CE_nRF24L01.low();
		initializeNRF24L01();
		setRXMode();
		CE_nRF24L01.high();
	}

	//Commands
	final byte R_REG = (byte)0x00;
	final byte W_REG = (byte)0x20;
	final byte RX_PAYLOAD = (byte)0x61;
	final byte TX_PAYLOAD = (byte)0xA0;
	final byte FLUSH_TX = (byte)0xE1;
	final byte FLUSH_RX = (byte)0xE2;
	final byte ACTIVATE = (byte)0x50;
	final byte R_STATUS = (byte)0xFF;

	//Registers
	final byte CONFIG = (byte)0x00;
	final byte EN_AA = (byte)0x01;
	final byte EN_RXADDR = (byte)0x02;
	final byte SETUP_AW = (byte)0x03;
	final byte SETUP_RETR = (byte)0x04;
	final byte RF_CH = (byte)0x05;
	final byte RF_SETUP = (byte)0x06;
	public final byte STATUS = (byte)0x07;
	final byte OBSERVE_TX = (byte)0x08;
	final byte CD = (byte)0x09;
	final byte RX_ADDR_P0 = (byte)0x0A;
	final byte RX_ADDR_P1 = (byte)0x0B;
	final byte RX_ADDR_P2 = (byte)0x0C;
	final byte RX_ADDR_P3 = (byte)0x0D;
	final byte RX_ADDR_P4 = (byte)0x0E;
	final byte RX_ADDR_P5 = (byte)0x0F;
	final byte TX_ADDR = (byte)0x10;
	final byte RX_PW_P0 = (byte)0x11;
	final byte RX_PW_P1 = (byte)0x12;
	final byte RX_PW_P2 = (byte)0x13;
	final byte RX_PW_P3 = (byte)0x14;
	final byte RX_PW_P4 = (byte)0x15;
	final byte RX_PW_P5 = (byte)0x16;
	final byte FIFO_STATUS = (byte)0x17;
	final byte DYNPD = (byte)0x1C;
	final byte FEATURE = (byte)0x1D;

	//Data
	private final byte[] RXTX_ADDR = { (byte)0x87, (byte)0xC1, (byte)0xB9 }; //Randomly chosen address

	public void initializeNRF24L01() {
		setCsnPin(true);
		CE_nRF24L01.low();

		// wait for the nRF24L01 to set up
		try { Thread.sleep(20); }
		catch (InterruptedException e) { e.printStackTrace(); }
		regWrite(CONFIG, (byte)0x0B); //RX mode
		regWrite(EN_AA, (byte)0x00);
		regWrite(EN_RXADDR, (byte)0x01);
		regWrite(SETUP_AW, (byte)0x01);
		regWrite(SETUP_RETR, (byte)0x00);
		regWrite(RF_CH, (byte)0x01); //RADIO CHANNEL
		regWrite(RF_SETUP, (byte)0x26); //250kbps, 0dBm = 0x26 250kbps, 0dBm (1mW) = 0x06
		regWrite(RX_PW_P0, (byte)0x0A); //Payload size = 10 byte

		addrWrite(RX_ADDR_P0, (byte)3, RXTX_ADDR);
		addrWrite(TX_ADDR, (byte)3, RXTX_ADDR);

		//System.out.println(ReadRegister(TX_ADDR));

		flushBuffers();

		// wait for the nRF24L01 to set up
		try { Thread.sleep(2); }
		catch (InterruptedException e) { e.printStackTrace(); }
		System.out.println("Initialized");
	}

	public void setRXMode() {
		regWrite(CONFIG, (byte)0x0B);
		CE_nRF24L01.high();
	}

	private void setTXMode() {
		CE_nRF24L01.low();
		regWrite(CONFIG, (byte)0x0A);

		CE_nRF24L01.high();
		long currentTime = System.nanoTime();
		while (System.nanoTime() - currentTime < 15000) {

		}
		CE_nRF24L01.low();
	}

	private void powerDownNRF() {
		CE_nRF24L01.low();
		regWrite(CONFIG, (byte)0);
	}

	public String getPacket() {
		byte[] data = new byte[10];
		getPayload((byte)10, data);

		regWrite(STATUS, (byte)0x40);
		return new String(data);
	}

	public byte signalStrength() {
		return regRead(CD);
	}

	public void setPayloadContent(String payload) {
		setCsnPin(false);
		byte packet[] = new byte[payload.length() + 1];
		packet[0] = TX_PAYLOAD;

		for (int cnt = 0; cnt < payload.length(); cnt++)
			packet[cnt + 1] = ((byte)payload.charAt(cnt));

		try {spi.write(packet);}catch (IOException e) {e.printStackTrace();}
		setCsnPin(true);
	}

	public void sendStringNRF(String packet) {
		setPayloadContent(packet);

		setTXMode();
		byte stat;
		do {
			stat = regRead(STATUS);
			//System.out.println(Integer.toBinaryString(stat));
			//try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
		} while ((stat & 0x20) == 0);
		//System.out.println("SENT");

		regWrite(STATUS, (byte)0x20);
		setRXMode();
	}

	public boolean packetAvail() {
		byte stat = regRead(STATUS);
		//System.out.println(Integer.toBinaryString(stat));
		return (stat & 0x40) != 0;
	}

	public void printRegisters() {
		for (byte i=0; i<29; i++) {
			setCsnPin(false);

			//System.out.println(transfer((byte)(i)));
			byte packet[] = new byte[2];
			packet[0] = i;
			packet[1] = 0x00;
			byte[] result = new byte[2];
			try {result = spi.write(packet);}catch (IOException e) {e.printStackTrace();}
			//byte result = packet[1];

			//byte result = transfer((byte)0x00);
			setCsnPin(true);
			System.out.print("reg ");
			System.out.print(i);
			System.out.print(": ");
			//System.out.print(Integer.toBinaryString(result));
			System.out.println(Integer.toBinaryString( (result[1]) ));
		}
	}

	public void flushBuffers() {
		regWrite(STATUS, (byte)0x70);
		sendCommand(FLUSH_RX);
		sendCommand(FLUSH_TX);
	}

	private void regWrite(byte reg, byte val) {
		setCsnPin(false);
		byte packet[] = new byte[2];
		packet[0] = (byte)(W_REG | reg);
		packet[1] = val;
		try {spi.write(packet);}catch (IOException e) {e.printStackTrace();}
		setCsnPin(true);
	}

	private void addrWrite(byte regNum, byte len, byte[] address) {
		setCsnPin(false);
		byte packet[] = new byte[len + 1];
		packet[0] = (byte)(W_REG | regNum);

		for (int cnt = 0; cnt < len; cnt++)
			packet[cnt + 1] = ((byte)address[cnt]);

		try {spi.write(packet);}catch (IOException e) {e.printStackTrace();}
		setCsnPin(true);
	}

	public byte regRead(byte regNum) {
		setCsnPin(false);
		byte packet[] = new byte[2];
		packet[0] = regNum;
		packet[1] = 0x00;
		byte[] result = new byte[2];
		try {result = spi.write(packet);}catch (IOException e) {e.printStackTrace();}
		setCsnPin(true);
		return result[1];
	}

	private void sendCommand(byte command) {
		setCsnPin(false);
		byte packet[] = new byte[1];
		packet[0] = command;
		try {spi.write(packet);}catch (IOException e) {e.printStackTrace();}
		setCsnPin(true);
	}

	private void setPayloadContent(byte num, byte[] data) {
		setCsnPin(false);

		byte packet[] = new byte[num + 1];
		packet[0] = (byte)(TX_PAYLOAD);

		for (int cnt = 0; cnt < num; cnt++)
			packet[cnt + 1] = ((byte)data[cnt]);

		try {spi.write(packet);}catch (IOException e) {e.printStackTrace();}

		setCsnPin(true);

		CE_nRF24L01.high();
		long startTime = System.nanoTime();
		while(startTime + 150 >= System.nanoTime());
		CE_nRF24L01.low();
	}

	private String getPayload(byte len, byte[] data) {
		//byte[] data = new byte[num];
		setCsnPin(false);


		byte packet[] = new byte[len + 1];
		packet[0] = (byte)(RX_PAYLOAD);

		for (int cnt = 0; cnt < len; cnt++)
			packet[cnt + 1] = ((byte)0);

		byte[] result = new byte[len + 1];
		try {result = spi.write(packet);} catch (IOException e) {e.printStackTrace();}

		String stringResult = "";
		for (int cnt = 1; cnt <= len; cnt++) {
			stringResult += (char)result[cnt];
			data[cnt - 1] = result[cnt];
		}

		setCsnPin(true);

		return stringResult;
	}

	private GpioPinDigitalOutput csnGPIO = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07);
	private void setCsnPin(boolean state) {
		if (!state) csnGPIO.low();
		if (state) csnGPIO.high();
	}
}
