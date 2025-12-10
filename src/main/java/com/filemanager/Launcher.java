package com.filemanager;

import com.filemanager.app.versions.FileManagerAppV18_Stable;

public class Launcher {
    public static void main(String[] args) {
        // 代理调用真正的主程序
        FileManagerAppV18_Stable.main(args);
    }
}