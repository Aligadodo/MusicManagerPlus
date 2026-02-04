package com.filemanager.plugin.impl.audioconverter.converter;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.audioconverter.enums.AudioFormat;
import com.filemanager.plugin.impl.audioconverter.enums.Channels;
import com.filemanager.plugin.impl.audioconverter.enums.SampleRate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FFmpegConverter {
    
    public static boolean convertAudio(File sourceFile, File targetFile, PluginConfigDTO config, ExecutionContext context) {
        Object ffmpegPathObj = config.getValue("ffmpegPath");
        String ffmpegPath = ffmpegPathObj != null ? ffmpegPathObj.toString() : "ffmpeg";
        
        Object targetFormatCodeObj = config.getValue("targetFormat");
        String targetFormatCode = targetFormatCodeObj != null ? targetFormatCodeObj.toString() : AudioFormat.WAV_CD_STANDARD.getCode();
        AudioFormat targetFormat = AudioFormat.fromCode(targetFormatCode);
        
        Object sampleRateCodeObj = config.getValue("sampleRate");
        String sampleRateCode = sampleRateCodeObj != null ? sampleRateCodeObj.toString() : SampleRate.ORIGINAL.getCode();
        SampleRate sampleRate = SampleRate.fromCode(sampleRateCode);
        
        Object channelsCodeObj = config.getValue("channels");
        String channelsCode = channelsCodeObj != null ? channelsCodeObj.toString() : Channels.STEREO.getCode();
        Channels channels = Channels.fromCode(channelsCode);
        
        Object ffmpegThreadsObj = config.getValue("ffmpegThreads");
        Integer ffmpegThreads = ffmpegThreadsObj instanceof Integer ? (Integer) ffmpegThreadsObj : 4;
        
        try {
            List<String> command = buildFFmpegCommand(
                ffmpegPath, sourceFile, targetFile, 
                targetFormat, sampleRate, channels, ffmpegThreads
            );
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                context.logDebug(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                context.logInfo("FFmpeg conversion completed successfully");
                return true;
            } else {
                context.logError("FFmpeg conversion failed with exit code: " + exitCode);
                return false;
            }
        } catch (Exception e) {
            context.logError("Error executing FFmpeg: " + e.getMessage());
            return false;
        }
    }
    
    private static List<String> buildFFmpegCommand(
            String ffmpegPath, File sourceFile, File targetFile,
            AudioFormat targetFormat, SampleRate sampleRate, 
            Channels channels, int ffmpegThreads) {
        
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(sourceFile.getPath());
        
        if (!sampleRate.isOriginal() && sampleRate.getValue() != null) {
            command.add("-ar");
            command.add(String.valueOf(sampleRate.getValue()));
        }
        
        if (!channels.isOriginal() && channels.getValue() != null) {
            command.add("-ac");
            command.add(String.valueOf(channels.getValue()));
        }
        
        command.add("-threads");
        command.add(String.valueOf(ffmpegThreads));
        
        command.add("-f");
        command.add(targetFormat.getFFmpegFormat());
        
        command.add(targetFile.getPath());
        
        return command;
    }
}