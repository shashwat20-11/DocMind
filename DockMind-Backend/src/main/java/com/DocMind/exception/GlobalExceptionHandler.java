package com.DocMind.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.DocMind.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?>handleNotFound(ResourceNotFoundException ex){
		
		log.warn("Resource not found : {}",ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.builder()
						.success(false)
						.message(ex.getMessage())
						.data(null)
						.timestamp(LocalDateTime.now())
						.build()
						);
	}
	
	@ExceptionHandler(DocumentProcessingException.class)
	public ResponseEntity<ApiResponse<Object>>handleDocumentProcessing(DocumentProcessingException ex){
		
		log.warn("Document Processing error : {}",ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(ApiResponse.builder()
						.success(false)
						.message("Failed to process document: "+ex.getMessage())
						.data(null)
						.timestamp(LocalDateTime.now())
						.build()
						);
	}
	
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Object>>handleMaxUploadSize(MaxUploadSizeExceededException ex){
		
		log.warn("File size limit exceeded : {}",ex.getMessage());
		
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(ApiResponse.builder()
						.success(false)
						.message("File size exceeds the allowed limit (25MB)")
						.data(null)
						.timestamp(LocalDateTime.now())
						.build()
						);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String,String>>>handleMethodArgumentNotValid(MethodArgumentNotValidException ex){
		
		Map<String,String>errors=new HashMap<>();
		
		for(FieldError error:ex.getBindingResult().getFieldErrors()) {
			errors.put(error.getField(), error.getDefaultMessage());
		}
		
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.<Map<String,String>>builder()
						.success(false)
						.message("Validation Failed")
						.data(errors)
						.timestamp(LocalDateTime.now())
						.build()
						);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?>handleIllegalArgument(IllegalArgumentException ex){
		
		log.warn("Illegal Argument : {}",ex.getMessage());
		
		
		
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.builder()
						.success(false)
						.message(ex.getMessage())
						.data(null)
						.timestamp(LocalDateTime.now())
						.build()
						);
	}
	
	
//  Generalized exception handling
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?>handleGeneralException(Exception ex){
		
		log.warn("Illegal Argument : {}",ex.getMessage());
		
		
		
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.builder()
						.success(false)
						.message("An unexpected error occur: "+ex.getMessage())
						.data(null)
						.timestamp(LocalDateTime.now())
						.build()
						);
	}
	
}
