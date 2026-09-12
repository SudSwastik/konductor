package com.konductor.projector.service;

import com.konductor.projector.dto.response.EventResponse;
import java.util.List;

public interface EventService {
    List<EventResponse> list();
    EventResponse get(String eventUid);
}
