package com.task10.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostReservationsRequest {
    private int tableNumber;
    private String clientName;
    private String phoneNumber;
    private String date;
    private String slotTimeStart;
    private String slotTimeEnd;
}
