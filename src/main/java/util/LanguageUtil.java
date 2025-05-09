package util;

import com.github.houbb.opencc4j.util.ZhConverterUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LanguageUtil {
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final String PPP = "!@#$%^&*()_+=<>?/:;.,{}！＠＃＄％＾＆＊（）＿＋＝＜＞？／：；．，｛｝";
    private static final Set<Character.UnicodeBlock> CHINESE_UNICODE_BLOCKS = new HashSet<Character.UnicodeBlock>() {{
        add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
        add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
        add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
        add(Character.UnicodeBlock.GENERAL_PUNCTUATION);
        add(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
        add(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    }};
    private static final Set<Character.UnicodeBlock> JAPANESE_UNICODE_BLOCKS = new HashSet<Character.UnicodeBlock>() {{
        add(Character.UnicodeBlock.HIRAGANA);
        add(Character.UnicodeBlock.KATAKANA);
        add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
    }};


    /**
     * 判断单个字符是否为中文
     */
    public static boolean isChineseChar(char c) {
        return CHINESE_UNICODE_BLOCKS.contains(Character.UnicodeBlock.of(c));
    }

    /**
     * 判断单个字符是否为英文
     */
    public static boolean isEnglishChar(char c) {
        if (c <= 'z' && c >= 'a') {
            return true;
        }
        return c <= 'Z' && c >= 'A';
    }

    /**
     * 判断文本是否包含日文
     */
    public static boolean isEnglishStr(String str, int minChar) {
        long count = str.chars().mapToObj(c -> (char) c).collect(Collectors.toList()).stream().filter(LanguageUtil::isJapaneseChar).count();
        if (minChar <= 0) {
            return count == str.length();
        }
        return count > minChar;
    }


    /**
     * 判断单个字符是否为英文
     */
    public static boolean isNumChar(char c) {
        return c <= '9' && c >= '0';
    }

    /**
     * 判断单个字符是否为日文
     */
    public static boolean isJapaneseChar(char c) {
        return JAPANESE_UNICODE_BLOCKS.contains(Character.UnicodeBlock.of(c));
    }

    /**
     * 判断文本是否包含日文
     */
    public static boolean isJapaneseStr(String str, int minChar) {
        long count = str.chars().mapToObj(c -> (char) c).collect(Collectors.toList()).stream().filter(LanguageUtil::isJapaneseChar).count();
        if (minChar <= 0) {
            return count == str.length();
        }
        return count > minChar;
    }


    /**
     * 判断单个字符是否为韩文
     */
    public static boolean isKoreaChar(char c) {
        if (!((c > 0x3130 && c < 0x318F)
                || (c >= 0xAC00 && c <= 0xD7A3))) {
            return false;
        }
        return true;
    }

    /**
     * 判断文本是否包含日文
     */
    public static boolean isKoreaStr(String str, int minChar) {
        long count = str.chars().mapToObj(c -> (char) c).collect(Collectors.toList()).stream().filter(LanguageUtil::isKoreaChar).count();
        if (minChar <= 0) {
            return count == str.length();
        }
        return count > minChar;
    }


    /**
     * 是否全是中文
     **/
    public static boolean isALlChinese(String name) {
        name = toSimpleChinese(name);
        String regex = "^[\u4e00-\u9fa5]+$";
        return name.matches(regex);
    }

    /**
     * 是否全是中文或特定符号
     **/
    public static boolean isAllChineseOrSymbol(String str, String symbols) {
        // 检查每个字符是否符合中文字符或特定符号
        for (int i = 0; i < str.length(); i++) {
            // 如果不是中文字符，检查是否是特定符号
            if (!CHINESE_PATTERN.matcher(String.valueOf(str.charAt(i))).matches()
                    && symbols.indexOf(str.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }


    /**
     * 繁体全部转为简体
     **/
    public static String toSimpleChinese(String name) {
        return ZhConverterUtil.toSimple(name);
    }

    /**
     * 全角字符全部转为半角字符
     **/
    public static String toHalfWidth(String fullWidthStr) {
        // 全角空格转半角空格
        fullWidthStr = fullWidthStr.replace('　', ' ');

        // 全角字符转半角字符
        char[] charArray = fullWidthStr.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            if (charArray[i] >= 65281 && charArray[i] <= 65374) {
                charArray[i] = (char) (charArray[i] - 65248);
            }
        }
        return new String(charArray);
    }

    /**
     * 半角字符转为全角字符
     **/
    public static String toFullWidth(String input) {
        char[] chars = input.toCharArray();
        char[] output = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') {
                output[i] = chars[i];
                continue;
            }
            if (chars[i] < 0x3000) {
                output[i] = (char) (chars[i] + 0xfee0);
            } else {
                output[i] = chars[i];
            }
        }
        return new String(output);
    }

    /**
     * 名称标准化，含去空格、转半角、转简体、转大写等
     **/
    public static String toClassicName(String name) {
        return toSimpleChinese(toHalfWidth(name)).replace(" ", "").toUpperCase();
    }


    public static void main(String[] args) {
        System.out.println(toFullWidth(PPP));
    }
}
