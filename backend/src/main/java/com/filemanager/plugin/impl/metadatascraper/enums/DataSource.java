package com.filemanager.plugin.impl.metadatascraper.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum DataSource implements PluginEnum {
    
    LOCAL_INFERENCE("local_inference", "本地推断 (仅生成清单)", "Local Inference", "本地推断，仅生成清单", "Local inference, generate manifest only"),
    NETEASE_MUSIC("netease_music", "网易云音乐 (中文歌曲) (不完善)", "NetEase Music", "网易云音乐，中文歌曲", "NetEase Music, Chinese songs"),
    MIGU_MUSIC("migu_music", "咪咕音乐 (版权歌曲) (不完善)", "Migu Music", "咪咕音乐，版权歌曲", "Migu Music, copyrighted songs"),
    MUSICBRAINZ("musicbrainz", "MusicBrainz (开源数据库)", "MusicBrainz", "MusicBrainz开源数据库", "MusicBrainz open source database"),
    ITUNES("itunes", "iTunes (苹果音乐)", "iTunes", "iTunes苹果音乐", "iTunes Apple Music"),
    LAST_FM("last_fm", "Last.fm (全球音乐平台) (不完善)", "Last.fm", "Last.fm全球音乐平台", "Last.fm global music platform"),
    DISCOGS("discogs", "Discogs (音乐数据库) (不完善)", "Discogs", "Discogs音乐数据库", "Discogs music database");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    DataSource(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getNameZh() {
        return nameZh;
    }
    
    @Override
    public String getNameEn() {
        return nameEn;
    }
    
    @Override
    public String getDescriptionZh() {
        return descriptionZh;
    }
    
    @Override
    public String getDescriptionEn() {
        return descriptionEn;
    }
    
    public boolean isLocalInference() {
        return this == LOCAL_INFERENCE;
    }
    
    public boolean isOnlineSource() {
        return this != LOCAL_INFERENCE;
    }
    
    public boolean isChineseSource() {
        return this == NETEASE_MUSIC || this == MIGU_MUSIC;
    }
    
    public boolean isGlobalSource() {
        return this == MUSICBRAINZ || this == ITUNES || this == LAST_FM || this == DISCOGS;
    }
    
    public static DataSource fromCode(String code) {
        return PluginEnum.fromCode(code, DataSource.class, LOCAL_INFERENCE);
    }
}
