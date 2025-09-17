package com.example.policyassistant.web;

import com.example.policyassistant.file.StoredFile;
import com.example.policyassistant.service.FileStorageService;
import com.example.policyassistant.web.dto.FileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@Validated
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        StoredFile storedFile = fileStorageService.store(file);
        return ResponseEntity.ok().body("file uploaded: " + storedFile.getOriginalFilename());
    }

    @GetMapping("/files")
    public List<FileResponse> list() {
        return fileStorageService.findAll().stream()
                .map(file -> new FileResponse(file.getId(), file.getOriginalFilename(), file.getSize(), file.getUploadedAt()))
                .toList();
    }

    @DeleteMapping("/files/delete/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String fileId) throws IOException {
        fileStorageService.deleteById(fileId);
        return ResponseEntity.noContent().build();
    }
}
