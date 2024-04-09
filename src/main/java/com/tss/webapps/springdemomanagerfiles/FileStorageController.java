package com.tss.webapps.springdemomanagerfiles;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/api/files")
public class FileStorageController {

    private final Path fileStorageLocation;

    public FileStorageController(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            file.transferTo(targetLocation);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/download/")
                    .path(fileName)
                    .toUriString();
            return ResponseEntity.ok("Upload completed! File download URI: " + fileDownloadUri);
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body("Could not store file " + fileName + ". Please try again!");
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFIle(@PathVariable String fileName, HttpServletRequest request) {
        Path filePath = fileStorageLocation.resolve(fileName).normalize();
        try {
            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

            if (contentType == null)
                contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(null);
        }

    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() {
        try {
            List<String> files = Files.list(fileStorageLocation)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
