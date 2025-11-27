package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;

import java.util.List;

public interface MusicSourcePort {

    List<DownloadOption> search(String artist, String release);

    String initiateDownload(DownloadOption option);
}
