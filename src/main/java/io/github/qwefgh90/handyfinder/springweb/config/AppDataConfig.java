package io.github.qwefgh90.handyfinder.springweb.config;

//import static io.github.qwefgh90.handyfinder.springweb.config.akka.SpringExtension.SpringExtProvider;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.BasicOption;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.MimeOption;
import io.github.qwefgh90.handyfinder.lucene.MimeOption.MimeXmlObjectFactory;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;

@Configuration
public class AppDataConfig {

	@Autowired
	private ApplicationContext applicationContext;

	@Bean
	public LuceneHandler luceneHandler(CommandInvoker invoker, BasicOption basicOption
			,MimeOption mimeOption) {
		return LuceneHandler
				.getInstance(AppStartupConfig.pathForIndex, invoker, basicOption, mimeOption);
	}

	@Bean
	public BasicOption basicOption(){
		return BasicOption.getInstance(AppStartupConfig.appDataJsonPath);
	}

	@Bean
	public MimeOption mimeOption() throws ParserConfigurationException, SAXException, IOException {
		return MimeXmlObjectFactory.getInstanceFromXml(AppStartupConfig.tikaXmlFilePath.toAbsolutePath().toString()
				,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
	}
	
	/**
	 * Actor system singleton for this application.
	 */
	/*
	@Bean
	public ActorSystem actorSystem() {
		ActorSystem system = ActorSystem.create("AkkaJavaSpring");
		// initialize the application context in the Akka Spring Extension
		SpringExtProvider.get(system).initialize(applicationContext);
		return system;
	}

	@Bean
	public ActorRef indexActor(ActorSystem actorSystem){
		return actorSystem.actorOf(
				SpringExtProvider.get(actorSystem).props("IndexActor"), "indexActor");	
	}
	
	@Autowired 
	ActorSystem actorSystem;
	
	@PreDestroy
	public void destroy(){
		scala.concurrent.Future<Terminated> future = actorSystem.terminate();
	}
	*/
}

