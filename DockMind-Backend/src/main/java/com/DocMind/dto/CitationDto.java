package com.DocMind.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitationDto {

	 private String documentId;
	    private String fileName;
	    private Integer chunkIndex;
	    private Integer pageNumber;
	    private String snippet;
	    private Double similarityScore;
	    private Map<String, Object> metadata;
	
}
