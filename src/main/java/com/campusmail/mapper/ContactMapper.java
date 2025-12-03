package com.campusmail.mapper;

import com.campusmail.entity.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContactMapper {
    void insert(Contact contact);

    void update(Contact contact);

    void delete(@Param("id") Long id);

    List<Contact> findByUserId(@Param("userId") Long userId);

    /**
     * 根据关键词搜索联系人（模糊匹配姓名或邮箱）
     */
    List<Contact> search(@Param("userId") Long userId, @Param("keyword") String keyword);
}
