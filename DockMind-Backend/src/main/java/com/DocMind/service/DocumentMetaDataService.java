package com.DocMind.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.DocMind.config.AppProperties;
import com.DocMind.dto.DocumentMetaDataDto;
import com.DocMind.dto.DocumentResponseDto;
import com.DocMind.entity.DocumentMetaData;
import com.DocMind.entity.DocumentStatus;
import com.DocMind.entity.User;
import com.DocMind.exception.DocumentProcessingException;
import com.DocMind.exception.ResourceNotFoundException;
import com.DocMind.repository.DocumentMetaDataRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMetaDataService {

	private final DocumentMetaDataRepo documentMetaDataRepo;
	private final JdbcTemplate jdbcTemplate;
	private final DocumentParserService parserService;
	private final DocumentIngestionService ingestService;
	private final ModelMapper modelMapper;
	private final VectorStore vectorStore;
	private final AppProperties appProperties;
	
	
	
    @Transactional
	public DocumentResponseDto uploadAndProcess(MultipartFile file, User user) {
		
		String fileName=file.getOriginalFilename()!=null?file.getOriginalFilename():"document";
		String contentType=file.getContentType()!=null?file.getContentType():"application/octat-stream";
		
		//Document meta data create
		
			//storing user details with document meta data
		DocumentMetaData documentMetaData=DocumentMetaData.builder()
				.fileName(fileName)
				.contentType(contentType)
				.status(DocumentStatus.UPLOADING)
				.createdAt(LocalDateTime.now())
				.fileSize(file.getSize())
				.user(user)
				.build();
		
		//saving document meta data
		 documentMetaData=documentMetaDataRepo.save(documentMetaData);
		 List<Document>parseDocs=null;
		 int chunksCreated=0;
		 
		 try {
			 
			 	//parse the file
				parseDocs=parserService.parse(file);
				
				//ingest service
				chunksCreated=ingestService.ingest(documentMetaData, parseDocs);
				
		 }catch(DocumentProcessingException e){
			 log.info("Document meta data deleteing due to fail processing");
			 documentMetaDataRepo.delete(documentMetaData);
			 throw e;
		 }
		
		
		
		return DocumentResponseDto.builder()
				.id(documentMetaData.getId())
				.fileName(documentMetaData.getFileName())
				.fileSize(documentMetaData.getFileSize())
				.chunksCreated(chunksCreated)
				.status(documentMetaData.getStatus())
				.message("Document Successfully processed and indexed")
				.userId(documentMetaData.getUser().getId())
				.build();
		
	}
	public List<DocumentResponseDto> uploadMultipleDocuments(List<MultipartFile> files, User user) {
		
		List<DocumentResponseDto>responseDtos=new ArrayList<>();
		
		for(MultipartFile file:files) {
			DocumentResponseDto result=this.uploadAndProcess(file, user);
			responseDtos.add(result);
		}
		return responseDtos;
	}
	
	
	// to get all the documents: This method is useful for admin
	public List<DocumentMetaDataDto> getAllDocuments() {
		List<DocumentMetaData>allDocuments=documentMetaDataRepo.findAllByOrderByCreatedAtDesc();
		return allDocuments.stream()
					.map(documentMetaData->modelMapper.map(documentMetaData, DocumentMetaDataDto.class))
					.toList();
		
	}
	
	
	public List<DocumentMetaDataDto> getAllDocumentsByUser(User user){
		
		List<DocumentMetaData>documents=documentMetaDataRepo.findByUserOrderByCreatedAtDesc(user);
		
		return documents.stream()
				.map(documentMetaData->modelMapper.map(documentMetaData, DocumentMetaDataDto.class))
				.toList();
	}
	
	public DocumentMetaDataDto getDocumentById(String id) {
        DocumentMetaData documentMetaData = documentMetaDataRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));
        return modelMapper.map(documentMetaData, DocumentMetaDataDto.class);


    }
	
	@Transactional
	public void deleteDocument(String id) {
	    DocumentMetaData documentMetaData = documentMetaDataRepo.findById(id)
	            .orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));

	    // 1. Delete relational metadata from MySQL
	    documentMetaDataRepo.delete(documentMetaData);

	    // 2. Retrieve chunk vector IDs matching this documentId and delete them
	    try {
	    	
	    	// Query with empty string without filter expressions
	        SearchRequest searchRequest = SearchRequest.query("")
	                .withTopK(appProperties.getRag().getMaxNumChunks());
	                //.withFilterExpression("documentId = '" + id + "'");

	        List<Document> matchedChunks = vectorStore.similaritySearch(searchRequest);

//	        if (!matchedChunks.isEmpty()) {
//	            List<String> chunkIdsToDelete = matchedChunks.stream()
//	                    .map(Document::getId)
//	                    .toList();
	            
	        //Modified
	        
	            List<String> chunkIdsToDelete = matchedChunks.stream()
	                    .filter(doc -> {
	                        Object docId = doc.getMetadata().get("documentId");
	                        return docId != null && id.equals(docId.toString());
	                    })
	                    .map(Document::getId)
	                    .toList();

	            // Delete chunks from memory
	           // vectorStore.delete(chunkIdsToDelete);

	            // Persist the updated store to vector_store.json
	            
	            //Modified
	            if (!chunkIdsToDelete.isEmpty()) {
	                vectorStore.delete(chunkIdsToDelete);

	                if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
	                    simpleVectorStore.save(new File(appProperties.getRag().getVectorStorePath()));
	                }

	                log.info("Deleted {} vector chunks from vector store for document id: {}", chunkIdsToDelete.size(), id);
	            }
	       // }
	    } catch (Exception e) {
	        log.warn("Could not delete vectors from vector store: {}", e.getMessage());
	    }
	}
	

}
