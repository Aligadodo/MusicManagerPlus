package plusv2.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AudioMeta {
    public String artist = "";
    public String album = "";
    public String title = "";
    public String year = "";
    public String track = "";
    public boolean isGuessed = false; // 标记是否是猜出来的（文件名推断）
}
