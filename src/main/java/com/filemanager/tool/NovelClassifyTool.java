package com.filemanager.tool;

import com.filemanager.rule.FileClassifyRule;
import com.filemanager.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NovelClassifyTool {

    private static final String path_of_novels = "D:\\小说\\阅读存档\\网络小说";
    private static final String path_of_sex_novels = "D:\\小说\\阅读存档\\SQ小说";
    private static final String path_of_japan_novels = "D:\\小说\\阅读存档\\JP小说";
    private static final String path_of_others = "D:\\小说\\阅读存档\\其他文档";
    private static final String path_of_delete = "D:\\小说\\阅读存档\\删除";

    private static final List<FileClassifyRule> rulesForNovel = new ArrayList<>();

    static {
        rulesForNovel.add(new FileClassifyRule("", "", 0, 10, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("（1）,（2）,(1),(2),(2),(1)", "", 200, 10000, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("ぁ,あ,ぃ,い,ぅ,う,ぇ,え,ぉ,お,か,が,き,ぎ,く,ぐ,け,げ,こ,ご,と,で,て,づ," +
                "つ,っ,ぢ,だ,だ,た,そ,せ,す,さ,さ,ど,ど,な,の,は,ば,ぱ,ひ,び,ぷ,ほ,ぼ,べ,へ,る,ま,ゑ,ゕ,ェ,ザ,ザ,ジ,ジ" +
                ",ス,ズ,ズ,ネ,モ,メ,ミ,ミ,ナ,ド,ポ,ヶ,ヿ,ヨ,ュ,ャ,セ,セ,タ,ヂ,ド,ヒ,ソ,ソ,ォ,ォ,ダ,ゴ,ゟ,ゟ,イ,ィ,シ,マ" +
                ",バ,セ,セ,な,び,か,る,か,ん,む,ち,ゃ,あ,ね,お,姉,ち,ゃ,ん,と,ボ,クの,ら,ぶ,ら,ぶ", "", 0, path_of_japan_novels));
        rulesForNovel.add(new FileClassifyRule("春色,丝袜,白丝,色欲,人妻,情欲,淫,桃花,香艳,风流,公媳,鹿鼎记,乱欲,奴隶,性奴,女奴," +
                "女警,后宫,艳史,黄蓉,巨乳,色戒,百美,后宫,堕落,肛,春情,绿帽,艳遇,百美,娇妻,妇科,胸部,姹女,风流,凌辱,欲望,姐夫," +
                "小姨子,成熟,韵味,风韵,淫贱,调教,調教,催眠,水乳,福利,淫靡,爆乳,肥臀,妖淫,娇妻,誘惑,美腿,强奸,肉欲,獸人,快樂,播種,後宮,操破,女侠,武神,黑星," +
                "069,翻云覆雨,情趣,肉棒,轮奸,玉洁,御姐,鬼畜,白虎,写真,性爱,高潮,爱抚,少妇,人妻,人妇,美娇,貞操,身體,R-18," +
                "肏,花季,采花,百花,嫂子,射精,女人,色鬼,好色,激情,美人.花间,魅魔,肉文,美女,母狗,操你,沉沦,群芳,污版,痴汉,调教," +
                "萝莉,野兽,风情,逆推,2003一千零一夜,榨汁,小穴,幼妻,白浆,性欲,纯爱,做爱,屁眼,屁股,性交,臀交,足交,尻,肉便器,乱伦,云雨," +
                "強姦,猥亵,潮吹,色魔,翘臀,怀孕,公交,幼乳,第一次,幼女,春梦,百合,NP,HP,寝取,母女,口交,中出,失禁,情色,学园系列,裤袜,干炮," +
                "臭脚,援交,桃色,白浊,漂亮,美女,欲火,妻子,姦,强迫,强暴,姦,壞,紧缚,丝足,表妹,表姐,种马,借种,乱交,侵犯,姐妹花,蹂躏,浪情,骚,名器," +
                "色色,LOLI,LUOLI,姿势,AV,乖巧,乱伦,太太,污版,诱惑,69,６９,未成年,援交,长腿,女仆,玷污,禁断,高潮,性福,抖M,sm,肉便器,寝取,肥肉," +
                "黄蓉,花劫,性福,双人行,榨精,母狗,獸慾,魅魔,嫖,诱骗,沦陷,操,破处,娇小,娇嫩,尤物,捆绑,儿媳,尼姑,失身,失足,销魂,双修,秘史,耻辱,贞洁,贞节," +
                "屈辱,红杏,罩杯,胁迫,保健,癖,脱衣,人兽,约炮",
                "txt,chm,pdf,epub", 0, path_of_sex_novels));
        rulesForNovel.add(new FileClassifyRule("中国,美国,军事,毛泽东,武器,工程,产业,发展,工业,跃进,评价,记录,经济,体制," +
                "革命,历史,文化,教育,政府,政策,战争,战役,母亲,妈,母子",
                "", 0, 300, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("漂亮,可爱,欲,粉,桃,迷,艳,缘,爱,色,春,性,女,黄,花,幼,熟,奴,贱,裸," +
                "交,淫,荡,妇,足,腿,脚,胸,奶,臀,乳,玉,肉,孕,姐姐,妹妹,姐妹,巫山,老师,姨,精,野,劫难,落难,欢,幸福,外传,里番," +
                "云雨,龙凤,羞耻,兄妹,姐弟,客车,地铁,番外,小姨,极品,妻,校园,生理,刺激,龙,凤,美,jk,我和,我与,我的,h,x,禁,学院,学员," +
                "学妹,学姐,师姐,萝,学园,圣姑,前犯,人渣,妹",
                "txt,chm,pdf,epub", 0, 9999, path_of_sex_novels));
        //rulesForNovel.add(new FileClassifyRule("", "", 0, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("^[0-9a-zA-Z]{15,45}", "", "", 0, 9999, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("^[0-9]{2,4}.{8,15}", "", "", 0, 9999, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("故事会", "", 0, path_of_others));
        rulesForNovel.add(new FileClassifyRule("都市,异能,灵气,灵异,文明,穿越,唐,宋,元,明,清,超级,科技,次元,电影,修仙,休闲,修行,无限,帝国", "txt,chm", 100, path_of_novels));

        rulesForNovel.add(new FileClassifyRule("^\\#[0-9]{2,4}.*$", "", "", 0, 99999, path_of_sex_novels));
        rulesForNovel.add(new FileClassifyRule("^\\[.{3,8}\\].*$", "", "", 0, 99999, path_of_sex_novels));
        rulesForNovel.add(new FileClassifyRule("^\\【.{3,8}\\】.*$", "", "", 0, 99999, path_of_sex_novels));
        rulesForNovel.add(new FileClassifyRule("第,章节", "txt,chm", 0, 100, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("", "pdf,epub", 0, path_of_others));
        rulesForNovel.add(new FileClassifyRule("", "jpg,jpeg,png,mhd,url", 0, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("exe", "rar,zip", 0, path_of_delete));
        rulesForNovel.add(new FileClassifyRule("", "txt,chm", 0, path_of_novels));
        rulesForNovel.add(new FileClassifyRule("", "txt", 0, 150, path_of_sex_novels));
        rulesForNovel.add(new FileClassifyRule("", "", 0, path_of_others));

    }

    public static void main(String[] args) {
        System.out.println("begin !");
        moveFiles("D:\\小说\\阅读存档\\网络小说-精简版本\\txt", rulesForNovel);
        System.out.println("done !");
    }

    public static void moveFiles(String rootDir, List<FileClassifyRule> rules) {
        List<File> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        FileUtil.listFiles(0, new File(rootDir), files, dirs);
        System.out.println(String.format("dir has %d files and %d dirs ", files.size(), dirs.size()));
        files.forEach(
                file -> {
                    FileClassifyRule rule = rules.stream().filter(fileTransRule -> fileTransRule.isApply(file)).findFirst().orElse(null);
                    if (rule != null) {
                        FileUtil.transferTo(file, rule.getDestPath(file));
                    }
                }
        );
        dirs.stream().sorted(Collections.reverseOrder()).forEach(
                dir -> {
                    File[] filesCheck = dir.listFiles();
                    if (filesCheck.length == 0) {
                        try {
                            FileUtil.delete(dir);
                            System.out.println(String.format("dir %s deleted! ", dir.getPath()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }


}
