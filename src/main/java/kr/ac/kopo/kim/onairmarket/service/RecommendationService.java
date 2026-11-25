package kr.ac.kopo.kim.onairmarket.service;

import kr.ac.kopo.kim.onairmarket.domain.TimedAuction;
import kr.ac.kopo.kim.onairmarket.ml.TfIdfRecommender;
import kr.ac.kopo.kim.onairmarket.repository.TimedAuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 경매 상세 화면의 '이 상품과 비슷한 경매' (머신러닝 추천).
 * DB(JPA)에서 공개 경매를 읽어 TF-IDF 모델을 만들고, 경매가 추가·수정되면(개수·최종 수정 시각 변화) 다시 학습한다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {
    public static final int LIMIT = 4;
    /** 이보다 낮은 유사도(공통 단어가 거의 없는 상품)는 추천하지 않는다 */
    public static final double MIN_SCORE = 0.05;

    private final TimedAuctionRepository auctionRepository;

    private volatile Model cache;

    /** 추천 1건: 경매와 유사도(0~1) */
    public record Recommendation(TimedAuction auction, double score) {
        public int percent() {
            return (int) Math.round(score * 100);
        }
    }

    private record Model(long version, TfIdfRecommender recommender, Map<String, TimedAuction> auctions) {
    }

    @Transactional(readOnly = true)
    public List<Recommendation> similar(String auctionId) {
        Model m = model();
        long now = System.currentTimeMillis();
        return m.recommender().similarTo(auctionId).stream()
                .map(s -> new Recommendation(m.auctions().get(s.id()), s.score()))
                .filter(r -> r.score() >= MIN_SCORE)
                .filter(r -> r.auction() != null && r.auction().getEndsAt() > now)   // 끝난 경매는 추천하지 않음
                .limit(LIMIT)
                .toList();
    }

    /** 공개 경매 전체로 학습한 모델. 데이터가 바뀌었을 때만 다시 만든다. */
    private Model model() {
        List<TimedAuction> all = auctionRepository.findByArchivedFalse();
        long version = all.size() * 31L + all.stream().mapToLong(TimedAuction::getUpdated).max().orElse(0);
        Model m = cache;
        if (m != null && m.version() == version) return m;
        TfIdfRecommender rec = new TfIdfRecommender(all.stream()
                .map(a -> new TfIdfRecommender.Item(a.getId(), a.getTitle(), a.getDescription(), a.getCategory()))
                .toList());
        m = new Model(version, rec, all.stream().collect(Collectors.toMap(TimedAuction::getId, Function.identity())));
        cache = m;
        return m;
    }
}
