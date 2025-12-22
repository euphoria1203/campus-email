package com.campusmail.mapper;

import com.campusmail.entity.Mail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MailMapper {
    void insert(Mail mail);

    void update(Mail mail);

    void delete(@Param("id") Long id);

    Optional<Mail> findById(@Param("id") Long id);

    Optional<Mail> findByIdIncludingDeleted(@Param("id") Long id);

    List<Mail> findByUserId(@Param("userId") Long userId);

    List<Mail> findByUserIdAndFolder(@Param("userId") Long userId, @Param("folder") String folder);

    List<Mail> findStarredByUserId(@Param("userId") Long userId);

    List<Mail> findTrashByUserId(@Param("userId") Long userId);

    List<Mail> findByAccountId(@Param("accountId") Long accountId);

    void markAsRead(@Param("id") Long id);

    void toggleStar(@Param("id") Long id);

    void moveToTrash(@Param("id") Long id);

    void batchDelete(@Param("ids") List<Long> ids);

    void deletePermanently(@Param("ids") List<Long> ids);

    List<Mail> search(@Param("userId") Long userId,
                      @Param("keyword") String keyword,
                      @Param("folder") String folder,
                      @Param("accountId") Long accountId,
                      @Param("limit") int limit,
                      @Param("offset") int offset);

    List<Mail> findScheduledDue(@Param("now") java.time.LocalDateTime now,
                                @Param("limit") int limit);

    // 统计未读邮件数量
    int countUnreadByUserId(@Param("userId") Long userId);

    // 统计草稿箱邮件数量
    int countDraftsByUserId(@Param("userId") Long userId);
}
