package com.DocMind.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.DocMind.dto.ApiResponse;
import com.DocMind.dto.ChatRequestDto;
import com.DocMind.dto.ChatResponseDto;
import com.DocMind.dto.SearchRequestDto;
import com.DocMind.dto.SearchResultDto;
import com.DocMind.entity.User;
import com.DocMind.service.RAGService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@Tag(
		name="Chat Management",
		description="All chat related apis gets here."
		)
@RequiredArgsConstructor
public class ChatController {

	private final RAGService ragService;
	
	@PostMapping("/query")
    @Operation(summary = "Ask a question against all documents or a specific document with citations")
	public ResponseEntity<ApiResponse<ChatResponseDto>>askQuestion(
																	@Valid @RequestBody ChatRequestDto requestDto,
																	Authentication authentication){
		User user=(User)authentication.getPrincipal();
		ChatResponseDto responseDto=ragService.askQuestion(requestDto, user);
		return ResponseEntity.ok(
				ApiResponse.<ChatResponseDto>builder()
				.success(true)
				.message(null)				
				.data(responseDto)
				.timestamp(LocalDateTime.now())				
				.build()				
				);
				
	}
	
	@PostMapping("/stream")
    @Operation(summary = "Stream real-time Q&A answer tokens via Server-Sent events(SSE)")
	public Flux<String> streamQuestion(
									@Valid @RequestBody ChatRequestDto requestDto, 
									Authentication authentication){
		User user=(User) authentication.getPrincipal();
		return ragService.streamQuestionAnswer(requestDto, user);
	}
	
	@PostMapping("/search/similarity")
    @Operation(summary = "Perform semantic similarity search on stored document vectors")
	public ResponseEntity<ApiResponse<SearchResultDto>>searchSimilar(
									@Valid@RequestBody SearchRequestDto requestDto, 
									Authentication authentication){
		User user=(User)authentication.getPrincipal();
		SearchResultDto results=ragService.searchSimilarChunks(requestDto, user);
		return ResponseEntity.ok(ApiResponse.<SearchResultDto>builder()
				.success(true)
				.message(null)				
				.data(results)
				.timestamp(LocalDateTime.now())				
				.build());
	}

	
}
