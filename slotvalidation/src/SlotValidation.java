
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class SlotValidation {

	private static GpioPinDigitalOutput sensorTriggerPin;
	private static GpioPinDigitalInput sensorEchoPin;
	final static GpioController gpio = GpioFactory.getInstance();
	public static String trigger;
	public static String echo;
	public static String RFID;
	public static String red;
	public static String green;
	public static String ParkingSlot;
	public static GpioPinDigitalOutput redLED;
	public static GpioPinDigitalOutput greenLED;

	public static void main(String[] args) throws IOException {
		new SlotValidation().SensorConnection();

	}

	public void SensorConnection() throws IOException {
		Properties p = new Properties();
		InputStream is = new FileInputStream("/home/pi/IOT-NEW/configuration.txt");
		p.load(is);
//		trigger = p.getProperty("ParkingTrigger");
//		echo = p.getProperty("ParkingEcho");
//		red = p.getProperty("ParkingRed");
//		green = p.getProperty("ParkingGreen");
//		RFID = p.getProperty("RFID");
		ParkingSlot = p.getProperty("ParkingSlot");
//		sensorTriggerPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByName("GPIO " + trigger)); // Trigger pin as
//																										// OUTPUT
//		sensorEchoPin = gpio.provisionDigitalInputPin(RaspiPin.getPinByName("GPIO " + echo),
//				PinPullResistance.PULL_DOWN); // Echo pin as INPUT
		sensorTriggerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_15);
		sensorEchoPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_16);
		redLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01,PinState.HIGH);
		greenLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04,PinState.LOW);
		int counter=1;

		while (true) {
			try {
				Thread.sleep(2000);
				sensorTriggerPin.high(); // Make trigger pin HIGH
				Thread.sleep((long) 0.01);// Delay for 10 microseconds
				sensorTriggerPin.low(); // Make trigger pin LOW

				while (sensorEchoPin.isLow()) { // Wait until the ECHO pin gets HIGH

				}
				int startTime = (int) System.nanoTime(); // Store the current time to calculate ECHO pin HIGH time.
				while (sensorEchoPin.isHigh()) { // Wait until the ECHO pin gets LOW

				}
				int endTime = (int) System.nanoTime(); // Store the echo pin HIGH end time to calculate ECHO pin HIGH
														// time.
				float distance = (float) ((((endTime - startTime) / 1e3) / 2) / 29.1);
				
				if (distance >= 1 && distance <= 8 && counter==1) {
					System.out.println("Place your card to Read");
					Process pr = Runtime.getRuntime().exec("python Read.py");

					BufferedReader bri = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					String line;
					while ((line = bri.readLine()) != null) {
						System.out.print(line);
						System.out.println();
						break;
					}
					bri.close();
					String result = new SlotValidation().RfidValidation(line); // calling API functionality
					String response[] = result.split(" ");
					new SlotValidation().LEDConnection(response[0]); // making Green LED on
					counter++;
				}
				else {
					counter=1;
				}

				Thread.sleep(1000);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public String RfidValidation(String RFID) {
		String urlLink = "https://qn1v75kue5.execute-api.us-east-2.amazonaws.com/Smart-Parking/parkinghistory/park?RFID="
				+ RFID + "&ParkingSlotID=" + ParkingSlot;
		String output = null;
		try {
			URL url = new URL(urlLink);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			output = reader.readLine();
			output = output.replace("\"", "");
			System.out.println(output);
		}

		catch (IOException e) {
			System.out.println("error" + e);
			// e.printStackTrace();
		}
		return output;

	}

	public void LEDConnection(String response) throws InterruptedException {
		if (response.equalsIgnoreCase("open")) {
			greenLED.high();
			redLED.low();
			Thread.sleep(15000);
			greenLED.low();
			redLED.low();
		} else {
			greenLED.low();
			redLED.high();
			Thread.sleep(15000);
			greenLED.low();
			redLED.low();
		}

	}
}
