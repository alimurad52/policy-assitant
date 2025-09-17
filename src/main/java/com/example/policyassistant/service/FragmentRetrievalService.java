package com.example.policyassistant.service;

import com.example.policyassistant.file.StoredFile;
import com.example.policyassistant.fragment.DocumentFragment;
import com.example.policyassistant.fragment.DocumentFragmentRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FragmentRetrievalService {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");
    private final DocumentFragmentRepository fragmentRepository;

    public FragmentRetrievalService(DocumentFragmentRepository fragmentRepository) {
        this.fragmentRepository = fragmentRepository;
    }

    public List<DocumentFragment> findRelevantFragments(String question, List<StoredFile> files, int limit) {
        if (files.isEmpty()) {
            return List.of();
        }
        Set<String> fileIds = files.stream().map(StoredFile::getId).collect(Collectors.toSet());
        Set<String> tokens = tokenize(question);
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<DocumentFragment> fragments = fragmentRepository.findByFileIdIn(fileIds);
        Map<DocumentFragment, Integer> scores = new HashMap<>();
        for (DocumentFragment fragment : fragments) {
            int score = scoreFragment(fragment, tokens);
            if (score > 0) {
                scores.put(fragment, score);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<DocumentFragment, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> e.getKey().getPageNumber())
                        .thenComparing(e -> e.getKey().getLineStart()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Set<String> tokenize(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        String[] parts = WORD_SPLIT.split(lower);
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (part.length() > 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private int scoreFragment(DocumentFragment fragment, Set<String> tokens) {
        String text = fragment.getText().toLowerCase();
        int score = 0;
        for (String token : tokens) {
            int index = 0;
            while (index >= 0) {
                index = text.indexOf(token, index);
                if (index >= 0) {
                    score++;
                    index += token.length();
                }
            }
        }
        return score;
    }
}
