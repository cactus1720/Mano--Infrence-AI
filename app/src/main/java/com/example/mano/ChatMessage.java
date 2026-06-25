package com.example.mano;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private boolean isLoading;

    // Hii message constructor ya normal
    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.isLoading = false;
    }

    // Loading/Typing state constructor
    public ChatMessage(boolean isLoading) {
        this.text = "";
        this.isUser = false;
        this.isLoading = isLoading;
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public boolean isLoading() { return isLoading; }
}