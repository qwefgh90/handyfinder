package com.qwefgh90.io.handyfinder.springweb.config;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import com.qwefgh90.io.handyfinder.gui.AppStartupConfig;
import com.qwefgh90.io.handyfinder.springweb.websocket.CommandInvoker;

import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerBasicOptionView;
import io.github.qwefgh90.handyfinder.lucene.ILuceneHandlerBasicOption;
import io.github.qwefgh90.handyfinder.lucene.ILuceneHandlerMimeOption;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandlerOption;
import io.github.qwefgh90.handyfinder.lucene.TikaMimeXmlObject;
import io.github.qwefgh90.handyfinder.lucene.TikaMimeXmlObject.TikaMimeXmlObjectFactory;

@Configuration
public class AppDataConfig {

	@Bean
	public LuceneHandler luceneHandler(CommandInvoker invoker, LuceneHandlerOption option) {
		return LuceneHandler
				.getInstance(AppStartupConfig.pathForIndex, invoker, option);
	}

	@Bean
	public LuceneHandlerOption luceneHandlerOption(ILuceneHandlerBasicOption basicOption,
			ILuceneHandlerMimeOption mimeOption){
		return new LuceneHandlerOption(basicOption, mimeOption);
	}
	
	@Bean
	public LuceneHandlerBasicOptionView globalAppDataView() {
		LuceneHandlerBasicOptionView view =  LuceneHandlerBasicOptionView.getInstance();
		return view;
	}

	@Bean
	public TikaMimeXmlObject tikaMimeXmlObject() throws ParserConfigurationException, SAXException, IOException {
		return TikaMimeXmlObjectFactory.getInstanceFromXml(AppStartupConfig.tikaXmlFilePath.toAbsolutePath().toString()
				,AppStartupConfig.propertiesPath, AppStartupConfig.customTikaGlobPropertiesPath);
	}
}
