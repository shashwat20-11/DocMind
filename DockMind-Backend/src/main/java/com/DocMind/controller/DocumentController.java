package com.DocMind.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.DocMind.dto.ApiResponse;
import com.DocMind.dto.DocumentMetaDataDto;
import com.DocMind.dto.DocumentResponseDto;
import com.DocMind.entity.User;
import com.DocMind.service.DocumentMetaDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(
		name="Document Management",
		description="Endpoints for uploading, listing and managing documents and their vector embeddings."
		)
public class DocumentController {

	private final DocumentMetaDataService documentService;
	@PostMapping(value="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary="upload and index a document(PDF, DOCX, TEXT, MD, CSV)",
			description="THis api is used to upload and index document file"
			
			)
	public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocuments(
												@RequestParam("file")MultipartFile file,
													Authentication authentication){
		
		User user=(User)authentication.getPrincipal();
		DocumentResponseDto documentResponseDto = this.documentService.uploadAndProcess(file,user);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.<DocumentResponseDto>builder()
						.success(true)
						.data(documentResponseDto)
						.timestamp(LocalDateTime.now())
						.message("Document uploaded and indexed successfully")
						.build()
						);
				
				
	}
	
//  Api to upload multiple documents.
	@PostMapping(value="/upload_multiple", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary="upload and index a multiple documents simultaneously",
			description="THis api is used to upload and index multiple documents"
			
			)
	public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> uploadMultiple(
												@RequestParam("files")List<MultipartFile>files, 
												Authentication authentication){
		
		User user=(User) authentication.getPrincipal();
		List<DocumentResponseDto>responseDtos=documentService.uploadMultipleDocuments(files, user);
		return ResponseEntity.status(HttpStatus.CREATED)
							 .body(
									 ApiResponse.<List<DocumentResponseDto>>builder()
									 
									 .message("Document uploaded successfully")
									 .success(true)
									 .timestamp(LocalDateTime.now())
									 .data(responseDtos)
									 .build()
									 
									 );
							 
							 
	}

	
	 @GetMapping("/user")
	    @Operation(summary = "List all uploaded documents and their indexing status of the logged user")
	 public ResponseEntity<ApiResponse<List<DocumentMetaDataDto>>>getAllDocumentsOfLoggedInUser(
			 											Authentication authentication){
		 
		 User user=(User)authentication.getPrincipal();
		 List<DocumentMetaDataDto> documents =documentService.getAllDocumentsByUser(user);
		 return ResponseEntity.ok(
	                ApiResponse.<List<DocumentMetaDataDto>>
	                                builder()
	                        .message("All documents is here")
	                        .success(true)
	                        .timestamp(LocalDateTime.now())
	                        .data(documents)
	                        .build()
	        );
		 
	 }
	
//  List all uploaded documents.
    //   Method for admin
	 
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	@Operation(
			summary="List all uploaded documents and their indexing status"	
			)
	public ResponseEntity<ApiResponse<List<DocumentMetaDataDto>>> getAllDocuments(){
		List<DocumentMetaDataDto>documents=documentService.getAllDocuments();
		return ResponseEntity.ok(
				ApiResponse.<List<DocumentMetaDataDto>>builder()
				 
				 .message("All documents is here")
				 .success(true)
				 .timestamp(LocalDateTime.now())
				 .data(documents)
				 .build()
				
				);
		
	}
	
	@GetMapping("/{id}")
	@Operation(
			summary="Get metadata of a specific document by id"	
			)
	public ResponseEntity<ApiResponse<DocumentMetaDataDto>>getDocumentById(@PathVariable String id){
		DocumentMetaDataDto document=documentService.getDocumentById(id);
		return ResponseEntity.ok(
				ApiResponse.<DocumentMetaDataDto>builder()
				 
				 .message("Single document is here")
				 .success(true)
				 .timestamp(LocalDateTime.now())
				 .data(document)
				 .build());
	}
	
	@DeleteMapping("/{id}")
	@Operation(
			summary="Delete a document and purge its vector embeddings from vector store"	
			)
	public ResponseEntity<ApiResponse<Void>>deleteDocument(@PathVariable String id){
		documentService.deleteDocument(id);
		return ResponseEntity.ok(
				ApiResponse.<Void>builder()
        				.message("Document deleted successfully")
        				.success(true)
        				.timestamp(LocalDateTime.now())
        				.data(null)
     					.build());
	}
}
