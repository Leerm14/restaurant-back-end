package com.nhahang.restaurant.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    // Thư mục lưu ảnh (có thể config trong application.properties)
    private static final String UPLOAD_DIR = "uploads/images/";

    // --- API UPLOAD ẢNH MENU ---
    @PostMapping("/menu-image")
    public ResponseEntity<Map<String, String>> uploadMenuImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // 1. Validation file
            if (file.isEmpty()) {
                response.put("error", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // 2. Kiểm tra định dạng file
            String contentType = file.getContentType();
            if (!isImageFile(contentType)) {
                response.put("error", "Chỉ chấp nhận file ảnh (jpg, jpeg, png, gif)");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Kiểm tra kích thước file (tối đa 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("error", "Kích thước file không được vượt quá 5MB");
                return ResponseEntity.badRequest().body(response);
            }

            // 4. Tạo thư mục nếu chưa tồn tại
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 5. Tạo tên file unique để tránh trùng lặp
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // 6. Lưu file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 7. Trả về đường dẫn ảnh cho client
            String imageUrl = "/api/upload/images/" + uniqueFilename;
            response.put("imageUrl", imageUrl);
            response.put("filename", uniqueFilename);
            response.put("message", "Upload ảnh thành công");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("error", "Lỗi khi lưu file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // --- API LẤY ẢNH (Serve static images) ---
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR).resolve(filename);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Xác định content type dựa vào file extension
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- HELPER METHODS ---
    
    /**
     * Kiểm tra xem file có phải là ảnh không
     */
    private boolean isImageFile(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp")
        );
    }

    /**
     * Lấy file extension từ filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // --- API XÓA ẢNH (Tùy chọn) ---
    @DeleteMapping("/images/{filename}")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable String filename) {
        Map<String, String> response = new HashMap<>();
        
        try {
            Path imagePath = Paths.get(UPLOAD_DIR).resolve(filename);
            
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                response.put("message", "Xóa ảnh thành công");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Không tìm thấy ảnh");
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            response.put("error", "Lỗi khi xóa ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}