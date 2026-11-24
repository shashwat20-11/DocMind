package com.DocMind.service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.DocMind.entity.ChatMessage;
import com.DocMind.entity.Conversation;
import com.DocMind.entity.MessageType;
import com.DocMind.repository.ChatMessageRepository;
import com.DocMind.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class JpaChatMemory implements ChatMemory{

	private final ChatMessageRepository chatMessageRepository;
	private final ConversationRepository conversationRepository;
	
	@Transactional
	@Override
	public void add(String conversationId, List<Message> messages) {
		if(messages==null || messages.isEmpty()) {
			return;
		}
		
		// Get or create a new conversation
		Conversation conversation=conversationRepository.findById(conversationId)
									.orElseGet(()->{
										log.warn("Conversation {} not found when adding messages. Creating placeholder.", conversationId );
									return conversationRepository.save(
											Conversation.builder()
											.id(conversationId)
											.title("New Conversation")
											.createdAt(LocalDateTime.now())
											.updatedAt(LocalDateTime.now())
											.build()	
											);
											
									});
		
		
		// Providing msgtype for each msgs
		
		for(Message msg:messages) {
			
			MessageType type=switch(msg.getMessageType()) {
			case USER->MessageType.USER;
			case ASSISTANT->MessageType.ASSISTANT;
			case SYSTEM->MessageType.SYSTEM;
			default->MessageType.USER;
			};
			
		ChatMessage chatMessage=ChatMessage.builder()
				.conversation(conversation)
				.messageType(type)
				.content(msg.getContent())
				.createdAt(LocalDateTime.now())
				.build();
		
		      chatMessageRepository.save(chatMessage);
		}
		
		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
	}

	@Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId, int lastN) {
        if (conversationId == null || conversationId.isBlank() || lastN <= 0) {
            return Collections.emptyList();
        }

        // Fetch using PageRequest to pass dynamic lastN without MySQL LIMIT syntax issues
        List<ChatMessage> all = chatMessageRepository.findRecentMessages(conversationId, PageRequest.of(0, lastN));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort chronologically (oldest -> newest) so LLM context reads forward in time
        all.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        List<Message> springAIMessages = new ArrayList<>();
        for (ChatMessage chatMessage : all) {
            String content = chatMessage.getContent() != null ? chatMessage.getContent() : "";

            switch (chatMessage.getMessageType()) {
                case USER -> springAIMessages.add(new UserMessage(content));
                case ASSISTANT -> springAIMessages.add(new AssistantMessage(content));
                case SYSTEM -> springAIMessages.add(new SystemMessage(content));
                default -> springAIMessages.add(new UserMessage(content));
            }
        }

        return springAIMessages;
    }

	 @Override
	    public void clear(String conversationId) {
	        chatMessageRepository.deleteByConversation_Id(conversationId);

	    }

}
