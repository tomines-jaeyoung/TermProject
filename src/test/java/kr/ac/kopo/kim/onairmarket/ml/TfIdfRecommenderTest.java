package kr.ac.kopo.kim.onairmarket.ml;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TfIdfRecommenderTest {

    private final TfIdfRecommender rec = new TfIdfRecommender(List.of(
            new TfIdfRecommender.Item("denim", "빈티지 데님 셀렉트 3종", "90년대 빈티지 데님 재킷과 청바지입니다.", "빈티지"),
            new TfIdfRecommender.Item("jacket", "빈티지 워크 재킷 L", "안감 상태가 좋은 빈티지 워크 재킷입니다.", "남성 패션"),
            new TfIdfRecommender.Item("watch", "스포츠 다이버 워치", "정상 작동하는 다이버 워치입니다.", "시계"),
            new TfIdfRecommender.Item("card", "포켓몬 카드 슬리브 세트", "카드 보관용 슬리브입니다.", "포켓몬 카드"),
            new TfIdfRecommender.Item("box", "트레이딩 카드 미개봉 박스", "미개봉 카드 박스입니다.", "TCG")));

    @Test
    void koreanIsSplitIntoCharacterBigrams() {
        assertEquals(List.of("빈티", "티지", "데님"), TfIdfRecommender.tokenize("빈티지 데님"));
        assertEquals(List.of("tcg", "270mm", "3"), TfIdfRecommender.tokenize("TCG 270mm 3"));
        assertTrue(TfIdfRecommender.tokenize("재킷입니다").contains("재킷"));
        assertFalse(TfIdfRecommender.tokenize("재킷입니다").contains("니다"));   // 불용어 제거
        assertTrue(TfIdfRecommender.tokenize(null).isEmpty());
    }

    @Test
    void similarItemsRankFirst() {
        var top = rec.similarTo("denim");
        assertEquals("jacket", top.get(0).id());          // 빈티지·재킷을 공유
        assertTrue(top.stream().noneMatch(s -> s.id().equals("denim")));   // 자기 자신 제외
        assertEquals("box", rec.similarTo("card").get(0).id());           // 카드를 공유
    }

    @Test
    void unrelatedItemsAreNotRecommended() {
        assertTrue(rec.similarTo("watch").stream().noneMatch(s -> s.id().equals("card")));
    }

    @Test
    void scoresAreSortedCosineValuesBetweenZeroAndOne() {
        var top = rec.similarTo("denim");
        for (int i = 0; i < top.size(); i++) {
            assertTrue(top.get(i).score() > 0 && top.get(i).score() <= 1.0);
            if (i > 0) assertTrue(top.get(i - 1).score() >= top.get(i).score());
        }
    }

    @Test
    void identicalVectorsHaveCosineOne() {
        Map<String, Double> v = Map.of("a", 0.6, "b", 0.8);
        assertEquals(1.0, TfIdfRecommender.cosine(v, v), 1e-9);
        assertEquals(0.0, TfIdfRecommender.cosine(v, Map.of("c", 1.0)), 1e-9);
    }

    @Test
    void unknownIdReturnsEmpty() {
        assertTrue(rec.similarTo("nope").isEmpty());
        assertEquals(5, rec.size());
    }
}
