package com.campusmail.controller;

import com.campusmail.dto.MailDTO;
import com.campusmail.entity.Attachment;
import com.campusmail.entity.Mail;
import com.campusmail.mapper.AttachmentMapper;
import com.campusmail.service.MailService;
import com.campusmail.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/mails")
public class MailController {

    private final MailService mailService;
    private final AttachmentMapper attachmentMapper;

    public MailController(MailService mailService, AttachmentMapper attachmentMapper) {
        this.mailService = mailService;
        this.attachmentMapper = attachmentMapper;
    }

    /**
     * 发送邮件
     */
    @PostMapping
    public ResponseEntity<Mail> send(@Valid @RequestBody MailDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setUserId(userId);
        return ResponseEntity.ok(mailService.sendMail(userId, request));
    }

    /**
     * 保存草稿
     */
    @PostMapping("/drafts")
    public ResponseEntity<Mail> saveDraft(@RequestBody MailDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setUserId(userId);
        return ResponseEntity.ok(mailService.saveDraft(userId, request));
    }

    /**
     * 发送草稿
     */
    @PostMapping("/drafts/{id}/send")
    public ResponseEntity<Mail> sendDraft(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(mailService.sendDraft(userId, id));
    }

    /**
     * 获取用户邮件列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Mail>> listByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "folder", required = false) String folder) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!Objects.equals(currentUserId, userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(mailService.listMails(currentUserId, folder));
    }

    /**
     * 全文搜索邮件
     */
    @GetMapping("/search")
    public ResponseEntity<List<Mail>> search(
        @RequestParam("keyword") String keyword,
        @RequestParam(value = "folder", required = false) String folder,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(mailService.search(currentUserId, keyword, folder, page, size));
    }

    /**
     * 获取邮件详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMailDetail(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return mailService.getMailById(id, userId)
            .map(mail -> {
                // 标记为已读
                mailService.markAsRead(id, userId);
                mail.setIsRead(true);
                
                // 获取附件列表
                List<Attachment> attachments = attachmentMapper.findByMailId(id);
                
                Map<String, Object> result = new HashMap<>();
                result.put("mail", mail);
                result.put("attachments", attachments);
                return ResponseEntity.ok(result);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 标记邮件为已读
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 切换星标状态
     */
    @PutMapping("/{id}/star")
    public ResponseEntity<Void> toggleStar(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailService.toggleStar(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除邮件（移动到垃圾箱）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMail(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailService.deleteMail(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量删除邮件
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDelete(@RequestBody List<Long> ids) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailService.batchDeleteMails(ids, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 垃圾箱彻底删除
     */
    @DeleteMapping("/trash")
    public ResponseEntity<Void> deletePermanently(@RequestBody List<Long> ids) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailService.deletePermanently(ids, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 移出垃圾箱
     */
    @PutMapping("/trash/restore")
    public ResponseEntity<Void> restoreFromTrash(@RequestBody List<Long> ids) {
        Long userId = SecurityUtils.getCurrentUserId();
        mailService.restoreMails(ids, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取邮件统计信息（未读数、草稿数）
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Integer>> getMailStats(@PathVariable("userId") Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!Objects.equals(currentUserId, userId)) {
            return ResponseEntity.status(403).build();
        }
        Map<String, Integer> stats = mailService.getMailStats(currentUserId);
        return ResponseEntity.ok(stats);
    }
}
