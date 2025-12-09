package com.sashkomusic.downloadagent.infrastracture.client.applemusic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ITunesSearchClient {

    private static final String ITUNES_SEARCH_API = "https://itunes.apple.com/search";
    private static final int MAX_RESULTS = 3;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AppleMusicSearchResult> search(String artist, String title) {
        try {
            log.info("Searching Apple Music: artist={}, title={}", artist, title);

            String searchTerm = buildSearchTerm(artist, title);
            String url = String.format("%s?term=%s&entity=album&limit=5",
                    ITUNES_SEARCH_API,
                    searchTerm);

            log.debug("iTunes API URL: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) {
                log.warn("Empty response from iTunes API");
                return Collections.emptyList();
            }

            iTunesSearchResponse searchResponse = objectMapper.readValue(response, iTunesSearchResponse.class);

            if (searchResponse.results == null || searchResponse.results.isEmpty()) {
                log.info("No results found in iTunes for: {}", searchTerm);
                return Collections.emptyList();
            }

            log.info("Found {} results from iTunes API", searchResponse.results.size());

            return searchResponse.results.stream()
                    .limit(MAX_RESULTS)
                    .map(this::toSearchResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching Apple Music: artist={}, title={}", artist, title, e);
            return Collections.emptyList();
        }
    }

    private String buildSearchTerm(String artist, String title) {
        String combined = (artist + " " + title).trim();
        return combined.replace(" ", "+");
    }

    private AppleMusicSearchResult toSearchResult(iTunesAlbum album) {
        String displayName = String.format("%s - %s (%d tracks)",
                album.artistName,
                album.collectionName,
                album.trackCount != null ? album.trackCount : 0);

        return new AppleMusicSearchResult(
                String.valueOf(album.collectionId),
                album.collectionViewUrl,
                album.artistName,
                album.collectionName,
                displayName,
                album.trackCount != null ? album.trackCount : 0
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class iTunesSearchResponse {
        public int resultCount;
        public List<iTunesAlbum> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class iTunesAlbum {
        public String wrapperType;
        public String collectionType;
        public long collectionId;
        public String artistName;
        public String collectionName;
        public String collectionViewUrl;
        public Integer trackCount;
    }

    public record AppleMusicSearchResult(
            String id,
            String url,
            String artistName,
            String albumName,
            String displayName,
            int trackCount
    ) {
    }
}
