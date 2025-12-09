package com.sashkomusic.downloadagent.config;

import com.sashkomusic.downloadagent.domain.MusicSourcePort;
import com.sashkomusic.downloadagent.domain.model.DownloadEngine;
import com.sashkomusic.downloadagent.infrastracture.client.applemusic.AppleMusicClient;
import com.sashkomusic.downloadagent.infrastracture.client.bandcamp.BandcampClient;
import com.sashkomusic.downloadagent.infrastracture.client.qobuz.QobuzClient;
import com.sashkomusic.downloadagent.infrastracture.client.slskd.SlskdClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MusicSourceConfig {

    @Bean
    public Map<DownloadEngine, MusicSourcePort> musicSources(
            QobuzClient qobuzClient,
            SlskdClient slskdClient,
            AppleMusicClient appleMusicClient,
            BandcampClient bandcampClient
    ) {
        return Map.of(
                DownloadEngine.QOBUZ, qobuzClient,
                DownloadEngine.SOULSEEK, slskdClient,
                DownloadEngine.APPLE_MUSIC, appleMusicClient,
                DownloadEngine.BANDCAMP, bandcampClient
        );
    }
}
