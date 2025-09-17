package com.example.policyassistant.web;

import com.example.policyassistant.chat.Chat;
import com.example.policyassistant.service.ChatService;
import com.example.policyassistant.web.dto.ChatResponse;
import com.example.policyassistant.web.dto.CreateChatRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chats/create")
    public ResponseEntity<ChatResponse> createChat(@RequestBody(required = false) CreateChatRequest request) {
        String title = request != null ? request.title() : null;
        Chat chat = chatService.createChat(title);
        ChatResponse response = new ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), chat.getUpdatedAt());
        return ResponseEntity.created(URI.create("/chats/" + chat.getId())).body(response);
    }

    @GetMapping("/chats")
    public List<ChatResponse> listChats() {
        return chatService.getChats().stream()
                .map(chat -> new ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), chat.getUpdatedAt()))
                .toList();
    }

    @DeleteMapping("/chats/delete/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable String chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }
}
