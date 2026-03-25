package com.project.InboxBuddy;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
}
