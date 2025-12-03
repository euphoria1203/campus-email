package com.campusmail.controller;

import com.campusmail.dto.MailDTO;
import com.campusmail.entity.Attachment;
import com.campusmail.entity.Mail;
import com.campusmail.mapper.AttachmentMapper;
import com.campusmail.service.MailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return ResponseEntity.ok(mailService.sendMail(request));
    }

    /**
     * 保存草稿
     */
    @PostMapping("/drafts")
    public ResponseEntity<Mail> saveDraft(@RequestBody MailDTO request) {
        return ResponseEntity.ok(mailService.saveDraft(request));
    }

    /**
     * 发送草稿
     */
    @PostMapping("/drafts/{id}/send")
    public ResponseEntity<Mail> sendDraft(@PathVariable("id") Long id) {
        return ResponseEntity.ok(mailService.sendDraft(id));
    }

    /**
     * 获取用户邮件列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Mail>> listByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "folder", required = false) String folder) {
        return ResponseEntity.ok(mailService.listMails(userId, folder));
    }

    /**
     * 获取邮件详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMailDetail(@PathVariable("id") Long id) {
        return mailService.getMailById(id)
            .map(mail -> {
                // 标记为已读
                mailService.markAsRead(id);
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
        mailService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 切换星标状态
     */
    @PutMapping("/{id}/star")
    public ResponseEntity<Void> toggleStar(@PathVariable("id") Long id) {
        mailService.toggleStar(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除邮件（移动到垃圾箱）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMail(@PathVariable("id") Long id) {
        mailService.deleteMail(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量删除邮件
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDelete(@RequestBody List<Long> ids) {
        mailService.batchDeleteMails(ids);
        return ResponseEntity.ok().build();
    }

    /**
     * 垃圾箱彻底删除
     */
    @DeleteMapping("/trash")
    public ResponseEntity<Void> deletePermanently(@RequestBody List<Long> ids) {
        mailService.deletePermanently(ids);
        return ResponseEntity.ok().build();
    }

    /**
     * 移出垃圾箱
     */
    @PutMapping("/trash/restore")
    public ResponseEntity<Void> restoreFromTrash(@RequestBody List<Long> ids) {
        mailService.restoreMails(ids);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取邮件统计信息（未读数、草稿数）
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Integer>> getMailStats(@PathVariable("userId") Long userId) {
        Map<String, Integer> stats = mailService.getMailStats(userId);
        return ResponseEntity.ok(stats);
    }
}
