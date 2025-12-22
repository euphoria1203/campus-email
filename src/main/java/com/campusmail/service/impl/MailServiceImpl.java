package com.campusmail.service.impl;

import com.campusmail.dto.MailDTO;
import com.campusmail.entity.Attachment;
import com.campusmail.entity.Mail;
import com.campusmail.entity.MailAccount;
import com.campusmail.mapper.AttachmentMapper;
import com.campusmail.mapper.MailAccountMapper;
import com.campusmail.mapper.MailMapper;
import com.campusmail.service.AttachmentService;
import com.campusmail.service.MailService;
import com.campusmail.service.OutboundMailSender;
import com.campusmail.smtp.ParsedMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private final MailMapper mailMapper;
    private final AttachmentMapper attachmentMapper;
    private final AttachmentService attachmentService;
    private final MailAccountMapper mailAccountMapper;
    private final OutboundMailSender outboundMailSender;

    public MailServiceImpl(MailMapper mailMapper,
                           AttachmentMapper attachmentMapper,
                           AttachmentService attachmentService,
                           MailAccountMapper mailAccountMapper,
                           OutboundMailSender outboundMailSender) {
        this.mailMapper = mailMapper;
        this.attachmentMapper = attachmentMapper;
        this.attachmentService = attachmentService;
        this.mailAccountMapper = mailAccountMapper;
        this.outboundMailSender = outboundMailSender;
    }

    @Override
    @Transactional
    public Mail sendMail(Long userId, MailDTO request) {
        assertRecipientsPresent(request.getToAddress(), request.getCcAddress(), request.getBccAddress());

        if (!Objects.equals(userId, request.getUserId())) {
            request.setUserId(userId);
        }

        MailAccount account = resolveMailAccount(userId, request.getAccountId());

        String aggregatedRecipients = aggregateRecipients(
            request.getToAddress(), request.getCcAddress(), request.getBccAddress()
        );

        Mail mail = new Mail();
        mail.setUserId(userId);
        mail.setAccountId(account.getId());
        mail.setFolder("sent");
        mail.setFromAddress(formatSenderAddress(account));
        mail.setToAddress(aggregatedRecipients);
        mail.setCcAddress(request.getCcAddress());
        mail.setBccAddress(request.getBccAddress());
        mail.setSubject(request.getSubject());
        mail.setContent(request.getContent());
        mail.setPlainContent(request.getPlainContent());
        mail.setIsRead(true);
        mail.setIsStarred(false);
        mail.setIsDeleted(false);
        mail.setHasAttachment(request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty());
        mail.setPriority(request.getPriority() != null ? request.getPriority() : 3);
        mail.setSendTime(LocalDateTime.now());

        mailMapper.insert(mail);

        // 关联附件到邮件
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            for (Long attachmentId : request.getAttachmentIds()) {
                attachmentMapper.updateMailId(attachmentId, mail.getId());
            }
        }

        sendOutbound(mail);
        distributeToLocalRecipients(mail);

        // 如果是从草稿编辑后发送，删除原草稿
        if (request.getId() != null) {
            mailMapper.findById(request.getId()).ifPresent(draft -> {
                if ("drafts".equals(draft.getFolder())) {
                    mailMapper.delete(request.getId());
                    log.info("已删除原草稿: id={}", request.getId());
                }
            });
        }

        log.info("邮件发送成功: id={}, from={}, to={}", mail.getId(), mail.getFromAddress(), mail.getToAddress());
        return mail;
    }

    @Override
    @Transactional
    public Mail saveDraft(Long userId, MailDTO request) {
        if (!Objects.equals(userId, request.getUserId())) {
            request.setUserId(userId);
        }

        MailAccount account = resolveMailAccount(userId, request.getAccountId());

        Mail mail;
        boolean isUpdate = false;
        
        // 如果提供了ID，则更新现有草稿
        if (request.getId() != null) {
            mail = mailMapper.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("草稿不存在"));
            isUpdate = true;
        } else {
            mail = new Mail();
            mail.setUserId(userId);
            mail.setIsRead(true);
            mail.setIsStarred(false);
            mail.setIsDeleted(false);
        }

        mail.setAccountId(account.getId());
        mail.setFolder("drafts");
        mail.setFromAddress(formatSenderAddress(account));  // 使用格式化方法包含显示名称
        mail.setToAddress(aggregateRecipients(
            request.getToAddress(), request.getCcAddress(), request.getBccAddress()
        ));
        mail.setCcAddress(request.getCcAddress());
        mail.setBccAddress(request.getBccAddress());
        mail.setSubject(request.getSubject());
        mail.setContent(request.getContent());
        mail.setPlainContent(request.getPlainContent());
        mail.setHasAttachment(request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty());
        mail.setPriority(request.getPriority() != null ? request.getPriority() : 3);
        mail.setSendTime(null);
        mail.setReceiveTime(null);

        if (isUpdate) {
            mailMapper.update(mail);
        } else {
            mailMapper.insert(mail);
        }

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            for (Long attachmentId : request.getAttachmentIds()) {
                attachmentMapper.updateMailId(attachmentId, mail.getId());
            }
        }

        log.info("草稿{}成功: id={}, userId={}", isUpdate ? "更新" : "保存", mail.getId(), mail.getUserId());
        return mail;
    }

    @Override
    public List<Mail> listMails(Long userId, String folder) {
        List<Mail> mails;
        if (folder != null && !folder.isEmpty()) {
            if ("starred".equalsIgnoreCase(folder)) {
                mails = mailMapper.findStarredByUserId(userId);
            } else if ("trash".equalsIgnoreCase(folder)) {
                mails = mailMapper.findTrashByUserId(userId);
            } else {
                mails = mailMapper.findByUserIdAndFolder(userId, folder);
            }
        } else {
            mails = mailMapper.findByUserId(userId);
        }

        sanitizeMailCollection(mails);
        return mails;
    }

    @Override
    public List<Mail> search(Long userId, String keyword, String folder, Long accountId, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        int pageSize = Math.min(Math.max(size, 1), 200);
        int offset = Math.max(page, 0) * pageSize;
        String query = keyword.trim();
        List<Mail> mails = mailMapper.search(userId, query, folder, accountId, pageSize, offset);
        sanitizeMailCollection(mails);
        applyHighlight(mails, keyword.trim());
        return mails;
    }

    @Override
    public Optional<Mail> getMailById(Long id, Long userId) {
        return mailMapper.findById(id)
            .filter(mail -> Objects.equals(mail.getUserId(), userId))
            .map(mail -> {
                sanitizeMailVisibility(mail);
                return mail;
            });
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Mail mail = requireOwnedMail(id, userId);
        mailMapper.markAsRead(mail.getId());
    }

    @Override
    public void toggleStar(Long id, Long userId) {
        Mail mail = requireOwnedMail(id, userId);
        mailMapper.toggleStar(mail.getId());
    }

    @Override
    public void deleteMail(Long id, Long userId) {
        Mail mail = requireOwnedMail(id, userId);
        mailMapper.moveToTrash(mail.getId());
    }

    @Override
    public void batchDeleteMails(List<Long> ids, Long userId) {
        if (ids != null && !ids.isEmpty()) {
            for (Long id : ids) {
                Mail mail = requireOwnedMail(id, userId);
                mailMapper.moveToTrash(mail.getId());
            }
        }
    }

    @Override
    @Transactional
    public void deletePermanently(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Mail mail = requireOwnedMail(id, userId);
            attachmentService.deleteByMailId(mail.getId());
        }
        mailMapper.deletePermanently(ids);
        log.info("Permanently deleted mails: {}", ids);
    }

    @Override
    @Transactional
    public void restoreMails(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            mailMapper.findByIdIncludingDeleted(id).ifPresent(mail -> {
                if (!Objects.equals(mail.getUserId(), userId)) {
                    throw new AccessDeniedException("无权恢复该邮件");
                }
                if (!Boolean.TRUE.equals(mail.getIsDeleted())) {
                    return;
                }
                String folder = mail.getFolder();
                String targetFolder = extractOriginalFolder(folder);
                mail.setFolder(targetFolder);
                mail.setIsDeleted(false);
                mailMapper.update(mail);
                log.info("Restored mail {} to folder {}", id, targetFolder);
            });
        }
    }

    // 从文件夹路径中提取原始文件夹名称
    private String extractOriginalFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "inbox";
        }
        if (folder.startsWith("trash:")) {
            String original = folder.substring("trash:".length());
            if (original == null || original.isBlank() || original.startsWith("trash")) {
                return "inbox";
            }
            return original;
        }
        if ("trash".equalsIgnoreCase(folder)) {
            return "inbox";
        }
        return folder;
    }

    @Override
    public java.util.Map<String, Integer> getMailStats(Long userId) {
        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        stats.put("unreadCount", mailMapper.countUnreadByUserId(userId));
        stats.put("draftsCount", mailMapper.countDraftsByUserId(userId));
        return stats;
    }

    @Override
    @Transactional
    public Mail sendDraft(Long userId, Long mailId) {
        Mail draft = requireOwnedMail(mailId, userId);

        if (!"drafts".equalsIgnoreCase(draft.getFolder())) {
            throw new IllegalArgumentException("该邮件不是草稿");
        }

        assertRecipientsPresent(draft.getToAddress(), draft.getCcAddress(), draft.getBccAddress());

        // 获取邮箱账号，格式化发件人地址以包含显示名称
        MailAccount account = mailAccountMapper.findById(draft.getAccountId())
            .orElseThrow(() -> new RuntimeException("邮箱账号不存在"));

        draft.setFolder("sent");
        draft.setFromAddress(formatSenderAddress(account));  // 格式化发件人地址
        draft.setSendTime(LocalDateTime.now());
        draft.setIsDeleted(false);
        draft.setIsRead(true);
        draft.setPriority(draft.getPriority() != null ? draft.getPriority() : 3);
        draft.setToAddress(aggregateRecipients(
            draft.getToAddress(), draft.getCcAddress(), draft.getBccAddress()
        ));

        mailMapper.update(draft);
        sendOutbound(draft);
        distributeToLocalRecipients(draft);
        log.info("草稿发送成功: id={}, to={}", draft.getId(), draft.getToAddress());
        return draft;
    }

    @Override
    @Transactional
    public Mail createMail(ParsedMail parsedMail) {
        log.info("Creating mail from SMTP: from={}, to={}, subject={}",
            parsedMail.getFrom(), parsedMail.getTo(), parsedMail.getSubject());

        // 根据收件人地址查找对应的用户和账户
        String toAddress = parsedMail.getTo().get(0); // 取第一个收件人
        Optional<MailAccount> accountOpt = mailAccountMapper.findByEmailAddress(toAddress);

        Mail mail = new Mail();
        if (accountOpt.isPresent()) {
            MailAccount account = accountOpt.get();
            mail.setUserId(account.getUserId());
            mail.setAccountId(account.getId());
        }
        mail.setFolder("inbox");
        mail.setFromAddress(parsedMail.getFrom());
        mail.setToAddress(String.join(",", parsedMail.getTo()));
        mail.setSubject(parsedMail.getSubject() != null ? parsedMail.getSubject() : "(无主题)");
        mail.setContent(parsedMail.getBody());
        mail.setPlainContent(parsedMail.getBody());
        mail.setIsRead(false);
        mail.setIsStarred(false);
        mail.setIsDeleted(false);
        mail.setHasAttachment(false);
        mail.setPriority(3);
        mail.setReceiveTime(LocalDateTime.now());

        mailMapper.insert(mail);

        log.info("Mail saved with id={}", mail.getId());
        return mail;
    }

    private void distributeToLocalRecipients(Mail sourceMail) {
        List<RecipientInfo> recipients = collectRecipientInfos(sourceMail);
        log.info("Distributing mail {} to {} recipients: {}", 
            sourceMail.getId(), recipients.size(), 
            recipients.stream().map(r -> r.address() + "(" + r.type() + ")").toList());
        if (recipients.isEmpty()) {
            return;
        }

        List<Attachment> sourceAttachments = Boolean.TRUE.equals(sourceMail.getHasAttachment())
            ? attachmentMapper.findByMailId(sourceMail.getId())
            : Collections.emptyList();

        for (RecipientInfo recipient : recipients) {
            String address = recipient.address();
            Optional<MailAccount> targetAccountOpt = mailAccountMapper.findByEmailAddress(address);
            if (targetAccountOpt.isEmpty()) {
                log.warn("Address '{}' is not bound to any local account, skip inbox delivery", address);
                continue;
            }

            MailAccount targetAccount = targetAccountOpt.get();
            Mail inboxMail = new Mail();
            inboxMail.setUserId(targetAccount.getUserId());
            inboxMail.setAccountId(targetAccount.getId());
            inboxMail.setFolder("inbox");
            inboxMail.setFromAddress(sourceMail.getFromAddress());
            if (recipient.type() == RecipientType.BCC) {
                inboxMail.setToAddress(address);
                inboxMail.setCcAddress(null);
            } else {
                inboxMail.setToAddress(sourceMail.getToAddress());
                inboxMail.setCcAddress(sourceMail.getCcAddress());
            }
            inboxMail.setBccAddress(null); // 收件箱副本不应暴露密送名单
            inboxMail.setSubject(sourceMail.getSubject());
            inboxMail.setContent(sourceMail.getContent());
            inboxMail.setPlainContent(sourceMail.getPlainContent());
            inboxMail.setIsRead(false);
            inboxMail.setIsStarred(false);
            inboxMail.setIsDeleted(false);
            inboxMail.setHasAttachment(sourceMail.getHasAttachment());
            inboxMail.setPriority(sourceMail.getPriority());
            inboxMail.setSendTime(sourceMail.getSendTime());
            inboxMail.setReceiveTime(LocalDateTime.now());

            mailMapper.insert(inboxMail);

            if (!sourceAttachments.isEmpty()) {
                copyAttachments(sourceAttachments, inboxMail.getId());
            }

            log.debug("Mail {} delivered to inbox of user {} via address {}",
                sourceMail.getId(), inboxMail.getUserId(), address);
        }
    }

    private List<RecipientInfo> collectRecipientInfos(Mail mail) {
        Map<String, RecipientInfo> normalized = new LinkedHashMap<>();
        addAddresses(normalized, mail.getBccAddress(), RecipientType.BCC);
        addAddresses(normalized, mail.getCcAddress(), RecipientType.CC);
        addAddresses(normalized, mail.getToAddress(), RecipientType.TO);
        return new ArrayList<>(normalized.values());
    }

    private void sanitizeMailCollection(List<Mail> mails) {
        if (mails == null || mails.isEmpty()) {
            return;
        }
        mails.forEach(this::sanitizeMailVisibility);
    }

    private void applyHighlight(List<Mail> mails, String keyword) {
        if (mails == null || mails.isEmpty() || !StringUtils.hasText(keyword)) {
            return;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        for (Mail mail : mails) {
            String subject = Optional.ofNullable(mail.getSubject()).orElse("");
            mail.setHighlightSubject(applyMarkup(subject, pattern, 120));

            String body = Optional.ofNullable(mail.getPlainContent())
                .orElse(Optional.ofNullable(mail.getContent()).orElse(""));
            mail.setHighlightSnippet(buildSnippet(body, pattern, 200));
        }
    }

    private String applyMarkup(String text, Pattern pattern, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String limited = text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
        Matcher matcher = pattern.matcher(limited);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<mark>" + Matcher.quoteReplacement(matcher.group()) + "</mark>");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String buildSnippet(String text, Pattern pattern, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int start = Math.max(0, matcher.start() - 40);
            int end = Math.min(text.length(), matcher.end() + 60);
            String snippet = text.substring(start, end);
            if (start > 0) {
                snippet = "..." + snippet;
            }
            if (end < text.length()) {
                snippet = snippet + "...";
            }
            return applyMarkup(snippet, pattern, maxLen);
        }
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private Mail requireOwnedMail(Long id, Long userId) {
        Mail mail = mailMapper.findByIdIncludingDeleted(id)
            .orElseThrow(() -> new RuntimeException("邮件不存在"));
        if (!Objects.equals(mail.getUserId(), userId)) {
            throw new AccessDeniedException("无权访问该邮件");
        }
        return mail;
    }

    private void sanitizeMailVisibility(Mail mail) {
        if (mail == null) {
            return;
        }
        String normalizedFolder = extractOriginalFolder(mail.getFolder());
        boolean senderFolders = "sent".equalsIgnoreCase(normalizedFolder)
            || "drafts".equalsIgnoreCase(normalizedFolder);
        if (!senderFolders) {
            mail.setBccAddress(null);
        }
    }

    private void assertRecipientsPresent(String to, String cc, String bcc) {
        boolean hasTo = StringUtils.hasText(to);
        boolean hasCc = StringUtils.hasText(cc);
        boolean hasBcc = StringUtils.hasText(bcc);
        if (!hasTo && !hasCc && !hasBcc) {
            throw new IllegalArgumentException("至少填写一个收件人/抄送/密送");
        }
    }

    private void addAddresses(Map<String, RecipientInfo> normalized, String rawAddresses, RecipientType type) {
        if (rawAddresses == null || rawAddresses.isBlank()) {
            return;
        }
        String[] parts = rawAddresses.split("[;,]");
        for (String part : parts) {
            String email = extractEmail(part);
            if (!email.isEmpty()) {
                normalized.putIfAbsent(email.toLowerCase(), new RecipientInfo(email, type));
            }
        }
    }

    private String aggregateRecipients(String... recipientGroups) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (recipientGroups != null) {
            for (String group : recipientGroups) {
                if (group == null || group.isBlank()) {
                    continue;
                }
                String[] parts = group.split("[;,]");
                for (String part : parts) {
                    String email = extractEmail(part);
                    if (!email.isEmpty()) {
                        normalized.putIfAbsent(email.toLowerCase(), email);
                    }
                }
            }
        }
        return String.join(",", normalized.values());
    }

    private enum RecipientType {
        TO,
        CC,
        BCC
    }

    private static class RecipientInfo {
        private final String address;
        private final RecipientType type;

        RecipientInfo(String address, RecipientType type) {
            this.address = address;
            this.type = type;
        }

        public String address() {
            return address;
        }

        public RecipientType type() {
            return type;
        }
    }

    private String extractEmail(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        int lt = value.indexOf('<');
        int gt = value.indexOf('>');
        if (lt >= 0 && gt > lt) {
            value = value.substring(lt + 1, gt);
        }
        return value.trim();
    }

    private void copyAttachments(List<Attachment> sourceAttachments, Long targetMailId) {
        if (sourceAttachments == null || sourceAttachments.isEmpty()) {
            return;
        }
        List<Attachment> clones = new ArrayList<>();
        for (Attachment attachment : sourceAttachments) {
            Attachment clone = new Attachment();
            clone.setMailId(targetMailId);
            clone.setFileName(attachment.getFileName());
            clone.setFileType(attachment.getFileType());
            clone.setFileSize(attachment.getFileSize());
            clone.setStoragePath(attachment.getStoragePath());
            clones.add(clone);
        }
        attachmentMapper.batchInsert(clones);
    }

    private void sendOutbound(Mail mail) {
        if (mail == null) {
            return;
        }
        List<Attachment> attachments = Boolean.TRUE.equals(mail.getHasAttachment())
            ? attachmentMapper.findByMailId(mail.getId())
            : Collections.emptyList();
        outboundMailSender.send(mail, attachments);
    }

    private MailAccount resolveMailAccount(Long userId, Long accountId) {
        if (accountId != null) {
            return mailAccountMapper.findById(accountId)
                .filter(acc -> Objects.equals(acc.getUserId(), userId))
                .orElseThrow(() -> new RuntimeException("发件邮箱不存在或没有权限"));
        }
        return mailAccountMapper.findDefaultByUserId(userId)
            .orElseThrow(() -> new RuntimeException("用户没有默认邮件账户"));
    }

    /**
     * 格式化发件人地址，包含显示名称
     * 例如: "张三 <zhangsan@campus.mail>" 或 "zhangsan@campus.mail"
     */
    private String formatSenderAddress(MailAccount account) {
        if (StringUtils.hasText(account.getDisplayName())) {
            return account.getDisplayName() + " <" + account.getEmailAddress() + ">";
        }
        return account.getEmailAddress();
    }
}
