package com.filemanager.strategy.scraper.source.impl;

import com.filemanager.strategy.scraper.model.*;

public class ITunesSourceTest {
    public static void main(String[] args) {
        ITunesSource source = new ITunesSource();
        
        System.out.println("Testing iTunes Source");
        System.out.println("Source Name: " + source.getSourceName());
        System.out.println("Source Description: " + source.getSourceDescription());
        System.out.println();
        
        System.out.println("Testing searchTrackInfo for Ed Sheeran - Shape of You");
        TrackInfo trackInfo = source.searchTrackInfo("Ed Sheeran", "Shape of You");
        if (trackInfo != null) {
            System.out.println("Track Title: " + trackInfo.getTitle());
            System.out.println("Track Artist: " + trackInfo.getArtist());
            System.out.println("Track Album: " + trackInfo.getAlbum());
            System.out.println("Track Duration: " + trackInfo.getDuration());
            System.out.println("Track Year: " + trackInfo.getYear());
            System.out.println("Track Genre: " + trackInfo.getGenre());
        } else {
            System.out.println("Track info not found");
        }
        System.out.println("Last Request URL: " + source.getLastRequestUrl());
        System.out.println("Last Request Error: " + source.getLastRequestError());
        System.out.println();
        
        System.out.println("Testing searchAlbumInfo for Ed Sheeran - Divide");
        AlbumInfo albumInfo = source.searchAlbumInfo("Ed Sheeran", "Divide");
        if (albumInfo != null) {
            System.out.println("Album Name: " + albumInfo.getName());
            System.out.println("Album Artist: " + albumInfo.getArtist());
            System.out.println("Album Year: " + albumInfo.getYear());
            System.out.println("Album Genre: " + albumInfo.getGenre());
            System.out.println("Album Tracks: " + albumInfo.getTracks().size());
        } else {
            System.out.println("Album info not found");
        }
        System.out.println("Last Request URL: " + source.getLastRequestUrl());
        System.out.println("Last Request Error: " + source.getLastRequestError());
        System.out.println();
        
        System.out.println("Testing searchCover for Ed Sheeran - Divide");
        CoverInfo coverInfo = source.searchCover("Ed Sheeran", "Divide");
        if (coverInfo != null) {
            System.out.println("Cover URL: " + coverInfo.getImageUrl());
            System.out.println("Cover Format: " + coverInfo.getFormat());
            System.out.println("Cover Size: " + coverInfo.getWidth() + "x" + coverInfo.getHeight());
        } else {
            System.out.println("Cover info not found");
        }
        System.out.println("Last Request URL: " + source.getLastRequestUrl());
        System.out.println("Last Request Error: " + source.getLastRequestError());
        System.out.println();
        
        System.out.println("Testing searchLyrics for Ed Sheeran - Shape of You");
        LyricsInfo lyricsInfo = source.searchLyrics("Ed Sheeran", "Shape of You", 240);
        if (lyricsInfo != null) {
            System.out.println("Lyrics Content: " + lyricsInfo.getContent());
        } else {
            System.out.println("Lyrics not supported by iTunes");
        }
        System.out.println("Last Request URL: " + source.getLastRequestUrl());
        System.out.println("Last Request Error: " + source.getLastRequestError());
    }
}
