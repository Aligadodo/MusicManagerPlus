/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.model.dump;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.filemanager.util.StringUtils;

import java.util.Arrays;

public class MetaData {

    private final JSONObject metaDataJson;

    public MetaData(byte[] metaDataBytes) {
        String metaDataStr = StringUtils.toString(metaDataBytes);
        this.metaDataJson = JSON.parseObject(metaDataStr);
    }

    public JSONObject getJson() {
        return metaDataJson;
    }

    public String[] getArtistsName() {
        JSONArray artists = metaDataJson.getJSONArray("artist");
        String[] artistsName = new String[artists.size()];
        for (int i = 0; i < artists.size(); ++i) {
            artistsName[i] = artists.getJSONArray(i).getString(0);
        }
        return artistsName;
    }

    @Override
    public String toString() {

        return "=> Music Name: " + metaDataJson.getString("musicName") + "\n" +
                "=> Artists: " + Arrays.toString(getArtistsName()) + "\n" +
                "=> Album: " + metaDataJson.getString("album") + "\n" +
                "=> Bitrate: " + metaDataJson.getInteger("bitrate") + "\n" +
                "=> Format: " + metaDataJson.getString("format");
    }
}
