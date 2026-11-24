package com.DocMind.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.DocMind.config.AppProperties;
import com.DocMind.entity.DocumentMetaData;
import com.DocMind.entity.DocumentStatus;
import com.DocMind.exception.DocumentProcessingException;
import com.DocMind.repository.DocumentMetaDataRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionService {
		
	private final VectorStore vectorStore;
	private final DocumentMetaDataRepo documentMetaDataRepo;
	private final AppProperties appProperties;
	
	public int ingest(DocumentMetaData documentMetaData, List<Document> parseDocs) {

		log.info("Ingesting document [id={}, name={}, pages={}]",
				documentMetaData.getId(),
				documentMetaData.getFileName(),
				parseDocs.size());
		
		try {
			documentMetaData.setStatus(DocumentStatus.PROCESSING);
			documentMetaData.setTotalPages(parseDocs.size());
			
		// 1. Text chunking using TokenTextSplitter
			
			TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
			        appProperties.getRag().getChunkSize(),              // defaultChunkSize (e.g. 700)
			        appProperties.getRag().getMinChunkSizeChars(),       // minChunkSizeChars (e.g. 350)
			        appProperties.getRag().getMinChunkLengthToEmbed(),   // minChunkLengthToEmbed (e.g. 5)
			        appProperties.getRag().getMaxNumChunks(),            // maxNumChunks (e.g. 1000)
			        true                                                 // keepSeparator
			);
			
			List<Document> chunks = tokenTextSplitter.apply(parseDocs);
			if (chunks.isEmpty()) {
				documentMetaData.setStatus(DocumentStatus.FAILED);
				documentMetaData.setErrorMessage("Documents appears to be empty or unscannable");
				documentMetaDataRepo.save(documentMetaData);
				return 0;
			}
		
		// 2. Metadata enrichment on each chunk
			
			List<Document> enrichedChunks = new ArrayList<>();
			for (int i = 0; i < chunks.size(); i++) {
				Document chunk = chunks.get(i);
				Map<String, Object> enrichedMetaData = new HashMap<>(chunk.getMetadata());
				enrichedMetaData.put("documentId", documentMetaData.getId());
				enrichedMetaData.put("fileName", documentMetaData.getFileName());
				enrichedMetaData.put("contentType", documentMetaData.getContentType());
				enrichedMetaData.put("chunkIndex", i);
				//storing chunks for user base documents
				enrichedMetaData.put("userId", documentMetaData.getUser().getId());
				// Preserve or calculate page number if available
				Object pageNumber = chunk.getMetadata().get("page_number");
				if (pageNumber == null) {
					pageNumber = chunk.getMetadata().get("pageNumber");
				}
				if (pageNumber != null) {
					enrichedMetaData.put("pageNumber", pageNumber);
				}
				
				Document enrichedDoc = new Document(chunk.getContent(), enrichedMetaData);
				enrichedChunks.add(enrichedDoc);
			}
			
		// 3. Write chunks and embedding to Vector Store and persist to JSON
			
			log.info("Writing {} vector chunks to vector store for document: {}", enrichedChunks.size(), documentMetaData.getFileName());
			vectorStore.add(enrichedChunks);

			// Persists the in-memory vectors directly into vector_store.json
			// Thread-safe persistence: save updated in-memory vectors to local JSON file
			if (vectorStore instanceof SimpleVectorStore simpleStore) {
			    synchronized (simpleStore) {
			        simpleStore.save(new File(appProperties.getRag().getVectorStorePath()));
			    }
			}
			
		// 4. Update document status to index
			
			documentMetaData.setStatus(DocumentStatus.INDEXED);
			documentMetaData.setTotalChunks(enrichedChunks.size());
			documentMetaData.setErrorMessage(null);
			
			documentMetaDataRepo.save(documentMetaData);
			log.info("Successfully indexed document [id ={}, name ={}, chunks ={}]", documentMetaData.getId(), documentMetaData.getFileName(), enrichedChunks.size());
			return enrichedChunks.size();
		
		} catch (Exception ex) {
			log.error("Failed to ingest document into vector store: {}", documentMetaData.getFileName(), ex);
			documentMetaData.setStatus(DocumentStatus.FAILED);
			documentMetaData.setErrorMessage(ex.getMessage());
			documentMetaDataRepo.save(documentMetaData);
			throw new DocumentProcessingException("Failed to index document: " + ex.getMessage(), ex);
		}
	}
}