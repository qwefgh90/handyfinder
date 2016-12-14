package io.github.qwefgh90.handyfinder.springweb.config;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

import io.github.qwefgh90.handyfinder.lucene.BasicOption;
import io.github.qwefgh90.handyfinder.gui.AppStartupConfig;
import io.github.qwefgh90.handyfinder.lucene.LuceneHandler;
import io.github.qwefgh90.handyfinder.lucene.MimeOption;
import io.github.qwefgh90.handyfinder.lucene.MimeOption.MimeXmlObjectFactory;
import io.github.qwefgh90.handyfinder.springweb.repository.MetaRespository;
import io.github.qwefgh90.handyfinder.springweb.websocket.CommandInvoker;

@Configuration
public class AppDataConfig {

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
}
