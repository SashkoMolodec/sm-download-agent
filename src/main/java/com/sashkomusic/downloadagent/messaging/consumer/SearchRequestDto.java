package com.sashkomusic.downloadagent.messaging.consumer;

public record SearchRequestDto(
        long chatId,
        String artist,
        String title) {

    public static SearchRequestDto of(long chatId, String artist, String title) {
        return new SearchRequestDto(chatId, artist, title);
    }
}