package com.example.policyassistant.service;

import com.example.policyassistant.chat.Chat;
import com.example.policyassistant.chat.ChatMessage;
import com.example.policyassistant.chat.ChatMessageRepository;
import com.example.policyassistant.chat.ChatRepository;
import com.example.policyassistant.chat.MessageRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public ChatService(ChatRepository chatRepository,
                       ChatMessageRepository chatMessageRepository,
                       ObjectMapper objectMapper) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    public List<Chat> getChats() {
        return chatRepository.findAll();
    }

    public Optional<Chat> findChat(String chatId) {
        return chatRepository.findById(chatId);
    }

    @Transactional
    public Chat createChat(String title) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String finalTitle = (title == null || title.isBlank()) ? "New chat" : title.trim();
        Chat chat = new Chat(id, finalTitle, now, now);
        return chatRepository.save(chat);
    }

    @Transactional
    public Chat ensureChat(String chatId, String fallbackQuestion) {
        if (chatId != null && !chatId.isBlank()) {
            Optional<Chat> existing = chatRepository.findById(chatId);
            if (existing.isPresent()) {
                Chat chat = existing.get();
                if (chat.getTitle() == null || chat.getTitle().isBlank()) {
                    chat.setTitle(generateTitle(fallbackQuestion));
                    chat.setUpdatedAt(Instant.now());
                    chatRepository.save(chat);
                }
                return chat;
            }
        }
        Chat newChat = new Chat(UUID.randomUUID().toString(), generateTitle(fallbackQuestion), Instant.now(), Instant.now());
        return chatRepository.save(newChat);
    }

    public void deleteChat(String chatId) {
        chatRepository.deleteById(chatId);
    }

    public List<ChatMessage> getMessagesForChat(String chatId) {
        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Transactional
    public void addMessage(Chat chat, MessageRole role, String content, Object citations) {
        String serialized = null;
        if (citations != null) {
            try {
                serialized = objectMapper.writeValueAsString(citations);
            } catch (JsonProcessingException e) {
                serialized = null;
            }
        }
        ChatMessage message = new ChatMessage(chat, role, content, Instant.now(), serialized);
        chatMessageRepository.save(message);
        chat.setUpdatedAt(Instant.now());
        chatRepository.save(chat);
    }

    private String generateTitle(String fallbackQuestion) {
        if (fallbackQuestion == null || fallbackQuestion.isBlank()) {
            return "Policy chat";
        }
        String normalized = fallbackQuestion.trim();
        normalized = normalized.replaceAll("\n", " ");
        if (normalized.length() > 60) {
            normalized = normalized.substring(0, 57) + "...";
        }
        return normalized;
    }
}
