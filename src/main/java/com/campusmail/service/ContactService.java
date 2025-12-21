package com.campusmail.service;

import com.campusmail.entity.Contact;

import java.util.List;

public interface ContactService {
    Contact create(Long userId, Contact contact);

    Contact update(Long userId, Contact contact);

    void delete(Long userId, Long id);

    List<Contact> listByUser(Long userId);

    /**
     * 搜索联系人
     */
    List<Contact> search(Long userId, String keyword);
}
