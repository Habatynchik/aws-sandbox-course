package com.task10.model;

import com.task10.dto.*;
import com.task10.exception.ReservationAlreadyExist;
import com.task10.exception.TableNotFoundException;
import com.task10.service.Tables;

public interface DynamoDbService {
    GetTablesResponse getAllTables();

    Tables getTablesById(Integer id) throws TableNotFoundException;

    GetReservationsResponse getAllReservations();

    PostTablesResponse createTables(PostTablesRequest request);

    PostReservationsResponse createReservations(PostReservationsRequest request) throws TableNotFoundException, ReservationAlreadyExist;
}
