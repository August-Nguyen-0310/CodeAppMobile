package com.example.businessmanagement.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    public static final String TYPE_TEXT  = "text";
    public static final String TYPE_VOICE = "voice";
    public static final String TYPE_FILE  = "file";
    public static final String TYPE_IMAGE = "image";

    private String id;
    private String chatId;
    private String taskId;
    private String senderId;
    private String senderName;
    private String senderProfileImageUrl; // Added for avatar in chat
    private String content;       // text content OR display caption
    private String messageType;   // text | voice | file
    private String fileUrl;       // Storage download URL
    private String fileName;      // original file name
    private long   fileSize;      // bytes
    private long   audioDuration; // ms (voice only)
    private long   timestamp;

    // --- New features (Reply, Pin) ---
    private boolean isPinned;
    private String  replyToMessageId;
    private String  replyToSenderName;
    private String  replyToContent;

    public Message() {}

    // --- Text constructor ---
    public Message(String id, String chatId, String taskId,
                   String senderId, String senderName, String senderProfileImageUrl,
                   String content, long timestamp) {
        this.id          = id;
        this.chatId      = chatId;
        this.taskId      = taskId;
        this.senderId    = senderId;
        this.senderName  = senderName;
        this.senderProfileImageUrl = senderProfileImageUrl;
        this.content     = content;
        this.messageType   = TYPE_TEXT;
        this.timestamp     = timestamp;
        this.isPinned      = false;
    }

    // --- Voice / File constructor ---
    public Message(String id, String chatId, String taskId,
                   String senderId, String senderName, String senderProfileImageUrl,
                   String messageType,
                   String fileUrl, String fileName,
                   long fileSize, long audioDuration,
                   long timestamp) {
        this.id            = id;
        this.chatId        = chatId;
        this.taskId        = taskId;
        this.senderId      = senderId;
        this.senderName    = senderName;
        this.senderProfileImageUrl = senderProfileImageUrl;
        this.messageType   = messageType;
        this.fileUrl       = fileUrl;
        this.fileName      = fileName;
        this.fileSize      = fileSize;
        this.audioDuration = audioDuration;
        this.timestamp     = timestamp;
        this.isPinned      = false;
    }

    // Getters & Setters
    public String getId()                        { return id; }
    public void   setId(String id)               { this.id = id; }

    public String getChatId()                    { return chatId; }
    public void   setChatId(String chatId)       { this.chatId = chatId; }

    public String getTaskId()                    { return taskId; }
    public void   setTaskId(String taskId)       { this.taskId = taskId; }

    public String getSenderId()                  { return senderId; }
    public void   setSenderId(String s)          { this.senderId = s; }

    public String getSenderName()                { return senderName; }
    public void   setSenderName(String s)        { this.senderName = s; }

    public String getSenderProfileImageUrl()     { return senderProfileImageUrl; }
    public void   setSenderProfileImageUrl(String url) { this.senderProfileImageUrl = url; }

    public String getContent()                   { return content; }
    public void   setContent(String content)     { this.content = content; }

    public String getMessageType()               { return messageType; }
    public void   setMessageType(String t)       { this.messageType = t; }

    public String getFileUrl()                   { return fileUrl; }
    public void   setFileUrl(String fileUrl)     { this.fileUrl = fileUrl; }

    public String getFileName()                  { return fileName; }
    public void   setFileName(String fileName)   { this.fileName = fileName; }

    public long   getFileSize()                  { return fileSize; }
    public void   setFileSize(long fileSize)     { this.fileSize = fileSize; }

    public long   getAudioDuration()             { return audioDuration; }
    public void   setAudioDuration(long d)       { this.audioDuration = d; }

    public long   getTimestamp()                 { return timestamp; }
    public void   setTimestamp(long timestamp)   { this.timestamp = timestamp; }

    public boolean isPinned()                    { return isPinned; }
    public void    setPinned(boolean pinned)     { isPinned = pinned; }

    public String getReplyToMessageId()          { return replyToMessageId; }
    public void   setReplyToMessageId(String id) { this.replyToMessageId = id; }

    public String getReplyToSenderName()         { return replyToSenderName; }
    public void   setReplyToSenderName(String s) { this.replyToSenderName = s; }

    public String getReplyToContent()            { return replyToContent; }
    public void   setReplyToContent(String c)    { this.replyToContent = c; }
}