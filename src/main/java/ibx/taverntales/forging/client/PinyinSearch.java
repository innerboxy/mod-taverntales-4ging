package ibx.taverntales.forging.client;

import ibx.taverntales.forging.TavernTalesEquipmentForge;

import java.util.Locale;

/**
 * 拼音搜索工具:为文本生成 原文/全拼/首字母 三种检索键。
 * pinyin4j 不可用时自动降级为仅原文匹配,不会导致崩溃。
 */
public final class PinyinSearch {
    private static final Converter CONVERTER = createConverter();

    private PinyinSearch() {}

    /** 生成检索键:[原文小写, 全拼, 拼音首字母],非中文字符原样保留 */
    public static String[] searchKeys(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (CONVERTER == null) {
            return new String[]{lower};
        }
        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        for (char c : text.toCharArray()) {
            String pinyin = CONVERTER.firstPinyin(c);
            if (pinyin != null && !pinyin.isEmpty()) {
                full.append(pinyin);
                initials.append(pinyin.charAt(0));
            } else {
                char lowerChar = Character.toLowerCase(c);
                full.append(lowerChar);
                initials.append(lowerChar);
            }
        }
        return new String[]{lower, full.toString(), initials.toString()};
    }

    public static boolean matches(String[] keys, String query) {
        for (String key : keys) {
            if (key.contains(query)) return true;
        }
        return false;
    }

    private static Converter createConverter() {
        try {
            Converter converter = new Pinyin4jConverter();
            converter.firstPinyin('中'); // 触发类加载验证可用性
            return converter;
        } catch (Throwable t) {
            TavernTalesEquipmentForge.LOGGER.warn("pinyin4j 不可用,锻造台搜索降级为仅原文匹配", t);
            return null;
        }
    }

    private interface Converter {
        /** 该字符的拼音(多音字取第一个读音),非中文返回 null */
        String firstPinyin(char c);
    }

    /** 隔离 pinyin4j 引用,库缺失时仅此类加载失败 */
    private static final class Pinyin4jConverter implements Converter {
        private static final net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat FORMAT =
                new net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat();

        static {
            FORMAT.setToneType(net.sourceforge.pinyin4j.format.HanyuPinyinToneType.WITHOUT_TONE);
            FORMAT.setCaseType(net.sourceforge.pinyin4j.format.HanyuPinyinCaseType.LOWERCASE);
            FORMAT.setVCharType(net.sourceforge.pinyin4j.format.HanyuPinyinVCharType.WITH_V);
        }

        @Override
        public String firstPinyin(char c) {
            try {
                String[] readings = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                return readings != null && readings.length > 0 ? readings[0] : null;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
