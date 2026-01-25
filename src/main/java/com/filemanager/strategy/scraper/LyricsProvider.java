/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-25
 */
package com.filemanager.strategy.scraper;

/**
 * 歌词提供者接口
 */
public interface LyricsProvider {
    /**
     * 搜索歌词
     * @param artist 艺术家
     * @param title 歌曲标题
     * @param duration 歌曲时长（秒）
     * @return 歌词内容
     */
    String search(String artist, String title, int duration);
}
