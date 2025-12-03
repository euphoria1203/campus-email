package com.campusmail.controller;

import com.campusmail.entity.Attachment;
import com.campusmail.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 上传单个附件
     */
    @PostMapping("/upload")
    public ResponseEntity<Attachment> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mailId", required = false) Long mailId) {
        Attachment attachment = attachmentService.upload(file, mailId);
        return ResponseEntity.ok(attachment);
    }

    /**
     * 批量上传附件
     */
    @PostMapping("/upload/batch")
    public ResponseEntity<List<Attachment>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "mailId", required = false) Long mailId) {
        List<Attachment> attachments = attachmentService.uploadMultiple(files, mailId);
        return ResponseEntity.ok(attachments);
    }

    /**
     * 获取邮件的所有附件
     */
    @GetMapping("/mail/{mailId}")
    public ResponseEntity<List<Attachment>> getByMailId(@PathVariable("mailId") Long mailId) {
        return ResponseEntity.ok(attachmentService.getByMailId(mailId));
    }

    /**
     * 下载附件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        Attachment attachment = attachmentService.getById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = attachmentService.download(id);
        String encodedFileName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    /**
     * 删除附件
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
        attachmentService.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "附件删除成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取附件信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Attachment> getById(@PathVariable("id") Long id) {
        Attachment attachment = attachmentService.getById(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(attachment);
    }
}
