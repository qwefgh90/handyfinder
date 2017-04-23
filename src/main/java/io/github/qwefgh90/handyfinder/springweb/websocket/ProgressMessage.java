package io.github.qwefgh90.handyfinder.springweb.websocket;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * singleton class
 * Command pattern
 * @author choechangwon
 *
 */
@JsonIgnoreProperties({ "receiver" })
public class ProgressMessage implements IMessage {
	private final static Logger LOG = LoggerFactory.getLogger(ProgressMessage.class);
	public enum STATE {
		START, PROGRESS, TERMINATE, PREPARE
	}

	private STATE state;	//START , PROGRESS , TERMINATE
	private int processIndex; // integer starting from ZERO
	private Path processPath; // path string
	private int totalProcessCount; // integer
	private IMessageSender sender;

	private ProgressMessage() {
	}
	
	public static ProgressMessage createMessage(IMessageSender sender, ProgressMessage.STATE state, int successCount, Path path, int totalCount) {
		ProgressMessage command = new ProgressMessage();
		command.sender = sender;
		command.processIndex = successCount;
		command.totalProcessCount = totalCount;
		command.state = state;
		return command;
	}

	@Override
	public void send() {
		switch (this.state) {
		case PREPARE:
			sender.sendToProgressChannel(this);
			break;
		case START:
			sender.sendToProgressChannel(this);
			break;
		case PROGRESS:
			sender.sendToProgressChannel(this);
			break;
		case TERMINATE:
			sender.sendToProgressChannel(this);
			break;
		default:
			break;
		}
	}

	public STATE getState() {
		return state;
	}

	public void setState(STATE state) {
		this.state = state;
	}

	public int getProcessIndex() {
		return processIndex;
	}

	public void setProcessIndex(int processIndex) {
		this.processIndex = processIndex;
	}

	public Path getProcessPath() {
		return processPath;
	}

	public void setProcessPath(Path processPath) {
		this.processPath = processPath;
	}

	public int getTotalProcessCount() {
		return totalProcessCount;
	}

	public void setTotalProcessCount(int totalProcessCount) {
		this.totalProcessCount = totalProcessCount;
	}

	public IMessageSender getReceiver() {
		return sender;
	}

	public void setReceiver(IMessageSender receiver) {
		this.sender = receiver;
	}

}
