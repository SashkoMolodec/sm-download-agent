package com.sashkomusic.downloadagent.infrastracture.client.bandcamp;

public record BandcampSearchResult(
        String artist,
        String title,
        String type,
        String url
) {
}
