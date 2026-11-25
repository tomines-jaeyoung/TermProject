package kr.ac.kopo.kim.onairmarket.ml;

import java.util.*;

/**
 * 내용 기반 추천 (Content-based filtering): TF-IDF 벡터 + 코사인 유사도.
 *
 * <ol>
 *   <li>토큰화 – 한글은 형태소 분석기 없이 글자 2-gram, 영문·숫자는 단어 단위로 자른다.
 *       (예: "빈티지 데님" → 빈티, 티지, 데님)</li>
 *   <li>TF – 문서 안에서 토큰이 나온 비율. 상품명은 2배 가중치를 준다.</li>
 *   <li>IDF – log((1 + N) / (1 + df)) + 1. 많은 상품에 공통으로 나오는 토큰일수록 낮아진다.</li>
 *   <li>TF × IDF 벡터를 L2 정규화하면, 두 벡터의 내적이 곧 코사인 유사도(0~1)가 된다.</li>
 * </ol>
 * 외부 라이브러리 없이 구현했고, Spring 과 무관한 순수 Java 클래스라 단위 테스트로 검증한다.
 */
public class TfIdfRecommender {

    /** 학습 데이터 1건: 상품 ID, 상품명, 설명, 카테고리 */
    public record Item(String id, String title, String description, String category) {
    }

    /** 추천 결과: 상품 ID 와 코사인 유사도 */
    public record Scored(String id, double score) {
    }

    /** 의미 없이 자주 나오는 2-gram (설명 문장의 어미 등) */
    private static final Set<String> STOP = Set.of(
            "니다", "합니", "입니", "습니", "있습", "드립", "드려", "주세", "세요", "해주", "에서", "으로", "하고",
            "the", "and", "for");

    private final Map<String, Map<String, Double>> vectors = new LinkedHashMap<>();
    private final Map<String, Double> idf = new HashMap<>();

    public TfIdfRecommender(Collection<Item> items) {
        // 1) 문서별 토큰 빈도(TF 원본)
        Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
        Map<String, Integer> df = new HashMap<>();
        for (Item it : items) {
            Map<String, Integer> c = new HashMap<>();
            for (String t : tokenize(it.title())) c.merge(t, 2, Integer::sum);          // 상품명 가중치 2
            for (String t : tokenize(it.description())) c.merge(t, 1, Integer::sum);
            for (String t : tokenize(it.category())) c.merge(t, 1, Integer::sum);
            counts.put(it.id(), c);
            for (String t : c.keySet()) df.merge(t, 1, Integer::sum);
        }
        // 2) IDF (smooth)
        int n = counts.size();
        df.forEach((t, d) -> idf.put(t, Math.log((1.0 + n) / (1.0 + d)) + 1.0));
        // 3) TF-IDF 벡터 + L2 정규화
        counts.forEach((id, c) -> {
            double total = c.values().stream().mapToInt(Integer::intValue).sum();
            Map<String, Double> v = new HashMap<>();
            c.forEach((t, k) -> v.put(t, (k / total) * idf.get(t)));
            double norm = Math.sqrt(v.values().stream().mapToDouble(x -> x * x).sum());
            if (norm > 0) v.replaceAll((t, x) -> x / norm);
            vectors.put(id, v);
        });
    }

    /** 기준 상품과 코사인 유사도가 높은 순서. 자기 자신과 유사도 0 인 상품은 뺀다. */
    public List<Scored> similarTo(String id) {
        Map<String, Double> base = vectors.get(id);
        if (base == null || base.isEmpty()) return List.of();
        List<Scored> out = new ArrayList<>();
        vectors.forEach((other, v) -> {
            if (other.equals(id)) return;
            double s = cosine(base, v);
            if (s > 0) out.add(new Scored(other, s));
        });
        out.sort(Comparator.comparingDouble(Scored::score).reversed().thenComparing(Scored::id));
        return out;
    }

    public int size() {
        return vectors.size();
    }

    public int vocabularySize() {
        return idf.size();
    }

    /** 정규화된 두 벡터의 내적 = 코사인 유사도 */
    static double cosine(Map<String, Double> a, Map<String, Double> b) {
        Map<String, Double> small = a.size() <= b.size() ? a : b, large = small == a ? b : a;
        double dot = 0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double x = large.get(e.getKey());
            if (x != null) dot += e.getValue() * x;
        }
        return Math.min(1.0, dot);
    }

    /** 한글 → 글자 2-gram, 영문·숫자 → 단어(2글자 이상 또는 숫자) */
    static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.isEmpty()) continue;
            if (word.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HANGUL)) {
                if (word.length() == 1) continue;
                for (int i = 0; i + 2 <= word.length(); i++) {
                    String g = word.substring(i, i + 2);
                    if (!STOP.contains(g)) out.add(g);
                }
            } else if ((word.length() >= 2 || word.chars().allMatch(Character::isDigit)) && !STOP.contains(word)) {
                out.add(word);
            }
        }
        return out;
    }
}
