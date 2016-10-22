package io.github.qwefgh90.handyfinder.springweb.config;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.ILuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.lucene.ILuceneHandlerMimeOptionView;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerMimeOptionView;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerMimeOptionView.TikaMimeXmlObjectFactory;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;

@Configuration
public class AppDataConfig {

	@Bean
	public LuceneHandler luceneHandler(CommandInvoker invoker, LuceneHandlerBasicOptionView basicOption
			,LuceneHandlerMimeOptionView mimeOption) {
		return LuceneHandler
				.getInstance(AppStartupConfig.pathForIndex, invoker, basicOption, mimeOption);
	}

	@Bean
	public LuceneHandlerBasicOptionView luceneHandlerBasicOptionView(){
		return LuceneHandlerBasicOptionView.getInstance(AppStartupConfig.appDataJsonPath);
	}

	@Bean
	public LuceneHandlerMimeOptionView tikaMimeXmlObject() throws ParserConfigurationException, SAXException, IOException {
		return TikaMimeXmlObjectFactory.getInstanceFromXml(AppStartupConfig.tikaXmlFilePath.toAbsolutePath().toString()
				,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
	}
}
