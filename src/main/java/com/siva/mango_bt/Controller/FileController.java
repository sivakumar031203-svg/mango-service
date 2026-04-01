package com.siva.mango_bt.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;

@RestController
public class FileController {
    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) throws Exception {
        String path = request.getRequestURI().substring("/uploads/".length());
        Path filePath = Paths.get("uploads/").resolve(path).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }
}
