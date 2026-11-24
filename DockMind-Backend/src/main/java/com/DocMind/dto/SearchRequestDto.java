package com.DocMind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchRequestDto {

	 @NotBlank(message = "Query cannot be empty")
	    private String query;

	    private String documentId;

	    private  Integer topK;

	    private  Double similaritySearch;
	    
}
