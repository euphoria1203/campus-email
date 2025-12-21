package com.campusmail.service;

import com.campusmail.dto.MailDTO;
import com.campusmail.entity.Mail;
import com.campusmail.smtp.ParsedMail;

import java.util.List;
import java.util.Optional;

public interface MailService {
    Mail sendMail(Long userId, MailDTO request);

    Mail saveDraft(Long userId, MailDTO request);

    Mail sendDraft(Long userId, Long mailId);

    List<Mail> listMails(Long userId, String folder);

    Optional<Mail> getMailById(Long id, Long userId);

    void markAsRead(Long id, Long userId);

    void toggleStar(Long id, Long userId);

    void deleteMail(Long id, Long userId);

    void batchDeleteMails(List<Long> ids, Long userId);

    void deletePermanently(List<Long> ids, Long userId);

    void restoreMails(List<Long> ids, Long userId);

    /**
     * 获取邮件统计信息（未读数、草稿数）
     */
    java.util.Map<String, Integer> getMailStats(Long userId);

    /**
     * 通过SMTP协议接收的邮件创建
     */
    Mail createMail(ParsedMail parsedMail);

    /**
     * 全文搜索用户邮件
     */
    List<Mail> search(Long userId, String keyword, String folder, int page, int size);
}
