package util;

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
 * 专注于解决 WAV 和 FLAC 文件的中文乱码问题
 */
public class AudioTagUtilsV1 {

    static {
        // === 全局配置 (关键步骤) ===
        
        // 1. 禁用 jaudiotagger 的日志噪音
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);

        // 2. 设置默认文本编码为 UTF-16
        // UTF-16 在 ID3v2.3 中是标准支持的，Windows 对其兼容性最好。
        // 虽然 UTF-8 (ID3v2.4) 更现代，但 Windows 资源管理器有时读取会有问题。
        TagOptionSingleton.getInstance().setId3v24DefaultTextEncoding(TextEncoding.UTF_16);
        TagOptionSingleton.getInstance().setId3v23DefaultTextEncoding(TextEncoding.UTF_16);
        TagOptionSingleton.getInstance().setId3v24UnicodeTextEncoding(TextEncoding.UTF_16);

        // 3. 强制重置 Tag，解决一些 padding 问题
        TagOptionSingleton.getInstance().setPadNumbers(true);
    }

    /**
     * 更新音频文件的元数据（标签）
     * 支持 FLAC, WAV, MP3 等格式
     *
     * @param audioFile 目标文件
     * @param title     歌名
     * @param artist    艺术家/歌手
     * @param album     专辑名称
     * @return true 表示更新成功，false 表示失败
     */
    public static boolean fixAudioTags(File audioFile, String title, String artist, String album) {
        if (audioFile == null || !audioFile.exists()) {
            System.out.println("文件不存在: " + audioFile);
            return false;
        }

        try {
            // 读取音频文件
            AudioFile f = AudioFileIO.read(audioFile);
            
            // 获取或创建 Tag
            Tag tag = f.getTag();

            // === 针对 WAV 文件的特殊处理 ===
            // WAV 标准比较混乱，有的只用 INFO chunk，有的用 ID3。
            // 为了支持中文，我们需要确保它有一个 ID3v2 标签。
            if (f.getAudioHeader().getFormat().toLowerCase().contains("wav")) {
                handleWavSpecifics(f);
                tag = f.getTag(); // 重新获取处理后的 Tag
            }

            // 如果此时 Tag 依然为空（例如全新的文件），创建一个默认 Tag
            if (tag == null) {
                tag = f.createDefaultTag();
                f.setTag(tag);
            }

            // === 核心修复逻辑：覆盖写入 ===
            // 注意：jaudiotagger 会根据我们在 static 块中设置的 Encoding 自动处理字符集
            
            setFieldWait(tag, FieldKey.TITLE, title);
            setFieldWait(tag, FieldKey.ARTIST, artist);
            setFieldWait(tag, FieldKey.ALBUM, album);

            // === 保存更改 ===
            f.commit();
            System.out.println("成功修复标签: " + audioFile.getName());
            return true;

        } catch (Exception e) {
            System.out.println("修复失败 [" + audioFile.getName() + "]: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 处理 WAV 特有的逻辑
     * 确保 WAV 文件拥有 ID3v2 标签以支持 Unicode
     */
    private static void handleWavSpecifics(AudioFile f) {
        Tag tag = f.getTag();

        // 场景1：已有 Tag 且是 WavTag，检查是否包含 ID3 标签
        if (tag instanceof WavTag) {
            WavTag wavTag = (WavTag) tag;
            // 使用 getId3Tag() 判空，兼容性更好
            if (wavTag.getID3Tag() == null) {
                wavTag.setID3Tag(new ID3v23Tag());
            }
        }
        // 场景2：Tag 不存在，或者读出来的不是 WavTag（例如损坏或未知格式）
        else {
            // 修正：使用 f.createDefaultTag() 而不是 new WavTag()
            // new WavTag() 需要 WavOptions 参数，直接调用库的工厂方法更安全
            Tag defaultTag = f.createDefaultTag();

            if (defaultTag instanceof WavTag) {
                WavTag newWavTag = (WavTag) defaultTag;
                // 确保新创建的 Tag 包含 ID3v2 容器
                if (newWavTag.getID3Tag() == null) {
                    newWavTag.setID3Tag(new ID3v23Tag());
                }
                f.setTag(newWavTag);
            }
        }
    }
    /**
     * 安全设置字段，处理 null 值和空字符串
     */
    private static void setFieldWait(Tag tag, FieldKey key, String value) {
        try {
            if (value != null && !value.trim().isEmpty()) {
                tag.setField(key, value.trim());
            } else {
                // 如果传入空值，可以选择删除该字段，防止显示空白
                // tag.deleteField(key); 
            }
        } catch (Exception e) {
            // 某些特殊 Tag 类型可能不支持特定 FieldKey，捕获以防中断
            System.out.println("无法设置字段 " + key + ": " + e.getMessage());
        }
    }

    // === 测试 Main 方法 ===
    public static void main(String[] args) {
        // 示例用法
        File file = new File("C:\\Music\\TestSong.wav");
        
        // 模拟从文件名或其他地方获取到的正确信息
        String correctTitle = "七里香";
        String correctArtist = "周杰伦";
        String correctAlbum = "七里香";

        fixAudioTags(file, correctTitle, correctArtist, correctAlbum);
    }
}