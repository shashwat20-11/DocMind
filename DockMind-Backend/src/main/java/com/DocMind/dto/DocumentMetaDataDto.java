package com.DocMind.dto;

import java.time.LocalDateTime;

import com.DocMind.entity.DocumentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentMetaDataDto {


    private String id;
    private  String fileName;
    private  String contentType;
    private  Long fileSize;
    private Integer totalPages;
    private  Integer totalChunks;
    private DocumentStatus status;
    private  String errorMessage;
    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;
}
