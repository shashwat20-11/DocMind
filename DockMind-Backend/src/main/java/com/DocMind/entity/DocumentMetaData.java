package com.DocMind.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="document_metaData")
public class DocumentMetaData {
	
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private String id;
	
	@Column(nullable = false)
	private String fileName;
	@Column(nullable = false)
	private String contentType;
	private Long fileSize;
	private Integer totalPages;
	private Integer totalChunks;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DocumentStatus status;
	
	@Column(length=1000)
	private String errorMessage;
	 @Column(nullable = false, updatable = false)
	    private LocalDateTime createdAt;
	    private LocalDateTime updatedAt;
	
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=false)
	private User user;

	

}
