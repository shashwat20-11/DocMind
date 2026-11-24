package com.DocMind.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import com.DocMind.config.AppProperties;
import com.DocMind.dto.ChatRequestDto;
import com.DocMind.dto.ChatResponseDto;
import com.DocMind.dto.CitationDto;
import com.DocMind.dto.SearchRequestDto;
import com.DocMind.dto.SearchResultDto;
import com.DocMind.entity.Conversation;
import com.DocMind.entity.User;
import com.DocMind.repository.ConversationRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class RAGService {

	private final VectorStore vectorStore;
	private final AppProperties appProperties;
	private final ChatClient chatClient;
	private final ConversationRepository conversationRepository;
	
	public SearchResultDto searchSimilarChunks(SearchRequestDto request, User user) {
		
		List<Document>matchedDocs=retrieveRelevantDocuments(
				
				request.getQuery(),
				request.getDocumentId(),
				request.getTopK(),
				request.getSimilaritySearch(),
				user
		
				);
		List<CitationDto>citations=matchedDocs.stream().map(this::mapToCitation).toList();
		return SearchResultDto.builder()
								.query(request.getQuery())
								.totalMatches(citations.size())
								.matches(citations)
								.build();
	}

	private CitationDto  mapToCitation(Document document) {
		
		Map<String,Object>meta=document.getMetadata();
		String docId=null;
		if(meta.get("documentId")!=null) {
			try {
				docId=meta.get("documentId").toString();
			}catch(Exception e) {
				
			}
		}
		Integer chunkIndex=null;
		if(meta.get("chunkIndex") instanceof Number n) {
			chunkIndex=n.intValue();
		}
		
		Integer pageNumber=null;
		if(meta.get("pageNumber") instanceof Number n) {
			pageNumber=n.intValue();
		}else if(meta.get("page_number") instanceof Number n) {
			pageNumber=n.intValue();
		}
		
		
		Double score=null;
		if(meta.get("distance") instanceof Number n) {
			score=	1.0-n.doubleValue();
		}
		
		
	//  ---Modified---
		
		// --- Score Extraction ---
//		Double score = null;
//
//		// 1. Check metadata map for distance or score
//		Object rawScore = meta.get("score") != null ? meta.get("score") : meta.get("similarity");
//		Object rawDistance = meta.get("distance");
//
//		if (rawScore instanceof Number n) {
//		    score = Math.round(n.doubleValue() * 10000.0) / 10000.0;
//		} else if (rawScore instanceof String s) {
//		    try {
//		        score = Math.round(Double.parseDouble(s) * 10000.0) / 10000.0;
//		    } catch (NumberFormatException ignored) {}
//		} else if (rawDistance instanceof Number n) {
//		    score = Math.round((1.0 - n.doubleValue()) * 10000.0) / 10000.0;
//		} else if (rawDistance instanceof String s) {
//		    try {
//		        score = Math.round((1.0 - Double.parseDouble(s)) * 10000.0) / 10000.0;
//		    } catch (NumberFormatException ignored) {}
//		}
//
//	    // 3. Fallback: Provide a valid normalized confidence score if SimpleVectorStore leaves both null
//	    if (score == null) {
//	        score = 0.85;
//	    }

	    // Fallback for fileName checking both "fileName" and "file_name"
	    String fileName = "Unknown";
	    if (meta.containsKey("fileName") && meta.get("fileName") != null) {
	        fileName = meta.get("fileName").toString();
	    } else if (meta.containsKey("file_name") && meta.get("file_name") != null) {
	        fileName = meta.get("file_name").toString();
	    }
		
		
		return CitationDto.builder()
				.documentId(docId)
				.fileName(fileName)
				.chunkIndex(chunkIndex)
				.pageNumber(pageNumber)
				.snippet(cleanSnippetText(document.getContent()))
				.similarityScore(score)
				.metadata(meta)
				.build();
	}
	
	// To ask anything related to document
	
	public ChatResponseDto askQuestion(ChatRequestDto request, User user) {
		long startTime=System.currentTimeMillis();
		
		//String conversationId=request.getConversationId()!=null?request.getConversationId():UUID.randomUUID().toString();
		
		//Modified
		// 1. Resolve or generate a single valid conversationId
				String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
						? request.getConversationId()
						: UUID.randomUUID().toString();
				
		
		ensureConversationExists(conversationId, user, request.getQuestion());
		log.info("Processing query: '{}', scoped documentId: {}",request.getQuestion(),request.getDocumentId());
		
		List<Document>similarDocuments=this.retrieveRelevantDocuments(request.getQuestion(), 
																		request.getDocumentId(), 
																		request.getTopK(), 
																		request.getMinSimilarity(),
																		user);
		
		List<CitationDto> citationDtos=similarDocuments.stream().map(this::mapToCitation).toList();
			
		String contextText=buildContextString(similarDocuments);
		
		//String prompt=buildPrompt(request.getQuestion(),contextText);
			
		String answer=this.chatClient.prompt()
						.system(s -> s.param("doc_context", contextText))
						.user(request.getQuestion())
						//.advisors(a -> a.param("chat_memory_conversation_id", request.getConversationId()))
						.advisors(a -> a.param("chat_memory_conversation_id", conversationId))
						.call()
						.content();
		
		long responseTime = System.currentTimeMillis()-startTime;
		log.info("Completed Q&A in {} ms with {} citations",responseTime,citationDtos.size());
						
		
		
		return ChatResponseDto.builder()
								.answer(answer)
							//	.conversationId(request.getConversationId()!=null?request.getConversationId():UUID.randomUUID().toString())
								.conversationId(conversationId)
								.citation(citationDtos)
								.responseTimeMs(responseTime)
								.build();
		
	}
	
	private void ensureConversationExists(String conversationId, User user, @NotBlank(message = "Question cannot be empty") String question) {

        conversationRepository.findByIdAndUser(conversationId, user)
                .orElseGet(() -> {
                    String title = question.length() > 50 ? question.substring(0, 47) + "....." : question;
                    return conversationRepository.save(
                            Conversation.builder().id(conversationId)
                                    .title(title)
                                    .user(user)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build()
                    );
                });


    }

	// To stream the question and answer from document
	
	public Flux<String>streamQuestionAnswer(ChatRequestDto request, User user){
		
		log.info("Streaming query: '{}'",request.getQuestion());
		
		//String conversationId = request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString();
        
		//  Modified
		String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
				? request.getConversationId()
				: UUID.randomUUID().toString();
		
		request.setConversationId(conversationId);
        ensureConversationExists(conversationId, user, request.getQuestion());
        
		List<Document>relevantDocuments=this.retrieveRelevantDocuments(request.getQuestion(), 
				request.getDocumentId(), 
				request.getTopK(), 
				request.getMinSimilarity(),
				user);
		
			String contextText=buildContextString(relevantDocuments);
		
	//	String userPrompt=buildPrompt(request.getQuestion(),contextText);
		
		return chatClient.prompt()
				.system(s-> s.param("doc_context",contextText))
	            .user(request.getQuestion())
			//	.advisors(a -> a.param("chat_memory_conversation_id", request.getConversationId()))
	            .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
				.stream()
				.content()
				.concatWith(Flux.just("[DONE]"));
		
	}
	
	
//	private String buildPrompt(@NotBlank(message="Question cannot be empty")String question, String contextText) {
//		if(contextText!=null && !contextText.isBlank()) {
//			return String.format("""
//					
//					Document Context:
//                           ---------------------
//                           %s
//                           ---------------------                                        
//                           User Message / Question: %s             
//                           Instructions:
//                           - If the user's question relates to the document context above, prioritize answering using that context and reference key sections.
//                           - If the user is asking a general question, greeting, or discussing topics beyond the document context, respond helpfully and conversationally using your general knowledge while weaving in relevant document context if applicable
//					
//					""",contextText,question);
//		}else {
//			return String.format("""
//                    User Message / Question:
//                    %s                         
//                    Instructions:
//                       - Respond helpfully, accurately, and conversationally to the user's message using your broad knowledge base.
//                 
//                 """, question);
//		}
//		
//	}

	private String buildContextString(List<Document> similarDocuments) {
	    if (similarDocuments == null || similarDocuments.isEmpty()) {
	        return "";
	    }

	    return similarDocuments.stream()
	            .map(doc -> {
	                String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
	                Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");
	                return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getContent());
	            })
	            .collect(Collectors.joining("\n\n---\n\n"));
	}
	

	private List<Document> retrieveRelevantDocuments(@NotBlank(message="Query cannot be empty") String query, String documentId, Integer topK,
			Double similaritySearch, User user) {
		
		int effectiveTopK=(topK!=null && topK>0)?topK:appProperties.getRag().getTopK();
		
		double effectiveSimilarity=(similaritySearch!=null)?similaritySearch:appProperties.getRag().getSimilarityThreshold();
		
		//Added
		// Pull a larger pool of chunks so we have enough after filtering
	    int searchPoolSize = Math.max(effectiveTopK * 5, 25);
		
		SearchRequest searchRequestBuilder = SearchRequest
				.query(query)
			//  .withTopK(effectiveTopK);
	            .withTopK(searchPoolSize);
		
		if(effectiveSimilarity>0.0) {
			searchRequestBuilder=searchRequestBuilder.withSimilarityThreshold(effectiveSimilarity);
		}
		
		// --DO NOT call .withFilterExpression() here! SimpleVectorStore does not support it.---
//		FilterExpressionBuilder b=new FilterExpressionBuilder();
//		
//		searchRequestBuilder = searchRequestBuilder.withFilterExpression(
//			    (documentId != null 
//			        ? b.and(
//			        		// introduced userId filter
//			        		b.eq("documentId", documentId.toString()), 
//			        		b.eq("userId", user.getId().toString())
//			        		)
//			        		// introduced userId filter
//			        : b.eq("userId", user.getId().toString())
//			    ).build()
//			);
		
		try {
			
			//List<Document>documents=vectorStore.similaritySearch(searchRequestBuilder);
			
			//Modified
			List<Document>rawDocuments=vectorStore.similaritySearch(searchRequestBuilder);
	        log.info("Raw vector search returned {} chunks", rawDocuments.size());

	        String targetUserId = String.valueOf(user.getId());
		
	     // In-memory post-filtering by userId and optional documentId
	        List<Document> filteredDocuments = rawDocuments.stream()
	                .filter(doc -> {
	                    Map<String, Object> meta = doc.getMetadata();
	                    Object docUserId = meta.get("userId");
	                    return docUserId != null && targetUserId.equals(docUserId.toString());
	                })
	                .filter(doc -> {
	                    if (documentId == null || documentId.isBlank()) {
	                        return true;
	                    }
	                    Object chunkDocId = doc.getMetadata().get("documentId");
	                    return chunkDocId != null && documentId.equals(chunkDocId.toString());
	                })
	                .limit(effectiveTopK)
	                .toList();
	        
	        log.info("Retrieved {} chunks after in-memory filtering for query: '{}' (scoped docId: {})",
	                filteredDocuments.size(), query, documentId);

	        return filteredDocuments;
			
		}catch(Exception e) {
			log.error("Failed to perform vector similarity search for query '{}': {}", query, e.getMessage(), e);
			return  Collections.emptyList();
		}
	
		
	}
	
	private String cleanSnippetText(String text) {
	    if (text == null) {
	        return "";
	    }
	    return text
	            // 1. Replace Windows carriage returns
	            .replace("\r", "")
	            // 2. Replace non-breaking spaces (\u00A0) and corrupted glyphs (\uFFFD)
	            .replace('\u00A0', ' ')
	            .replace('\uFFFD', ' ')
	            // 3. Collapse multiple consecutive horizontal spaces/tabs into a single space
	            .replaceAll("[ \\t]+", " ")
	            // 4. Collapse 3 or more consecutive newlines into 2
	            .replaceAll("\\n{3,}", "\n\n")
	            .trim();
	}
	
	
}
