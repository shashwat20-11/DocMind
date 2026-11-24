package com.DocMind.dto;

import com.DocMind.entity.DocumentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentResponseDto {

	private String id;
	private String fileName;
	private Long fileSize;
	private DocumentStatus status;
	private Integer chunksCreated;
	private String message;
	private long userId;
	
}
