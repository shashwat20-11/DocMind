package com.DocMind.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.DocMind.entity.Conversation;
import com.DocMind.entity.User;

public interface ConversationRepository extends JpaRepository<Conversation, String>
{
    List<Conversation> findByUserOrderByUpdatedAtDesc(User user);
    Optional<Conversation> findByIdAndUser(String id, User user);
    void deleteByIdAndUser(String id, User user);
}
