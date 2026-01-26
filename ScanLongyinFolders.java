import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScanLongyinFolders {
    public static void main(String[] args) {
        String path = "V:\\Z - 中文厂牌\\L - 龙音港版唱片";
        File dir = new File(path);
        
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("目录不存在: " + path);
            return;
        }
        
        File[] folders = dir.listFiles(File::isDirectory);
        if (folders == null) {
            System.out.println("无法读取目录: " + path);
            return;
        }
        
        List<String> folderNames = new ArrayList<>();
        for (File folder : folders) {
            folderNames.add(folder.getName());
        }
        
        // 按名称排序
        folderNames.sort(String::compareTo);
        
        // 输出JSON格式
        System.out.println("[");
        for (int i = 0; i < folderNames.size(); i++) {
            System.out.println("    \"" + folderNames.get(i) + "\"" + (i < folderNames.size() - 1 ? "," : ""));
        }
        System.out.println("]");
        
        System.out.println("\n总共找到 " + folderNames.size() + " 个文件夹");
    }
}
