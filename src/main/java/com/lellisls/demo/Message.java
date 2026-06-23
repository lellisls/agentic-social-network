package com.lellisls.demo;

public record Message(String message) {
    public Message() {
        this("");
    } // JSON-B/Jackson
}
