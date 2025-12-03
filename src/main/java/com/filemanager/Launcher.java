package com.filemanager;

import com.filemanager.front.MusicFileManagerAppV14_Stable;
import com.filemanager.front.MusicFileManagerAppV16;

public class Launcher {
    public static void main(String[] args) {
        // 代理调用真正的主程序
        MusicFileManagerAppV16.main(args);
    }
}