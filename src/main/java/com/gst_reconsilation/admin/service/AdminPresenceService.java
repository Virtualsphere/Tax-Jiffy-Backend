package com.gst_reconsilation.admin.service;

import com.gst_reconsilation.admin.dto.UserLastActiveResponse;
import com.gst_reconsilation.presence.dto.PresenceEvent;
import com.gst_reconsilation.presence.service.PresenceTracker;
import com.gst_reconsilation.user.entity.UserDetails;
import com.gst_reconsilation.user.repository.UserDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPresenceService {

    private final UserDetailsRepository userDetailsRepository;
    private final PresenceTracker presenceTracker;

    /** REST snapshot for the admin panel's initial page load, before /topic/presence delivers anything live. */
    public List<UserLastActiveResponse> listLastActive() {
        return userDetailsRepository.findAll().stream()
                .map(u -> UserLastActiveResponse.builder()
                        .userId(u.getId())
                        .userName(u.getUserName())
                        .userEmail(u.getUserEmail())
                        .companyName(u.getCompany() != null ? u.getCompany().getCompanyName() : null)
                        .online(presenceTracker.isOnline(u.getId()))
                        .lastActiveAt(u.getLastActiveAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<PresenceEvent> listOnline() {
        return presenceTracker.onlineUserIds().stream()
                .map(userDetailsRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(this::toOnlineEvent)
                .collect(Collectors.toList());
    }

    private PresenceEvent toOnlineEvent(UserDetails u) {
        return PresenceEvent.builder()
                .userId(u.getId())
                .userName(u.getUserName())
                .companyId(u.getCompany() != null ? u.getCompany().getId() : null)
                .companyName(u.getCompany() != null ? u.getCompany().getCompanyName() : null)
                .status("ONLINE")
                .at(u.getLastActiveAt())
                .build();
    }
}
