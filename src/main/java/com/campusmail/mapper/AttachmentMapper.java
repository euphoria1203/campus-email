package com.campusmail.mapper;

import com.campusmail.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentMapper {
    void insert(Attachment attachment);
    
    void batchInsert(@Param("attachments") List<Attachment> attachments);

    List<Attachment> findByMailId(@Param("mailId") Long mailId);

    Attachment findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);

    void deleteByMailId(@Param("mailId") Long mailId);

    void updateMailId(@Param("id") Long id, @Param("mailId") Long mailId);
}
