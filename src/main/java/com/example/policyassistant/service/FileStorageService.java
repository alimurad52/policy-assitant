package com.example.policyassistant.service;

import com.example.policyassistant.file.StoredFile;
import com.example.policyassistant.file.StoredFileRepository;
import com.example.policyassistant.fragment.DocumentFragment;
import com.example.policyassistant.fragment.DocumentFragmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    private final StoredFileRepository storedFileRepository;
    private final DocumentFragmentRepository fragmentRepository;
    private final PdfExtractionService pdfExtractionService;
    private final Path storageDirectory;

    public FileStorageService(StoredFileRepository storedFileRepository,
                              DocumentFragmentRepository fragmentRepository,
                              PdfExtractionService pdfExtractionService,
                              @Value("${file.storage.location}") String storageDirectory) throws IOException {
        this.storedFileRepository = storedFileRepository;
        this.fragmentRepository = fragmentRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.storageDirectory = Path.of(storageDirectory);
        Files.createDirectories(this.storageDirectory);
    }

    @Transactional
    public StoredFile store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
        String id = UUID.randomUUID().toString();
        String storedFilename = id + ".pdf";
        Path targetPath = storageDirectory.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        StoredFile storedFile = new StoredFile();
        storedFile.setId(id);
        storedFile.setOriginalFilename(file.getOriginalFilename());
        storedFile.setStoredFilename(storedFilename);
        storedFile.setUploadedAt(Instant.now());
        storedFile.setSize(file.getSize());
        storedFile.setContentType(contentType);
        storedFileRepository.save(storedFile);

        List<DocumentFragment> fragments = pdfExtractionService.extractFragments(targetPath);
        for (DocumentFragment fragment : fragments) {
            fragment.setFile(storedFile);
        }
        fragmentRepository.saveAll(fragments);
        return storedFile;
    }

    public List<StoredFile> findAll() {
        return storedFileRepository.findAll();
    }

    public Optional<StoredFile> findById(String id) {
        return storedFileRepository.findById(id);
    }

    @Transactional
    public void deleteById(String id) throws IOException {
        Optional<StoredFile> storedFileOptional = storedFileRepository.findById(id);
        if (storedFileOptional.isEmpty()) {
            return;
        }
        StoredFile storedFile = storedFileOptional.get();
        Path targetPath = storageDirectory.resolve(storedFile.getStoredFilename());
        Files.deleteIfExists(targetPath);
        storedFileRepository.delete(storedFile);
    }

    public Path resolvePath(StoredFile storedFile) {
        return storageDirectory.resolve(storedFile.getStoredFilename());
    }
}
