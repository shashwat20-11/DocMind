package com.DocMind.service;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.DocMind.exception.DocumentProcessingException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentParserService {

	public List<Document> parse(MultipartFile file) {
		
		String fileName=file.getOriginalFilename()!=null?file.getOriginalFilename():"document";
		String contentType=file.getContentType()!=null?file.getContentType():"application/octat-stream";
		
		log.info("Parsing document: {}, size: {}, contentType: {},",fileName,file.getSize(),contentType);
		
		try {
			Resource resource=new ByteArrayResource(file.getBytes()) {
				
				@Override
				public @Nullable String getFilename() {
					return fileName;
				}
			};
			
			if(fileName.toLowerCase().endsWith(".pdf") || contentType.contains("pdf")) {
				return parsePdf(resource);
			}else {
				return parseGenericFile(resource);
			}
			
			
		}catch(IOException e){
			log.error("Failed to read file bytes: {}",fileName,e);
			throw new DocumentProcessingException("Could not read upload file : "+fileName,e);
		}catch(Exception e) {
			log.error("Error during document parsing: {}",fileName,e);
			throw new DocumentProcessingException("Failed to parse document content : "+fileName,e);
		}
		
	}

	private List<Document> parsePdf(Resource resource) {

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageBottomMargin(0)
                .withPageTopMargin(0)
                .build();

        PagePdfDocumentReader documentReader = new PagePdfDocumentReader(resource, config);
        return documentReader.read();
    }

    private List<Document> parseGenericFile(Resource resource) {
        TikaDocumentReader tikaDocumentReader= new TikaDocumentReader(resource);
        return tikaDocumentReader.read();
    }

}
