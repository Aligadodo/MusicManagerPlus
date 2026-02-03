package com.filemanager.plugin.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlaylistGenerator {

    public static void generateM3UPlaylist(File playlistFile, List<String> trackPaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(playlistFile))) {
            writer.write("#EXTM3U");
            writer.newLine();
            
            for (String trackPath : trackPaths) {
                writer.write(trackPath);
                writer.newLine();
            }
        }
    }

    public static void generatePLSPlaylist(File playlistFile, List<String> trackPaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(playlistFile))) {
            writer.write("[playlist]");
            writer.newLine();
            writer.write("NumberOfEntries=" + trackPaths.size());
            writer.newLine();
            
            for (int i = 0; i < trackPaths.size(); i++) {
                writer.write("File" + (i + 1) + "=" + trackPaths.get(i));
                writer.newLine();
                writer.write("Title" + (i + 1) + "=Track " + (i + 1));
                writer.newLine();
                writer.write("Length" + (i + 1) + "=-1");
                writer.newLine();
            }
            
            writer.write("Version=2");
            writer.newLine();
        }
    }

    public static void generateWPLPlaylist(File playlistFile, List<String> trackPaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(playlistFile))) {
            writer.write("<?wpl version=\"1.0\"?>");
            writer.newLine();
            writer.write("<smil>");
            writer.newLine();
            writer.write("    <head>");
            writer.newLine();
            writer.write("        <meta name=\"Generator\" content=\"FileManager Plus\"/>");
            writer.newLine();
            writer.write("        <meta name=\"ItemCount\" content=\"" + trackPaths.size() + "\"/>");
            writer.newLine();
            writer.write("    </head>");
            writer.newLine();
            writer.write("    <body>");
            writer.newLine();
            writer.write("        <seq>");
            writer.newLine();
            
            for (int i = 0; i < trackPaths.size(); i++) {
                writer.write("            <media src=\"" + trackPaths.get(i) + "\"/>");
                writer.newLine();
            }
            
            writer.write("        </seq>");
            writer.newLine();
            writer.write("    </body>");
            writer.newLine();
            writer.write("</smil>");
            writer.newLine();
        }
    }

    public static void generatePlaylist(File playlistFile, List<String> trackPaths, String format) throws IOException {
        switch (format.toLowerCase()) {
            case "m3u":
                generateM3UPlaylist(playlistFile, trackPaths);
                break;
            case "pls":
                generatePLSPlaylist(playlistFile, trackPaths);
                break;
            case "wpl":
                generateWPLPlaylist(playlistFile, trackPaths);
                break;
            default:
                throw new IllegalArgumentException("Unsupported playlist format: " + format);
        }
    }

    public static List<String> getRelativePaths(List<String> absolutePaths, File baseDir) {
        List<String> relativePaths = new ArrayList<>();
        Path basePath = baseDir.toPath();
        
        for (String absolutePath : absolutePaths) {
            Path absolute = Paths.get(absolutePath);
            Path relative = basePath.relativize(absolute);
            relativePaths.add(relative.toString());
        }
        
        return relativePaths;
    }
}
