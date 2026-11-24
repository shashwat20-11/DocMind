package com.DocMind.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResultDto {

	 private  String query;
	    private  int totalMatches;
	    private List<CitationDto> matches;
	
}
