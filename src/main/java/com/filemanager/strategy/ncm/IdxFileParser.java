/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-25 
 */
package com.filemanager.strategy.ncm;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Idx文件解析器
 * 负责解析网易云音乐的.idx文件，判断缓存是否完整
 */
public class IdxFileParser {
    
    /**
     * 检查缓存文件是否完整
     * @param ucFile .uc缓存文件
     * @return 是否完整
     */
    public boolean isCacheFileComplete(File ucFile) {
        if (ucFile.getName().toLowerCase().endsWith(".uc")) {
            String idxFileName = ucFile.getName().substring(0, ucFile.getName().lastIndexOf('.')) + ".idx";
            File idxFile = new File(ucFile.getParent(), idxFileName);
            if (!idxFile.exists()) {
                return false;
            }
            // 解析 idx 文件，判断是否加载全部数据
            return isCompleteIdxFile(idxFile);
        }
        return false;
    }
    
    /**
     * 检查.idx文件是否完整
     * @param idxFile .idx文件
     * @return 是否完整
     */
    public boolean isCompleteIdxFile(File idxFile) {
        try {
            // 读取整个 idx 文件内容
            FileInputStream fis = new FileInputStream(idxFile);
            byte[] buffer = new byte[(int) idxFile.length()];
            int bytesRead = fis.read(buffer);
            fis.close();
            
            if (bytesRead > 0) {
                // 将字节数组转换为字符串
                String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                
                // 提取 size
                int size = 0;
                int sizeStart = content.indexOf("size");
                if (sizeStart != -1) {
                    int colonStart = content.indexOf(":", sizeStart);
                    if (colonStart != -1) {
                        int quoteStart = content.indexOf("\"", colonStart);
                        if (quoteStart != -1) {
                            int quoteEnd = content.indexOf("\"", quoteStart + 1);
                            if (quoteEnd != -1) {
                                String sizeStr = content.substring(quoteStart + 1, quoteEnd).trim();
                                size = Integer.parseInt(sizeStr);
                            } else {
                                return false;
                            }
                        } else {
                            // 尝试处理数字格式的 size
                            int sizeEnd = content.indexOf(",", colonStart);
                            if (sizeEnd == -1) {
                                sizeEnd = content.indexOf("}", colonStart);
                            }
                            if (sizeEnd != -1) {
                                String sizeStr = content.substring(colonStart + 1, sizeEnd).trim();
                                size = Integer.parseInt(sizeStr);
                            } else {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
                
                // 提取 zone
                List<String> zone = new ArrayList<>();
                int zoneStart = content.indexOf("zone");
                if (zoneStart != -1) {
                    int bracketStart = content.indexOf("[", zoneStart);
                    if (bracketStart != -1) {
                        int bracketEnd = content.indexOf("]", bracketStart);
                        if (bracketEnd != -1) {
                            String zoneStr = content.substring(bracketStart + 1, bracketEnd).trim();
                            // 提取 zone 数组中的元素
                            if (zoneStr.startsWith("\"")) {
                                // 处理带引号的情况
                                int quoteStart = 0;
                                while (quoteStart < zoneStr.length()) {
                                    int quoteEnd = zoneStr.indexOf("\"", quoteStart + 1);
                                    if (quoteEnd != -1) {
                                        String zoneElement = zoneStr.substring(quoteStart + 1, quoteEnd);
                                        zone.add(zoneElement);
                                        quoteStart = zoneStr.indexOf("\"", quoteEnd + 1);
                                        if (quoteStart == -1) {
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            } else {
                                // 处理不带引号的情况
                                String[] elements = zoneStr.split(",");
                                for (String element : elements) {
                                    String trimmedElement = element.trim();
                                    if (!trimmedElement.isEmpty()) {
                                        zone.add(trimmedElement);
                                    }
                                }
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
                
                // 检查 zone 数组长度是否为 1
                if (zone.size() != 1) {
                    return false;
                }
                
                // 检查 zone 数组第一个元素的格式
                String zoneElement = zone.get(0);
                String[] zoneParts = zoneElement.split(" ");
                if (zoneParts.length != 2) {
                    return false;
                }
                
                // 检查 size 是否与 zone 中的值匹配
                int zoneStartVal = Integer.parseInt(zoneParts[0]);
                int zoneEndVal = Integer.parseInt(zoneParts[1]);
                if (size != (zoneEndVal - zoneStartVal + 1)) {
                    return false;
                }
                
                return true;
            }
        } catch (Exception e) {
            // 记录错误信息
            System.err.println("解析 idx 文件失败: " + e.getMessage());
        }
        return false;
    }
}
