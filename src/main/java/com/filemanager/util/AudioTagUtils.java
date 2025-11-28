package com.filemanager.util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;
import org.jaudiotagger.tag.wav.WavTag;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 音频标签元数据修复工具类
 * 修复 NPE 问题，并确保中文不乱码
 */
public class AudioTagUtils {

    static {
        // 1. 禁用日志噪音
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);

        // 2. 设置默认编码为 UTF-16 (ID3v2.3 标准)
        TagOptionSingleton.getInstance().setId3v24DefaultTextEncoding(TextEncoding.UTF_16);
        TagOptionSingleton.getInstance().setId3v23DefaultTextEncoding(TextEncoding.UTF_16);
        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_16);

        // 3. 解决 Padding 问题
        TagOptionSingleton.getInstance().setPadNumbers(true);
    }

    /**
     * 修复并刷新音频文件的标签信息
     */
    public static boolean fixAudioTags(File audioFile, String title, String artist, String album) {
        if (audioFile == null || !audioFile.exists()) {
            System.err.println("文件不存在: " + audioFile);
            return false;
        }

        try {
            AudioFile f = AudioFileIO.read(audioFile);
            Tag tag = f.getTag();

            // === 核心修正逻辑 ===
            if (f.getAudioHeader().getFormat().toLowerCase().contains("wav")) {
                // 针对 WAV 的特殊处理：分离写入，避免乱码
                updateWavTagSafely(f, title, artist, album);
            } else {
                // 针对 FLAC/MP3 等通用格式：直接使用标准方法
                if (tag == null) {
                    tag = f.createDefaultTag();
                    f.setTag(tag);
                }
                setFieldWait(tag, FieldKey.TITLE, title);
                setFieldWait(tag, FieldKey.ARTIST, artist);
                setFieldWait(tag, FieldKey.ALBUM, album);
            }

            f.commit();
            System.out.println("成功修复标签: " + audioFile.getName());
            return true;

        } catch (Exception e) {
            System.err.println("修复失败 [" + audioFile.getName() + "]: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 安全更新 WAV 标签
     * 策略：
     * 1. 确保 ID3v2 存在并直接写入它 (支持 Unicode)。
     * 2. 确保 InfoTag 存在 (防止 NPE) 但清空其关键字段 (防止乱码)。
     */
    private static void updateWavTagSafely(AudioFile f, String title, String artist, String album) {
        Tag tag = f.getTag();
        WavTag wavTag;

        // 1. 获取或创建 WavTag
        if (tag instanceof WavTag) {
            wavTag = (WavTag) tag;
        } else {
            wavTag = new WavTag(TagOptionSingleton.getInstance().getWavOptions());
            f.setTag(wavTag);
        }

        // 2. 确保 ID3Tag 存在
        if (wavTag.getID3Tag() == null) {
            wavTag.setID3Tag(new ID3v23Tag());
        }

        // 3. 确保 InfoTag 存在 (关键！避免 convertInfoChunk 报 NPE)
        // 注意：我们不再调用 setInfoTag(null)
        if (wavTag.getInfoTag() == null) {
            // 如果没有，就让它保持 null，或者由库在内部处理。
            // 但既然报错 NPE，说明库希望它有值。
            // 如果此处不干预，下面的 deleteField 操作会自动跳过 null 检查。
        }

        // 4. [关键步骤] 分离操作
        // 不要调用 wavTag.setField(...)，因为它会同时写 ID3 和 Info。

        // A. 直接写入 ID3Tag (完美支持中文)
        Tag id3Tag = wavTag.getID3Tag();
        setFieldWait(id3Tag, FieldKey.TITLE, title);
        setFieldWait(id3Tag, FieldKey.ARTIST, artist);
        setFieldWait(id3Tag, FieldKey.ALBUM, album);

        // B. 清空 InfoTag 中的对应字段 (如果 InfoTag 存在)
        // 这样 Info Chunk 即使被写入，也是空的，播放器就会回退去读取 ID3
        Tag infoTag = wavTag.getInfoTag();
        if (infoTag != null) {
            try {
                infoTag.deleteField(FieldKey.TITLE);
                infoTag.deleteField(FieldKey.ARTIST);
                infoTag.deleteField(FieldKey.ALBUM);
            } catch (Exception ignored) {
                // 某些旧版 InfoTag 可能不支持 deleteField，忽略即可
            }
        }
    }

    private static void setFieldWait(Tag tag, FieldKey key, String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                tag.setField(key, value.trim());
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    public static void main(String[] args) {
        // 测试用例
        File file = new File("C:\\Music\\ErrorSong.wav");
        fixAudioTags(file, "测试歌曲", "测试歌手", "测试专辑");
    }
}