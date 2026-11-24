package com.DocMind.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.DocMind.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversation_IdOrderByCreatedAtAsc(String conversationId);

 // Clean JPQL query using Pageable
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("conversationId") String conversationId, Pageable pageable);

    void deleteByConversation_Id(String conversationId);
}