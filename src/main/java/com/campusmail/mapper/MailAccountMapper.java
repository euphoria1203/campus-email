package com.campusmail.mapper;

import com.campusmail.entity.MailAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MailAccountMapper {
    void insert(MailAccount account);

    void update(MailAccount account);

    void delete(@Param("id") Long id);

    Optional<MailAccount> findById(@Param("id") Long id);

    List<MailAccount> findByUserId(@Param("userId") Long userId);

    Optional<MailAccount> findDefaultByUserId(@Param("userId") Long userId);

    Optional<MailAccount> findByEmailAddress(@Param("emailAddress") String emailAddress);

    void clearDefaultByUserId(@Param("userId") Long userId);

    int countDefaultByUserId(@Param("userId") Long userId);

    void updateDefaultFlag(@Param("id") Long id, @Param("isDefault") boolean isDefault);
}
