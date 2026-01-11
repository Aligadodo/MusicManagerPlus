/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.model;

import lombok.Data;

import java.io.File;

@Data
public class MusicInfo{
    public File file;
    public String artist;// 歌手名
    public String album;// 專輯名
    public String songName;// 歌名
}