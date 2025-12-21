package com.campusmail.service.impl;

import com.campusmail.entity.Attachment;
import com.campusmail.mapper.AttachmentMapper;
import com.campusmail.service.AttachmentService;
import com.campusmail.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final Path uploadPath;
    private final SnowflakeIdGenerator idGen;

    public AttachmentServiceImpl(AttachmentMapper attachmentMapper,
                                  @Value("${file.upload-dir:./uploads}") String uploadDir,
                                  SnowflakeIdGenerator idGen) {
        this.attachmentMapper = attachmentMapper;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.idGen = idGen;

        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录", e);
        }
    }

    @Override
    @Transactional
    public Attachment upload(MultipartFile file, Long mailId) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("文件名包含无效路径序列: " + originalFileName);
            }

            Path targetLocation = this.uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = new Attachment();
            attachment.setId(idGen.nextId());
            attachment.setMailId(mailId);
            attachment.setFileName(originalFileName);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(storedFileName);

            attachmentMapper.insert(attachment);
            return attachment;

        } catch (IOException e) {
            throw new RuntimeException("无法存储文件: " + originalFileName, e);
        }
    }

    @Override
    @Transactional
    public List<Attachment> uploadMultiple(MultipartFile[] files, Long mailId) {
        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                attachments.add(upload(file, mailId));
            }
        }
        return attachments;
    }

    @Override
    public List<Attachment> getByMailId(Long mailId) {
        return attachmentMapper.findByMailId(mailId);
    }

    @Override
    public Attachment getById(Long id) {
        return attachmentMapper.findById(id);
    }

    @Override
    public Resource download(Long id) {
        Attachment attachment = attachmentMapper.findById(id);
        if (attachment == null) {
            throw new RuntimeException("附件不存在");
        }

        try {
            Path filePath = this.uploadPath.resolve(attachment.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("文件不存在");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("文件路径错误", e);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Attachment attachment = attachmentMapper.findById(id);
        if (attachment != null) {
            try {
                Path filePath = this.uploadPath.resolve(attachment.getStoragePath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // 忽略文件删除错误，继续删除数据库记录
            }
            attachmentMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public void deleteByMailId(Long mailId) {
        List<Attachment> attachments = attachmentMapper.findByMailId(mailId);
        for (Attachment attachment : attachments) {
            try {
                Path filePath = this.uploadPath.resolve(attachment.getStoragePath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // 忽略文件删除错误
            }
        }
        attachmentMapper.deleteByMailId(mailId);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
