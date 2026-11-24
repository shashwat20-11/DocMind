package com.DocMind.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRequestDto {

	@NotBlank(message="Question cannot be empty")
	private String question;
	
	private String documentId;
	private Integer topK;
	private double minSimilarity;
	private String conversationId;
	
}
