package plusv2.util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import plusv2.model.AudioMeta;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetadataHelper {
    static {
        // Disable JAudioTagger extensive logging
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }


    /**
     * 智能提取元数据：
     * 1. 尝试读取文件 Tag
     * 2. 检查 Tag 是否乱码或为空
     * 3. 如果乱码，回退到文件名解析
     * 4. 如果 forceFile 为 true，直接使用文件名解析
     */
    public static AudioMeta getSmartMetadata(File file, boolean forceFile) {
        AudioMeta meta = new AudioMeta();
        boolean tagValid = false;

        if (!forceFile) {
            try {
                AudioFile f = AudioFileIO.read(file);
                Tag tag = f.getTag();
                if (tag != null) {
                    meta.title = tag.getFirst(FieldKey.TITLE);
                    meta.artist = tag.getFirst(FieldKey.ARTIST);
                    meta.album = tag.getFirst(FieldKey.ALBUM);
                    meta.year = tag.getFirst(FieldKey.YEAR);
                    meta.track = tag.getFirst(FieldKey.TRACK);

                    // 简单的有效性检查
                    if (isValid(meta.title) || isValid(meta.artist)) {
                        tagValid = true;
                    }

                    // 乱码检测 heuristic
                    if (isMessy(meta.title) || isMessy(meta.artist) || isMessy(meta.album)) {
                        // System.out.println("检测到疑似乱码: " + file.getName());
                        tagValid = false;
                    }
                }
            } catch (Exception e) {
                // 读取失败，忽略，将在下面回退
            }
        }

        // 如果 Tag 无效或强制使用文件名，则进行文件名解析
        if (!tagValid) {
            AudioMeta guessed = extractFromFileSystem(file);
            // 补全策略：如果 Tag 是空的，完全用文件名；如果 Tag 是乱码，完全覆盖
            meta = guessed;
            meta.isGuessed = true;
        } else {
            // 如果 Tag 有效，但部分字段缺失，可以用文件名补全吗？
            // 这里的策略是：如果 Tag 看起来是好的，就信 Tag。
            // 仅仅在 track 为空时尝试补全一下
            if (!isValid(meta.track)) {
                AudioMeta guessed = extractFromFileSystem(file);
                if (isValid(guessed.track)) meta.track = guessed.track;
            }
        }

        // 最后的清理
        if (meta.title == null || meta.title.isEmpty()) meta.title = removeExt(file.getName());

        return meta;
    }

    private static boolean isValid(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // 简单的乱码检测逻辑
    private static boolean isMessy(String s) {
        if (s == null) return false;
        // 1. 包含 Unicode 替换字符
        if (s.contains("\uFFFD")) return true;
        // 2. 包含大量问号 (通常是编码转换失败)
        return s.contains("????");
        // 3. 包含生僻乱码字符组合 (示例，可扩展)
        // 这里的判断比较主观，最稳妥的是上面的替换字符
    }

    // 原有的基于文件名的解析逻辑 (重命名为 extractFromFileSystem)
    public static AudioMeta extractFromFileSystem(File file) {
        AudioMeta meta = new AudioMeta();
        String name = file.getName();

        // Pattern 1: "01. Track Title" or "01 - Track Title"
        if (name.matches("^\\d+[.\\s-].*")) {
            String[] parts = name.split("[.\\s-]", 2);
            if (parts.length > 1) {
                meta.track = parts[0].trim();
                meta.title = removeExt(parts[1].trim());
            }
        }
        // Pattern 2: "Artist - Title"
        else if (name.contains(" - ")) {
            String[] parts = name.split(" - ");
            if (parts.length >= 2) {
                meta.artist = parts[0].trim();
                meta.title = removeExt(parts[1].trim());
            }
            if (parts.length >= 3) meta.album = parts[1].trim();
        } else {
            meta.title = removeExt(name);
        }

        File parent = file.getParentFile();
        if (parent != null) {
            String parentName = parent.getName();
            if (parentName.matches("^\\d{4}\\s+-\\s+.*")) {
                meta.year = parentName.substring(0, 4);
                meta.album = parentName.substring(7).trim();
            } else if (parentName.contains(" - ")) {
                String[] parts = parentName.split(" - ", 2);
                if (!isValid(meta.artist)) meta.artist = parts[0].trim();
                meta.album = parts[1].trim();
            } else {
                meta.album = parentName;
            }

            File grandParent = parent.getParentFile();
            if (grandParent != null && !isValid(meta.artist)) {
                meta.artist = grandParent.getName();
            }
        }
        return meta;
    }

    private static String removeExt(String s) {
        int d = s.lastIndexOf('.');
        return d > 0 ? s.substring(0, d) : s;
    }

    public static String format(String template, AudioMeta meta) {
        return template.replace("%artist%", meta.artist == null ? "" : meta.artist)
                .replace("%album%", meta.album == null ? "" : meta.album)
                .replace("%title%", meta.title == null ? "" : meta.title)
                .replace("%year%", meta.year == null ? "" : meta.year)
                .replace("%track%", meta.track == null ? "" : meta.track);
    }
}