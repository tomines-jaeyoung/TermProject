package kr.ac.kopo.kim.onairmarket.repository;

import kr.ac.kopo.kim.onairmarket.domain.TimedAuction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimedAuctionRepository extends JpaRepository<TimedAuction, String> {

    /** 목록 탐색: 카테고리(선택)·검색어(선택), 보관되지 않은 경매만 */
    @Query("""
            select a from TimedAuction a
            where a.archived = false
              and (:categoryId is null or a.categoryId = :categoryId)
              and (:keyword is null or lower(a.title) like lower(concat('%', :keyword, '%')))
              and (:openOnly = false or (a.startsAt <= :now and a.endsAt > :now))
              and (:videoOnly = false or a.video <> '')
            """)
    Page<TimedAuction> search(@Param("categoryId") String categoryId,
                              @Param("keyword") String keyword,
                              @Param("openOnly") boolean openOnly,
                              @Param("videoOnly") boolean videoOnly,
                              @Param("now") long now,
                              Pageable pageable);

    List<TimedAuction> findByOwnerIdAndArchivedFalseOrderByCreatedDesc(Long ownerId);

    /** 관리자: 보관된 경매까지 포함해 제목·판매자 검색 */
    @Query("""
            select a from TimedAuction a
            where :keyword is null or lower(a.title) like lower(concat('%', :keyword, '%'))
               or lower(a.seller) like lower(concat('%', :keyword, '%'))
            """)
    Page<TimedAuction> adminSearch(@Param("keyword") String keyword, Pageable pageable);

    long countByArchivedTrue();

    /** 추천 모델 학습용: 공개(숨기지 않은) 경매 전체 */
    List<TimedAuction> findByArchivedFalse();

    long countByArchivedFalseAndStartsAtLessThanEqualAndEndsAtGreaterThan(long startedBy, long endsAfter);

    /** 홈 '곧 낙찰됩니다': 진행 중인 경매를 마감이 가까운 순으로 4개 */
    List<TimedAuction> findTop4ByArchivedFalseAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByEndsAtAsc(long startedBy, long endsAfter);

    /** 홈 '영상으로 만나는 경매': 상품 소개 영상이 있고 아직 끝나지 않은 경매 (최근 등록 순 12개) */
    List<TimedAuction> findTop12ByArchivedFalseAndVideoNotAndEndsAtGreaterThanOrderByCreatedDesc(String emptyVideo, long now);

    /** 홈 '곧 시작하는 경매': 시작 전 경매를 시작이 가까운 순으로 4개 */
    List<TimedAuction> findTop4ByArchivedFalseAndStartsAtGreaterThanOrderByStartsAtAsc(long now);

    /** 홈 '사진으로 보고, 경매에 참여하세요': 최근 등록 경매 8개 */
    List<TimedAuction> findTop8ByArchivedFalseOrderByCreatedDesc();

    /**
     * 조건부 갱신 입찰. 서버가 확인한 revision·현재가가 그대로이고, 본인 경매가 아니며,
     * 입찰 기간 안일 때만 1행이 바뀐다. 0이면 다른 입찰이 먼저 반영된 충돌이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TimedAuction a
               set a.price = :amount, a.leader = :leader, a.revision = a.revision + 1,
                   a.bidCount = a.bidCount + 1, a.updated = :now
             where a.id = :id and a.owner.id <> :bidderId and a.revision = :revision
               and a.price = :expectedPrice and a.startsAt <= :now and a.endsAt > :now and a.archived = false
            """)
    int applyBid(@Param("id") String id, @Param("bidderId") Long bidderId, @Param("revision") long revision,
                 @Param("expectedPrice") long expectedPrice, @Param("amount") long amount,
                 @Param("leader") String leader, @Param("now") long now);
}
