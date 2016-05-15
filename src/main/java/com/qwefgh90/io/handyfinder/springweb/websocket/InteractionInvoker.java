package com.qwefgh90.io.handyfinder.springweb.websocket;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * UI Command API
 * 
 * @author choechangwon
 *
 */
@Service
public class InteractionInvoker {
	
	@Autowired
	IInteractionReceiver receiver;
	
	ProgressCommand progress;
	
	
	synchronized public void updateProgress(int processIndex, Path processPath, int totalProcessCount){
		progress = ProgressCommand.getInstance(receiver);
		progress.setState(ProgressCommand.STATE.PROGRESS);
		progress.setProcessIndex(processIndex);
		progress.setProcessPath(processPath);
		progress.setTotalProcessCount(totalProcessCount);
		progress.execute();
	}
}
