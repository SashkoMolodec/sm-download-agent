package com.sashkomusic.downloadagent.domain.model;

import java.util.List;
import java.util.Map;

public record DownloadOption(
        String id,                 // Унікальний ID (для UI/ідентифікації)
        String sourceName,         // Назва джерела (напр. "Soulseek")
        String distributorName,    // Ім'я користувача (напр. "waxwax")
        int totalSize,            // Загальний розмір релізу
        List<FileItem> files,      // Список файлів всередині
        Map<String, String> technicalMetadata // Технічні дані для скачування (token, username)
) {
    public record FileItem(
            String filename,
            int size,
            Integer bitRate,    // Може бути null (для FLAC)
            Integer bitDepth,   // Може бути null (для MP3)
            Integer sampleRate, // Може бути null
            int lengthSeconds   // Тривалість
    ) {
    }
}