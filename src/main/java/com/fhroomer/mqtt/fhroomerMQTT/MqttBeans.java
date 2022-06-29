package com.fhroomer.mqtt.fhroomerMQTT;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.*;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@Configuration
public class MqttBeans {
	
	private static String mqttUsername = "fhroomer", mqttPassword = "fhroomer", roomTopic = "ef42/e220";
	private static int numberPeopleInRoom = 0, capacityOfPeopleInRoom = 30;
	
	public MqttPahoClientFactory mqttClientFactory() {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		MqttConnectOptions options = new MqttConnectOptions();
		
		options.setServerURIs(new String[] {"tcp://localhost:1883"});
		options.setUserName(mqttUsername);
		options.setPassword(mqttPassword.toCharArray());
		options.setCleanSession(true);
		
		factory.setConnectionOptions(options);
		
		return factory;
	}
	
	@Bean
	public MessageChannel mqttInputChannel() {
		return new DirectChannel();
	}
	
	@Bean
	public MessageProducer inbound() {
		MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter("serverIn", mqttClientFactory(), roomTopic);
		
		adapter.setCompletionTimeout(5000);
		adapter.setConverter(new DefaultPahoMessageConverter());
		adapter.setQos(2);
		adapter.setOutputChannel(mqttInputChannel());
		return adapter;
	}
	
	@Bean
	@ServiceActivator(inputChannel = "mqttInputChannel")
	public MessageHandler handler() {
		return new MessageHandler() {
			
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
				if(topic.equals(roomTopic)) {
					
					if(message.getPayload().toString().equals("Person+")) {
						System.out.println("Someone Entered the Room!");
						checkNumberOfPeople(1);
					}
					else {
						System.out.println("Someone Left the Room!");
						checkNumberOfPeople(-1);
					}
				}
				System.out.println("Number of Person inside of Room "+roomTopic+": "+numberPeopleInRoom+
						"\nNumber of Person allowed: "+capacityOfPeopleInRoom);
				System.out.println(message.getPayload());
			}
		};
	}
	
	@Bean
	public MessageChannel mqttOutboundChannel() {
		return new DirectChannel();
	}
	
	@Bean
	@ServiceActivator(inputChannel = "mqttOutboundChannel")
	public MessageHandler mqttOutbound() {
		MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler("serverOut", mqttClientFactory());
		
		messageHandler.setAsync(true);
		messageHandler.setDefaultTopic("fhroomer");
		
		return messageHandler;
	}
	
	private void checkNumberOfPeople(int input) {
		if(numberPeopleInRoom > capacityOfPeopleInRoom) {
			System.out.println("ERROR! TOO MANY PEOPLE IN ROOM" + roomTopic + "!");
		}
		else {
			numberPeopleInRoom = ((numberPeopleInRoom+input)<0)?0:numberPeopleInRoom;
		}
	}
}
