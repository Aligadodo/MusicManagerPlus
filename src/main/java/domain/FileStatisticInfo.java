package domain;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import util.FileUtil;
import util.LanguageUtil;

import java.io.File;

@Data
@Getter
@Setter
public class FileStatisticInfo {
    public File file;
    /***
     * basic info
     */
    public String type;
    public String oriName;
    public String classicName;
    public int fileNameLength;
    public double fileSizeMb;
    /***
     * name related info
     */
    public int countCNChars;
    public int countENChars;
    public int countJPChars;
    public int countKoreanChars;
    public int countNUMChars;

    public static FileStatisticInfo create(File file) {
        FileStatisticInfo statisticInfo = new FileStatisticInfo();
        statisticInfo.file = file;
        statisticInfo.type = FileUtil.getFileType(file);
        statisticInfo.fileSizeMb = FileUtil.getFileSizeMB(file);
        String filename = file.getName();
        if (filename.indexOf('.') > 0) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        statisticInfo.oriName = filename;
        statisticInfo.classicName = LanguageUtil.toClassicName(filename);
        statisticInfo.fileNameLength = filename.length();
        for (char c : filename.toCharArray()) {
            if (LanguageUtil.isChineseChar(c)) {
                statisticInfo.countCNChars = statisticInfo.countCNChars + 1;
            } else if (LanguageUtil.isEnglishChar(c)) {
                statisticInfo.countENChars = statisticInfo.countENChars + 1;
            } else if (LanguageUtil.isJapaneseChar(c)) {
                statisticInfo.countJPChars = statisticInfo.countJPChars + 1;
            } else if (LanguageUtil.isKoreaChar(c)) {
                statisticInfo.countKoreanChars = statisticInfo.countKoreanChars + 1;
            } else if (LanguageUtil.isNumChar(c)) {
                statisticInfo.countNUMChars = statisticInfo.countNUMChars + 1;
            }
        }
        return statisticInfo;
    }

    public void print(){
        System.out.println(this.classicName + ":" + JSONObject.toJSONString(this));
    }
}
