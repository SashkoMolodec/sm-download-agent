package com.sashkomusic.downloadagent.infrastracture.client.qobuz;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QobuzQualityMapper {

    /**
     * Maps Qobuz quality code to human-readable label
     */
    public String getQualityLabel(int qualityCode) {
        return switch (qualityCode) {
            case 5 -> "MP3 320kbps";
            case 6 -> "FLAC 16bit/44.1kHz";
            case 7 -> "FLAC 24bit/96kHz (Hi-Res)";
            case 27 -> "FLAC Maximum Quality";
            default -> "Quality " + qualityCode;
        };
    }

    /**
     * Maps quality code to bitrate (for MP3)
     * Returns null for lossless formats
     */
    public Integer getBitRate(int qualityCode) {
        return switch (qualityCode) {
            case 5 -> 320;
            case 6, 7, 27 -> null; // Lossless
            default -> null;
        };
    }

    /**
     * Maps quality code to bit depth
     */
    public Integer getBitDepth(int qualityCode) {
        return switch (qualityCode) {
            case 5 -> null; // MP3 doesn't have bit depth
            case 6 -> 16;
            case 7, 27 -> 24;
            default -> null;
        };
    }

    /**
     * Maps quality code to sample rate (Hz)
     */
    public Integer getSampleRate(int qualityCode) {
        return switch (qualityCode) {
            case 5 -> null; // MP3
            case 6 -> 44100;
            case 7, 27 -> 96000;
            default -> null;
        };
    }

    /**
     * Determines if quality is lossless
     */
    public boolean isLossless(int qualityCode) {
        return qualityCode != 5; // Everything except MP3 320
    }

    /**
     * Returns expected file extension for quality
     */
    public String getFileExtension(int qualityCode) {
        return switch (qualityCode) {
            case 5 -> ".mp3";
            case 6, 7, 27 -> ".flac";
            default -> ".flac";
        };
    }

    /**
     * Sorts quality codes by priority (best first)
     */
    public List<Integer> sortByPriority(List<Integer> qualities) {
        return qualities.stream()
                .sorted((q1, q2) -> {
                    // 27 (max) > 7 (24/96) > 6 (16/44) > 5 (320 MP3)
                    int priority1 = getPriority(q1);
                    int priority2 = getPriority(q2);
                    return Integer.compare(priority2, priority1); // Descending
                })
                .toList();
    }

    private int getPriority(int qualityCode) {
        return switch (qualityCode) {
            case 27 -> 4; // Maximum quality
            case 7 -> 3;  // 24/96 Hi-Res
            case 6 -> 2;  // 16/44 CD quality
            case 5 -> 1;  // MP3 320
            default -> 0;
        };
    }

    /**
     * Estimates average file size in MB for a 4-minute track
     */
    public int estimateFileSize(int qualityCode, int lengthSeconds) {
        int bytesPerSecond = switch (qualityCode) {
            case 5 -> 40000;      // MP3 320kbps ≈ 40 KB/s
            case 6 -> 176000;     // FLAC 16/44 ≈ 176 KB/s (1411 kbps)
            case 7, 27 -> 576000; // FLAC 24/96 ≈ 576 KB/s (4608 kbps)
            default -> 200000;
        };

        return (bytesPerSecond * lengthSeconds) / (1024 * 1024); // Convert to MB
    }
}
