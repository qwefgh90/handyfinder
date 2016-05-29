package com.qwefgh90.io.handyfinder.sax;

import java.util.ArrayList;
import java.util.List;

import org.apache.tika.mime.MimeTypes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * not-thread-safe parser of xml below link
 * http://grepcode.com/file/repo1.maven.org/maven2/org.apache.tika/tika-core/1.9
 * /org/apache/tika/mime/tika-mimetypes.xml?av=f
 * 
 * @author choechangwon
 *
 */
public class TikaMimeTypesSaxHandler extends DefaultHandler {
	/*
	 * <mime-type type="text/x-asciidoc"> 
	 * <_comment>Asciidoc source
	 * code
	 * </_comment> 
	 * <glob pattern="*.asciidoc"/> 
	 * <glob pattern="*.adoc"/>
	 * <glob pattern="*.ad"/> 
	 * <glob pattern="*.ad.txt"/> 
	 * <glob pattern="*.adoc.txt"/> 
	 * <sub-class-of type="text/plain"/> 
	 * </mime-type>
	 */
	
	public TikaMimeTypesSaxHandler(TikaMimeXmlObject xmlObject){
		this.xmlObject = xmlObject;
	}
	
	TikaMimeXmlObject xmlObject;
	
	private enum TikaMimeEnum{
		MIME_TYPE_TAG("mime-type",Boolean.TRUE),
		GLOB_TAG("glob",Boolean.FALSE),
		MIME_TYPE_TYPE_ATTR("type",null),
		GLOB_PATTERN_ATTR("pattern",null);
		private final String nameInGrammar;
		private final Boolean slashNeed;
		private TikaMimeEnum(String name, Boolean slashNeed) {
			this.nameInGrammar = name;
			this.slashNeed = slashNeed;
		}
		public String getNameInGrammar() {
			return nameInGrammar;
		}
		public Boolean getSlashNeed() {
			return slashNeed;
		}
		
	}

	//boolean mimeTypeTagStartFlag = false;
	//boolean globTagStartFlag = false;
	String currentType = null;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (TikaMimeEnum.MIME_TYPE_TAG.getNameInGrammar().equals(qName)) {
			currentType = attributes.getValue(TikaMimeEnum.MIME_TYPE_TYPE_ATTR.getNameInGrammar());
		}
		if (TikaMimeEnum.GLOB_TAG.getNameInGrammar().equals(qName)) {
			String glob = attributes.getValue(TikaMimeEnum.GLOB_PATTERN_ATTR.getNameInGrammar());
			xmlObject.addGlobType(currentType, glob);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}

}
