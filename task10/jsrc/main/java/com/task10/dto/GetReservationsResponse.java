package com.task10.dto;

import com.task10.service.Reservations;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetReservationsResponse {
    private List<Reservations> reservations;
}
