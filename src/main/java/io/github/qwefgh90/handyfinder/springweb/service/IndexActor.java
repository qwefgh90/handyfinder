package io.github.qwefgh90.handyfinder.springweb.service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import akka.actor.UntypedActor;
import akka.util.Timeout;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.model.Directory;
import io.github.qwefgh90.handyfinder.springweb.repository.MetaRespository;
import scala.concurrent.duration.Duration;

@Named("IndexActor")
@Scope("prototype")
public class IndexActor extends UntypedActor {

	private final static Logger LOG = LoggerFactory
			.getLogger(IndexActor.class);

	public static class Restart{

	}

	@Autowired
	MetaRespository indexProperty;

	final LuceneHandler luceneHandler;
	final ExecutorService executorService;
	final Runnable indexRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				List<Directory> list = indexProperty.selectDirectory();
				if (luceneHandler.isReady()){
					luceneHandler.startIndex(list);
					luceneHandler.updateIndexedDocuments(list);
				}
			} catch (IOException e) {
				LOG.warn(ExceptionUtils.getStackTrace(e));
			}
		}
	};

	@Autowired
	public IndexActor(LuceneHandler luceneHandler) {
		this.luceneHandler = luceneHandler;
		this.executorService = Executors.newSingleThreadExecutor();
		this.context().system().scheduler().scheduleOnce(Duration.create(3000, TimeUnit.MILLISECONDS), () -> {
			//if reset file exists, remove all indexes.
			if(Files.exists(AppStartupConfig.resetFilePath)){
				try {
					luceneHandler.deleteAllIndexesFromFileSystem();
					Files.delete(AppStartupConfig.resetFilePath);
				} catch (IOException e) {
					LOG.warn(ExceptionUtils.getStackTrace(e));
				}
			}
			//on startup, start indexing.
			self().tell(new Restart(), null);
			
		}, this.context().system().dispatcher());
	}
	
	@Override
	public void postStop() throws Exception {
		luceneHandler.stopIndex();
		executorService.shutdown();
		executorService.awaitTermination(20, TimeUnit.SECONDS);
		LuceneHandler.closeResources();
		super.postStop();
	}
	
	@Override
	public void onReceive(Object message) throws Throwable {
		if(message instanceof Restart){
			luceneHandler.stopIndex();
			executorService.submit(indexRunnable);
		}
	}
}
