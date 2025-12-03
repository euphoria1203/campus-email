package com.campusmail.service;

import com.campusmail.dto.MailDTO;
import com.campusmail.entity.Mail;
import com.campusmail.smtp.ParsedMail;

import java.util.List;
import java.util.Optional;

public interface MailService {
    Mail sendMail(MailDTO request);

    Mail saveDraft(MailDTO request);

    Mail sendDraft(Long mailId);

    List<Mail> listMails(Long userId, String folder);

    Optional<Mail> getMailById(Long id);

    void markAsRead(Long id);

    void toggleStar(Long id);

    void deleteMail(Long id);

    void batchDeleteMails(List<Long> ids);

    void deletePermanently(List<Long> ids);

    void restoreMails(List<Long> ids);

    /**
     * 获取邮件统计信息（未读数、草稿数）
     */
    java.util.Map<String, Integer> getMailStats(Long userId);

    /**
     * 通过SMTP协议接收的邮件创建
     */
    Mail createMail(ParsedMail parsedMail);
}
