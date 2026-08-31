package com.gst_reconsilation.presence.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class PresenceEvent {
    private Integer userId;
    private String userName;
    private Integer companyId;
    private String companyName;
    /** ONLINE | OFFLINE */
    private String status;
    private LocalDateTime at;
}
