package com.qwefgh90.io.handyfinder.springweb.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class InteractionReceiver implements IInteractionReceiver {

	@Autowired
	SimpMessagingTemplate messaging;

	@Override
	public void sendToProgressChannel(Object obj) {
		messaging.convertAndSend("/receiver/progress", obj);
	}

}
