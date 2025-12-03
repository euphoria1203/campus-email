package com.campusmail.service;

import com.campusmail.entity.Attachment;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    /**
     * 上传附件
     */
    Attachment upload(MultipartFile file, Long mailId);

    /**
     * 批量上传附件
     */
    List<Attachment> uploadMultiple(MultipartFile[] files, Long mailId);

    /**
     * 获取邮件的所有附件
     */
    List<Attachment> getByMailId(Long mailId);

    /**
     * 根据ID获取附件
     */
    Attachment getById(Long id);

    /**
     * 下载附件
     */
    Resource download(Long id);

    /**
     * 删除附件
     */
    void delete(Long id);

    /**
     * 删除邮件的所有附件
     */
    void deleteByMailId(Long mailId);
}
