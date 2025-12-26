package com.campusmail.mapper;

import com.campusmail.dto.ContactStatDTO;
import com.campusmail.dto.DailyMailCountDTO;
import com.campusmail.entity.Mail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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
    
    // ===== 邮件统计相关方法 =====
    
    /**
     * 统计用户总邮件数
     */
    Long countTotalByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户收件数
     */
    Long countReceivedByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户发件数
     */
    Long countSentByUserId(@Param("userId") Long userId);
    
    /**
     * 统计星标邮件数
     */
    Long countStarredByUserId(@Param("userId") Long userId);
    
    /**
     * 统计有附件的邮件数
     */
    Long countWithAttachmentByUserId(@Param("userId") Long userId);
    
    /**
     * 统计指定时间范围内的收件数
     */
    Long countReceivedByUserIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 统计指定时间范围内的发件数
     */
    Long countSentByUserIdAndTimeRange(
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 获取每日邮件数量统计（最近N天）
     */
    List<DailyMailCountDTO> getDailyMailCounts(
        @Param("userId") Long userId,
        @Param("days") int days
    );
    
    /**
     * 获取常用联系人统计（Top N）
     */
    List<ContactStatDTO> getTopContacts(
        @Param("userId") Long userId,
        @Param("limit") int limit
    );
    
    /**
     * 统计邮件响应时间
     */
    List<Mail> getMailsForResponseTimeAnalysis(@Param("userId") Long userId);
}
