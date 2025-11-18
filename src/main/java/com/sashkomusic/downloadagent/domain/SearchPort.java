package com.sashkomusic.downloadagent.domain;

import com.sashkomusic.downloadagent.domain.model.DownloadOption;

import java.util.List;

public interface SearchPort {
    List<DownloadOption> search(String artist, String release);
}
