package com.filemanager;

import com.filemanager.plusv2.MusicFileManagerApp;

public class Launcher {
    public static void main(String[] args) {
        // 代理调用真正的主程序
        MusicFileManagerApp.main(args);
    }
}