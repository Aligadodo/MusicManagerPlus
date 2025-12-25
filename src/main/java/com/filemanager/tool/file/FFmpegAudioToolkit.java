//package com.filemanager.tool.file;
//
//import net.bramp.ffmpeg.FFmpeg;
//import net.bramp.ffmpeg.FFmpegExecutor;
//import net.bramp.ffmpeg.builder.FFmpegBuilder;
//import org.bytedeco.ffmpeg.global.avcodec;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.FFmpegFrameRecorder;
//import org.bytedeco.javacv.Frame;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.util.List;
//import java.util.concurrent.Semaphore;
//import java.util.concurrent.TimeUnit;
//
///**
// * 统一音频转换工具类
// * 支持 Native (JNI) 和 CLI (命令行) 两种模式
// */
//public class FFmpegAudioToolkit {
//    private static final Logger log = LoggerFactory.getLogger(FFmpegAudioToolkit.class);
//
//    public enum ExecutionMode { NATIVE, CLI }
//
//    private static FFmpeg ffmpeg = null;
//    private static final Semaphore semaphore= new Semaphore(10);
//
//    public static void init(FFmpeg ffmpeg){
//        FFmpegAudioToolkit.ffmpeg = ffmpeg;
//    }
//
//    /**
//     * 统一调用入口
//     */
//    public static void runJob(FFmpegBuilder builder, ExecutionMode mode) throws Exception {
//        // 1. 等待获取许可（防止高并发瞬间崩溃）
//        if (!semaphore.tryAcquire(60, TimeUnit.SECONDS)) {
//            throw new RuntimeException("转换任务排队超时");
//        }
//
//        try {
//            if (mode == ExecutionMode.NATIVE) {
//                executeNative(builder);
//            } else {
//                executeCli(builder);
//            }
//        } finally {
//            semaphore.release();
//        }
//    }
//
//    /**
//     * 模式1：Native (JNI) 方式 - 高效、低延迟、无额外进程
//     */
//    private static void executeNative(FFmpegBuilder builder) throws Exception {
//        // 从 Builder 提取参数
//        String inputPath = getInternalField(builder, "inputs", List.class).get(0).toString();
//        Object outputObj = getInternalField(builder, "outputs", List.class).get(0);
//        String outputPath = getInternalField(outputObj, "filename", String.class);
//
//        long audioBitrate = getInternalField(outputObj, "audio_bitrate", Long.class);
//        int sampleRate = getInternalField(outputObj, "audio_sample_rate", Integer.class);
//        String codec = getInternalField(outputObj, "audio_codec", String.class);
//
//        log.info("Native 模式启动: {} -> {}", inputPath, outputPath);
//
//        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath)) {
//            grabber.start();
//            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, grabber.getAudioChannels())) {
//                // 设置编码格式
//                recorder.setAudioCodec(mapCodec(codec));
//                if (audioBitrate > 0) recorder.setAudioBitrate((int) audioBitrate);
//                if (sampleRate > 0) recorder.setSampleRate(sampleRate);
//
//                recorder.start();
//                Frame frame;
//                while ((frame = grabber.grabFrame()) != null) {
//                    recorder.record(frame);
//                }
//                recorder.stop();
//            }
//            grabber.stop();
//        }
//    }
//
//    /**
//     * 模式2：CLI 方式 - 兼容性最强、适合复杂滤镜
//     */
//    private static void executeCli(FFmpegBuilder builder) throws IOException {
//        log.info("CLI 模式启动");
//        new FFmpegExecutor(ffmpeg).createJob(builder).run();
//    }
//
//    // --- 工具方法：通过反射提取 FFmpegBuilder 内部私有属性 ---
//    @SuppressWarnings("unchecked")
//    private static <T> T getInternalField(Object target, String fieldName, Class<T> type) throws Exception {
//        Field field = target.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        return (T) field.get(target);
//    }
//
//    private static int mapCodec(String codec) {
//        if (codec == null || codec.isEmpty()) {
//            // 默认返回 NONE，或者根据业务需求返回 AV_CODEC_ID_MP3
//            return avcodec.AV_CODEC_ID_NONE;
//        }
//
//        // 将 FFmpeg 的编码器字符串映射为 JavaCV/FFmpeg 的枚举 ID
//        switch (codec.toLowerCase()) {
//            // --- MP3 系列 ---
//            case "mp3":
//            case "libmp3lame":
//                return avcodec.AV_CODEC_ID_MP3;
//
//            // --- AAC 系列 ---
//            case "aac":
//                return avcodec.AV_CODEC_ID_AAC;
//
//            // --- PCM 系列 (常用于 WAV) ---
//            case "pcm_s16le":
//                return avcodec.AV_CODEC_ID_PCM_S16LE;
//            case "pcm_s24le":
//                return avcodec.AV_CODEC_ID_PCM_S24LE;
//            case "pcm_s32le":
//                return avcodec.AV_CODEC_ID_PCM_S32LE;
//            case "pcm_alaw":
//                return avcodec.AV_CODEC_ID_PCM_ALAW;
//            case "pcm_mulaw":
//                return avcodec.AV_CODEC_ID_PCM_MULAW;
//
//            // --- FLAC 系列 ---
//            case "flac":
//                return avcodec.AV_CODEC_ID_FLAC;
//
//            // --- 其他常用格式 (补充) ---
//            case "opus":
//                return avcodec.AV_CODEC_ID_OPUS;
//            case "vorbis":
//                return avcodec.AV_CODEC_ID_VORBIS;
//            case "amr_nb":
//                return avcodec.AV_CODEC_ID_AMR_NB;
//            case "amr_wb":
//                return avcodec.AV_CODEC_ID_AMR_WB;
//
//            default:
//                // 如果匹配不到，记录警告日志
//                log.warn("未知的编码器映射: {}, 将尝试由 FFmpeg 自行决定", codec);
//                return avcodec.AV_CODEC_ID_NONE;
//        }
//    }
//}