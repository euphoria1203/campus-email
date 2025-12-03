package com.campusmail.service;

import com.campusmail.entity.Contact;

import java.util.List;

public interface ContactService {
    Contact create(Contact contact);

    Contact update(Contact contact);

    void delete(Long id);

    List<Contact> listByUser(Long userId);

    /**
     * 搜索联系人
     */
    List<Contact> search(Long userId, String keyword);
}
