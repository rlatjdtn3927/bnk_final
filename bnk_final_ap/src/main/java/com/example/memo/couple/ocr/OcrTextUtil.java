package com.example.memo.couple.ocr;

import java.text.Normalizer;

public final class OcrTextUtil {
    private OcrTextUtil() {}

    /** 화면 표시용 이름: 괄호(한자 등) 통째 제거 + 기호 제거 + 공백 정리 */
    public static String tidyNameForDisplay(String s) {
        if (s == null) return null;
        String x = nfkc(s);
        // (한자) 괄호 내용 통째 제거
        x = x.replaceAll("\\s*[\\(\\[{<][^\\)\\]}>]*[\\)\\]}>]\\s*", " ");
        // 점/중점/불릿/하이픈/밑줄 등 노이즈 제거
        x = x.replaceAll("[·•ㆍ・‧∙•⸱·\\-—_~`'\"^]+", " ");
        // 한글/영문/공백만 남김
        x = x.replaceAll("[^\\p{IsHangul}A-Za-z\\s]", " ");
        // 다중 공백 압축
        return x.trim().replaceAll("\\s{2,}", " ");
    }

    /** 비교용 이름: 표시용 정리 후 공백 제거 + 소문자 */
    public static String normalizeNameForCompare(String s) {
        String t = tidyNameForDisplay(s);
        return t == null ? null : t.replaceAll("\\s+", "").toLowerCase();
    }

    /** 생년월일(자유형식) → yyyy-MM-dd */
    public static String normalizeBirth(String s) {
        if (s == null) return null;
        String d = s.replaceAll("\\D", "");
        if (d.length() >= 8) {
            return d.substring(0,4) + "-" + d.substring(4,6) + "-" + d.substring(6,8);
        }
        return null;
    }

    /**
     * 주민번호에서 생년월일 파생: ######-S######
     * 규칙: S=1 or 2 → 1900년대, S=3 or 4 → 2000년대
     * (마스킹 '*' 있어도 숫자만 추려서 동작)
     */
    public static String birthFromRRN(String rrn) {
        if (rrn == null) return null;
        String d = rrn.replaceAll("\\D", "");
        if (d.length() < 7) return null;

        String yy = d.substring(0, 2);
        String mm = d.substring(2, 4);
        String dd = d.substring(4, 6);
        char s = d.charAt(6);

        Integer century = switch (s) {
            case '1', '2' -> 1900;
            case '3', '4' -> 2000;
            default -> null; 
        };
        if (century == null) return null;

        return (century + Integer.parseInt(yy)) + "-" + mm + "-" + dd;
    }

    private static String nfkc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFKC);
    }
}