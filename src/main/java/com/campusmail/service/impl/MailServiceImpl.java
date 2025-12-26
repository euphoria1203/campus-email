package com.campusmail.service.impl;

import com.campusmail.dto.ContactStatDTO;
import com.campusmail.dto.DailyMailCountDTO;
import com.campusmail.dto.MailDTO;
import com.campusmail.dto.MailStatisticsDTO;
import com.campusmail.dto.ResponseTimeStatDTO;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final String INTERNAL_EMAIL_DOMAIN = "@campus.mail";

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

        Mail mail = new Mail();
        mail.setUserId(userId);
        mail.setAccountId(account.getId());
        mail.setFolder("sent");
        mail.setFromAddress(formatSenderAddress(account));
        mail.setToAddress(request.getToAddress());
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
        mail.setToAddress(request.getToAddress());
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

        mailMapper.update(draft);
        sendOutbound(draft);
        distributeToLocalRecipients(draft);
        log.info("草稿发送成功: id={}, to={}", draft.getId(), draft.getToAddress());
        return draft;
    }

    @Override
    @Transactional
    public Mail scheduleMail(Long userId, MailDTO request) {
        assertRecipientsPresent(request.getToAddress(), request.getCcAddress(), request.getBccAddress());
        if (!Objects.equals(userId, request.getUserId())) {
            request.setUserId(userId);
        }

        LocalDateTime scheduledTime = parseScheduledTime(request.getScheduledTime());
        if (scheduledTime == null) {
            throw new IllegalArgumentException("scheduledTime is required");
        }
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("scheduledTime must be in the future");
        }

        MailAccount account = resolveMailAccount(userId, request.getAccountId());

        Mail mail = new Mail();
        mail.setUserId(userId);
        mail.setAccountId(account.getId());
        mail.setFolder("scheduled");
        mail.setFromAddress(formatSenderAddress(account));
        mail.setToAddress(request.getToAddress());
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
        mail.setSendTime(scheduledTime);

        mailMapper.insert(mail);

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            for (Long attachmentId : request.getAttachmentIds()) {
                attachmentMapper.updateMailId(attachmentId, mail.getId());
            }
        }

        if (request.getId() != null) {
            mailMapper.findById(request.getId()).ifPresent(draft -> {
                if ("drafts".equals(draft.getFolder())) {
                    mailMapper.delete(request.getId());
                    log.info("??????: id={}", request.getId());
                }
            });
        }

        log.info("???????: id={}, scheduledTime={}", mail.getId(), scheduledTime);
        return mail;
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
        Mail outboundMail = buildOutboundMail(mail);
        if (outboundMail == null) {
            log.info("Skip outbound SMTP: internal-only recipients mailId={}", mail.getId());
            return;
        }
        List<Attachment> attachments = Boolean.TRUE.equals(mail.getHasAttachment())
            ? attachmentMapper.findByMailId(mail.getId())
            : Collections.emptyList();
        outboundMailSender.send(outboundMail, attachments);
    }

    private Mail buildOutboundMail(Mail source) {
        String toAddress = filterExternalRecipients(source.getToAddress());
        String ccAddress = filterExternalRecipients(source.getCcAddress());
        String bccAddress = filterExternalRecipients(source.getBccAddress());

        if (!StringUtils.hasText(toAddress) && !StringUtils.hasText(ccAddress) && !StringUtils.hasText(bccAddress)) {
            return null;
        }

        Mail mail = new Mail();
        mail.setId(source.getId());
        mail.setAccountId(source.getAccountId());
        mail.setFromAddress(source.getFromAddress());
        mail.setToAddress(toAddress);
        mail.setCcAddress(ccAddress);
        mail.setBccAddress(bccAddress);
        mail.setSubject(source.getSubject());
        mail.setContent(source.getContent());
        mail.setPlainContent(source.getPlainContent());
        return mail;
    }

    private String filterExternalRecipients(String rawAddresses) {
        if (!StringUtils.hasText(rawAddresses)) {
            return null;
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        String[] parts = rawAddresses.split("[;,]");
        for (String part : parts) {
            String email = extractEmail(part);
            if (!StringUtils.hasText(email)) {
                continue;
            }
            if (isInternalAddress(email)) {
                continue;
            }
            normalized.putIfAbsent(email.toLowerCase(), email);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join(",", normalized.values());
    }

    private boolean isInternalAddress(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return email.trim().toLowerCase().endsWith(INTERNAL_EMAIL_DOMAIN);
    }

    private LocalDateTime parseScheduledTime(String scheduledTime) {
        if (!StringUtils.hasText(scheduledTime)) {
            return null;
        }
        String value = scheduledTime.trim();
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid scheduledTime format. Expect ISO_LOCAL_DATE_TIME.");
        }
    }

    @Scheduled(fixedDelayString = "${mail.schedule.dispatch-interval-ms:60000}")
    @Transactional
    public void dispatchScheduledMails() {
        List<Mail> dueMails = mailMapper.findScheduledDue(LocalDateTime.now(), 50);
        if (dueMails.isEmpty()) {
            return;
        }
        for (Mail mail : dueMails) {
            try {
                sendOutbound(mail);
                distributeToLocalRecipients(mail);
                mail.setFolder("sent");
                mail.setIsDeleted(false);
                mail.setIsRead(true);
                mail.setSendTime(LocalDateTime.now());
                mailMapper.update(mail);
                log.info("Scheduled mail sent: id={}, to={}", mail.getId(), mail.getToAddress());
            } catch (Exception ex) {
                log.error("Failed to send scheduled mail: id={}", mail.getId(), ex);
            }
        }
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
    
    @Override
    public MailStatisticsDTO getMailStatistics(Long userId) {
        MailStatisticsDTO statistics = new MailStatisticsDTO();
        
        // 基础统计
        statistics.setTotalMails(mailMapper.countTotalByUserId(userId));
        statistics.setReceivedMails(mailMapper.countReceivedByUserId(userId));
        statistics.setSentMails(mailMapper.countSentByUserId(userId));
        statistics.setDraftMails((long) mailMapper.countDraftsByUserId(userId));
        statistics.setUnreadMails((long) mailMapper.countUnreadByUserId(userId));
        statistics.setStarredMails(mailMapper.countStarredByUserId(userId));
        statistics.setMailsWithAttachment(mailMapper.countWithAttachmentByUserId(userId));
        
        // 时间范围统计
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);
        
        statistics.setTodayReceived(mailMapper.countReceivedByUserIdAndTimeRange(userId, todayStart, todayEnd));
        statistics.setTodaySent(mailMapper.countSentByUserIdAndTimeRange(userId, todayStart, todayEnd));
        statistics.setWeekReceived(mailMapper.countReceivedByUserIdAndTimeRange(userId, weekStart, now));
        statistics.setWeekSent(mailMapper.countSentByUserIdAndTimeRange(userId, weekStart, now));
        statistics.setMonthReceived(mailMapper.countReceivedByUserIdAndTimeRange(userId, monthStart, now));
        statistics.setMonthSent(mailMapper.countSentByUserIdAndTimeRange(userId, monthStart, now));
        
        // 每日邮件数量统计（最近30天）
        List<DailyMailCountDTO> dailyCounts = mailMapper.getDailyMailCounts(userId, 30);
        statistics.setDailyMailCounts(fillMissingDates(dailyCounts, 30));
        
        // 常用联系人统计（Top 10）
        List<ContactStatDTO> topContacts = mailMapper.getTopContacts(userId, 10);
        statistics.setTopContacts(topContacts);
        
        // 邮件响应时间统计
        ResponseTimeStatDTO responseTimeStats = calculateResponseTime(userId);
        statistics.setResponseTimeStats(responseTimeStats);
        
        return statistics;
    }
    
    /**
     * 填充缺失的日期数据，确保每一天都有数据点
     */
    private List<DailyMailCountDTO> fillMissingDates(List<DailyMailCountDTO> dailyCounts, int days) {
        Map<String, DailyMailCountDTO> countMap = new LinkedHashMap<>();
        
        // 初始化所有日期为0
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            countMap.put(date, new DailyMailCountDTO(date, 0L, 0L));
        }
        
        // 填充实际数据
        for (DailyMailCountDTO count : dailyCounts) {
            countMap.put(count.getDate(), count);
        }
        
        return new ArrayList<>(countMap.values());
    }
    
    /**
     * 计算邮件响应时间统计
     */
    private ResponseTimeStatDTO calculateResponseTime(Long userId) {
        ResponseTimeStatDTO stats = new ResponseTimeStatDTO();
        
        // 获取最近的发送邮件
        List<Mail> sentMails = mailMapper.getMailsForResponseTimeAnalysis(userId);
        
        if (sentMails.isEmpty()) {
            stats.setTotalResponses(0L);
            stats.setAvgResponseTimeMinutes(0.0);
            stats.setMinResponseTimeMinutes(0.0);
            stats.setMaxResponseTimeMinutes(0.0);
            stats.setWithinOneHour(0L);
            stats.setWithinOneDay(0L);
            stats.setWithinOneWeek(0L);
            stats.setOverOneWeek(0L);
            return stats;
        }
        
        List<Double> responseTimes = new ArrayList<>();
        long withinOneHour = 0;
        long withinOneDay = 0;
        long withinOneWeek = 0;
        long overOneWeek = 0;
        
        // 分析每封发送邮件的响应时间
        for (Mail sentMail : sentMails) {
            // 查找对应的回复邮件（收件箱中，发件人是原收件人）
            List<String> recipients = parseRecipients(sentMail.getToAddress());
            
            for (String recipient : recipients) {
                List<Mail> replies = mailMapper.findByUserIdAndFolder(userId, "inbox").stream()
                    .filter(m -> m.getFromAddress().contains(recipient) 
                                && m.getReceiveTime() != null 
                                && m.getReceiveTime().isAfter(sentMail.getSendTime()))
                    .toList();
                
                if (!replies.isEmpty()) {
                    // 找到最早的回复
                    Mail earliestReply = replies.stream()
                        .min((m1, m2) -> m1.getReceiveTime().compareTo(m2.getReceiveTime()))
                        .orElse(null);
                    
                    if (earliestReply != null) {
                        Duration duration = Duration.between(sentMail.getSendTime(), earliestReply.getReceiveTime());
                        double minutes = duration.toMinutes();
                        responseTimes.add(minutes);
                        
                        // 按时间段分类
                        if (minutes <= 60) {
                            withinOneHour++;
                        } else if (minutes <= 1440) { // 24小时
                            withinOneDay++;
                        } else if (minutes <= 10080) { // 7天
                            withinOneWeek++;
                        } else {
                            overOneWeek++;
                        }
                    }
                }
            }
        }
        
        stats.setTotalResponses((long) responseTimes.size());
        stats.setWithinOneHour(withinOneHour);
        stats.setWithinOneDay(withinOneDay);
        stats.setWithinOneWeek(withinOneWeek);
        stats.setOverOneWeek(overOneWeek);
        
        if (!responseTimes.isEmpty()) {
            stats.setAvgResponseTimeMinutes(responseTimes.stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0));
            stats.setMinResponseTimeMinutes(responseTimes.stream()
                .mapToDouble(Double::doubleValue).min().orElse(0.0));
            stats.setMaxResponseTimeMinutes(responseTimes.stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0));
        } else {
            stats.setAvgResponseTimeMinutes(0.0);
            stats.setMinResponseTimeMinutes(0.0);
            stats.setMaxResponseTimeMinutes(0.0);
        }
        
        return stats;
    }
    
    /**
     * 解析收件人地址列表
     */
    private List<String> parseRecipients(String toAddress) {
        if (toAddress == null || toAddress.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> recipients = new ArrayList<>();
        String[] addresses = toAddress.split(",");
        
        for (String address : addresses) {
            String email = extractEmail(address.trim());
            if (!email.isEmpty()) {
                recipients.add(email);
            }
        }
        
        return recipients;
    }
}
