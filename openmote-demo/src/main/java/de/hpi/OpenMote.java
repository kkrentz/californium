package de.hpi;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class OpenMote implements CoapHandler {

    private CoapClient pingCoapClient;
    private Map<Led, CoapClient> ledCoapClients;
    private CoapClient sht21CoapClient;
    private CoapClient max44009CoapClient;
    private CoapClient buttonCoapClient;
    private CoapObserveRelation coapObserveRelation;
    private ButtonObserver buttonObserver;

    public OpenMote(String ipv6Address) throws URISyntaxException {
        String baseUri = "coap://[" + ipv6Address + "]:5683/";
        pingCoapClient = new CoapClient(new URI(baseUri));
        ledCoapClients = new HashMap<Led, CoapClient>();
        ledCoapClients.put(Led.RED, new CoapClient(new URI(baseUri + "actuators/leds?color=r")));
        ledCoapClients.put(Led.GREEN, new CoapClient(new URI(baseUri + "actuators/leds?color=g")));
        ledCoapClients.put(Led.BLUE, new CoapClient(new URI(baseUri + "actuators/leds?color=b")));
        sht21CoapClient = new CoapClient(new URI(baseUri + "sensors/sht21"));
        max44009CoapClient = new CoapClient(new URI(baseUri + "sensors/max44009"));
        buttonCoapClient = new CoapClient(new URI(baseUri + "sensors/button"));
    }

    public boolean ping(long timeout_in_ms) {
        return pingCoapClient.ping(timeout_in_ms);
    }

    public void toggleLed(Led led) {
    	ledCoapClients.get(led).post("mode=toggle", MediaTypeRegistry.TEXT_PLAIN);
    }

    public Double senseTemperature() {
        CoapResponse coapResponse;
        String responseText;

        coapResponse = sht21CoapClient.get();
        if(coapResponse == null) {
            return null;
        }
        responseText = coapResponse.getResponseText();

        return new Double(responseText.split(";")[0])/100;
    }

    public Double senseHumidity() {
        CoapResponse coapResponse;
        String responseText;

        coapResponse = sht21CoapClient.get();
        if(coapResponse == null) {
            return null;
        }
        responseText = coapResponse.getResponseText();

        return new Double(responseText.split(";")[1])/100;
    }

    public Double senseLight() {
    	CoapResponse coapResponse;

        coapResponse = max44009CoapClient.get();
        if(coapResponse == null) {
            return null;
        }

        return new Double(coapResponse.getResponseText());
    }
    
    public void registerForButtonPresses(ButtonObserver buttonObserver) {
    	if(coapObserveRelation == null) {
    		coapObserveRelation = buttonCoapClient.observe(this);
    	}
    	this.buttonObserver = buttonObserver;
    }

    @Override
	public void onLoad(CoapResponse response) {
		if(response.getResponseText().equals("0")) {
			buttonObserver.onReleased();
		} else {
			buttonObserver.onPressed();
		}
	}

	@Override
	public void onError() {
		
	}

	public static void main(String[] args) {
		OpenMote openMote;

		try {
			openMote = new OpenMote("fd00::212:4b00:430:53c0");
			openMote.toggleLed(Led.RED);
			openMote.toggleLed(Led.GREEN);
			openMote.toggleLed(Led.BLUE);
			System.out.println("Temperature: " + openMote.senseTemperature() + "°C");
			System.out.println("Humidity: " + openMote.senseHumidity() + "%");
			System.out.println("Light: " + openMote.senseLight() + "?");
			openMote.registerForButtonPresses(new ButtonObserver() {
		
				@Override
				public void onPressed() {
					System.out.println("onPressed");
				}
				
				@Override
				public void onReleased() {
					System.out.println("onReleased");
				}
			});
			System.in.read();
		} catch (URISyntaxException e) {
			System.err.println("Invalid URI: " + e.getMessage());
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
