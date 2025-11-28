package com.filemanager.util;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Util {

    /**
     * 获取MP3歌曲名、歌手、时长、照片信息
     *
     * @param url
     * @return
     */
    public void getMP3Info(String url) throws ReadOnlyFileException, TagException, InvalidAudioFrameException, IOException, CannotReadException {
        url = "E:\\CloudMusic\\VipSongsDownload\\FKJ,Tom Bailey - Drops.mp3";//测试数据**

        MP3File mp3File = (MP3File) AudioFileIO.read(new File(url));
        AbstractID3v2Tag v2tag = mp3File.getID3v2Tag();

        String artist = v2tag.getFirst(FieldKey.ARTIST);// 歌手名
        String album = v2tag.getFirst(FieldKey.ALBUM);// 專輯名
        String songName = v2tag.getFirst(FieldKey.TITLE);// 歌名
        System.out.println("album: " + album); // 專輯名
        System.out.println("singer: " + artist); // 歌手名
        System.out.println("songName: " + songName); // 歌名

        MP3AudioHeader header = mp3File.getMP3AudioHeader(); // mp3文件頭部信息
        int length = header.getTrackLength();
        System.out.println("Length: " + length / 60 + ":" + length % 60 + "sec"); // 歌曲時長
        AbstractID3v2Tag tag = mp3File.getID3v2Tag();
        AbstractID3v2Frame frame = (AbstractID3v2Frame) tag.getFrame("APIC");

        FrameBodyAPIC body = (FrameBodyAPIC) frame.getBody();
        byte[] imageData = body.getImageData();
        //System.out.println(imageData);
        Image img = Toolkit.getDefaultToolkit().createImage(imageData, 0, imageData.length);
        System.out.println("img----" + imageData);
        ImageIcon icon = new ImageIcon(img);
//        FileOutputStream fos = new FileOutputStream("D://test1.jpg");
        FileOutputStream fos = new FileOutputStream("E:\\CloudMusic\\VipSongsDownload\\test1.jpg");
        fos.write(imageData);
        fos.close();

        //要在有图形环境下运行这个方法，只读取可以不用这个方法
//        getImg(icon);
    }

//    public static void getImg(ImageIcon img){
//        JFrame f = new JFrame();
//        JLabel l = new JLabel();
//        l.setIcon(img);
//        l.setVisible(true);
//        f.add(l);
//        f.setSize(500, 500);
//        f.setVisible(true);
//    }
}