package com.sashkomusic.downloadagent.infrastracture.client.slskd;

public class NoSearchResultsException extends RuntimeException {
    public NoSearchResultsException(String message) {
        super(message);
    }
}
