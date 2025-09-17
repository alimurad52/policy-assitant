package com.example.policyassistant.service;

import com.example.policyassistant.chat.Chat;
import com.example.policyassistant.chat.MessageRole;
import com.example.policyassistant.file.StoredFile;
import com.example.policyassistant.file.StoredFileRepository;
import com.example.policyassistant.fragment.DocumentFragment;
import com.example.policyassistant.web.dto.AskRequest;
import com.example.policyassistant.web.dto.AskResponse;
import com.example.policyassistant.web.dto.CitationDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AskService {

    private static final int MAX_CONTEXT_FRAGMENTS = 8;
    private final StoredFileRepository storedFileRepository;
    private final FragmentRetrievalService fragmentRetrievalService;
    private final BedrockChatService bedrockChatService;
    private final ChatService chatService;

    public AskService(StoredFileRepository storedFileRepository,
                      FragmentRetrievalService fragmentRetrievalService,
                      BedrockChatService bedrockChatService,
                      ChatService chatService) {
        this.storedFileRepository = storedFileRepository;
        this.fragmentRetrievalService = fragmentRetrievalService;
        this.bedrockChatService = bedrockChatService;
        this.chatService = chatService;
    }

    public AskResponse ask(AskRequest request) {
        List<StoredFile> files = resolveFiles(request.fileIds());
        Chat chat = chatService.ensureChat(request.chatId(), request.question());
        if (request.question() == null || request.question().isBlank()) {
            return new AskResponse(chat.getId(), chat.getTitle(), "I don't know.", List.of());
        }
        if (files.isEmpty()) {
            chatService.addMessage(chat, MessageRole.USER, request.question(), null);
            chatService.addMessage(chat, MessageRole.ASSISTANT, "I don't know.", null);
            return new AskResponse(chat.getId(), chat.getTitle(), "I don't know.", List.of());
        }
        List<DocumentFragment> fragments = fragmentRetrievalService.findRelevantFragments(request.question(), files, MAX_CONTEXT_FRAGMENTS);
        chatService.addMessage(chat, MessageRole.USER, request.question(), null);
        if (fragments.isEmpty()) {
            chatService.addMessage(chat, MessageRole.ASSISTANT, "I don't know.", null);
            return new AskResponse(chat.getId(), chat.getTitle(), "I don't know.", List.of());
        }
        String contextPrompt = buildContextPrompt(fragments);
        String userPrompt = "Answer the user's question strictly using the context. If the context does not contain the answer, respond with \"I don't know.\".\n\nQuestion: " + request.question();
        String answer = bedrockChatService.askModel(systemPrompt(), contextPrompt + "\n\n" + userPrompt);
        if (answer == null || answer.isBlank()) {
            answer = "I don't know.";
        }
        List<CitationDto> citations = fragments.stream()
                .sorted(Comparator.comparing(DocumentFragment::getPageNumber)
                        .thenComparing(DocumentFragment::getLineStart))
                .map(this::toCitation)
                .toList();
        chatService.addMessage(chat, MessageRole.ASSISTANT, answer, citations);
        return new AskResponse(chat.getId(), chat.getTitle(), answer, citations);
    }

    private List<StoredFile> resolveFiles(List<String> fileIds) {
        List<StoredFile> files;
        if (fileIds == null || fileIds.isEmpty()) {
            files = storedFileRepository.findAll();
        } else {
            files = fileIds.stream()
                    .map(storedFileRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
        return files;
    }

    private String buildContextPrompt(List<DocumentFragment> fragments) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a policy assistant. Use only the provided context to answer the user. If you lack the information, reply with \"I don't know.\".\n\nContext:\n");
        for (DocumentFragment fragment : fragments) {
            StoredFile file = fragment.getFile();
            builder.append("Document: ")
                    .append(file != null ? file.getOriginalFilename() : "unknown")
                    .append(" | Page ")
                    .append(fragment.getPageNumber())
                    .append(" | Lines ")
                    .append(fragment.getLineStart());
            if (fragment.getLineEnd() != fragment.getLineStart()) {
                builder.append("-").append(fragment.getLineEnd());
            }
            builder.append("\n")
                    .append(fragment.getText())
                    .append("\n---\n");
        }
        return builder.toString();
    }

    private CitationDto toCitation(DocumentFragment fragment) {
        String text = fragment.getText();
        if (text.length() > 100) {
            text = text.substring(0, 97) + "...";
        }
        StoredFile file = fragment.getFile();
        return new CitationDto(file != null ? file.getId() : null,
                file != null ? file.getOriginalFilename() : "unknown",
                text,
                fragment.getPageNumber(),
                fragment.getLineStart());
    }

    private String systemPrompt() {
        return "You are a helpful policy assistant for internal company documents. Only use the provided context to answer. If the context does not contain the answer or you are uncertain, reply exactly with 'I don't know.'. Do not fabricate information.";
    }
}
