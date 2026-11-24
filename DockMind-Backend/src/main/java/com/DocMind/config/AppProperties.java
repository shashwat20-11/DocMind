package com.DocMind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@Data
@ConfigurationProperties(prefix="app")
public class AppProperties {

	private RagProperties rag=new RagProperties();
	private CorsProperties cors=new CorsProperties();
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RagProperties{
		
		private int chunkSize=600;
		private int chunkOverlap=100;
		private int topK=5;
		private int minChunkSizeChars=350;
		private int minChunkLengthToEmbed=5;
		private int maxNumChunks=1000;
		private double similarityThreshold=0.0;
		private String vectorStorePath="./vector_store.json";
	}
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class CorsProperties{
		
		private String allowedOrigins="*";
		private String allowedMethods="GET,PUT,POST,DELETE,OPTIONS";
		private String allowedHeaders="*";
		
		private double similarityDouble=0.0;
	}
	
	
}
