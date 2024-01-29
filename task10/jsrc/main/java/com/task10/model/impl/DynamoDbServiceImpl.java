package com.task10.model.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.task10.dto.*;
import com.task10.exception.ReservationAlreadyExist;
import com.task10.exception.TableNotFoundException;
import com.task10.model.DynamoDbService;
import com.task10.service.Reservations;
import com.task10.service.Tables;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DynamoDbServiceImpl implements DynamoDbService {
    private static final String TABLES_TABLE = "cmtr-985d4752-Tables-test";
    private static final String RESERVATIONS_TABLE = "cmtr-985d4752-Reservations-test";

    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDB dynamoDB;

    public DynamoDbServiceImpl(AmazonDynamoDB amazonDynamoDB) {
        this.amazonDynamoDB = amazonDynamoDB;
        dynamoDB = new DynamoDB(this.amazonDynamoDB);
    }

    @Override
    public GetTablesResponse getAllTables() {
        Table table = dynamoDB.getTable(TABLES_TABLE);
        List<Tables> tables = new ArrayList<>();

        table.scan().forEach(e -> tables.add(Tables.builder()
                .id(e.getInt("id"))
                .number(e.getInt("number"))
                .places(e.getInt("places"))
                .isVip(e.getNumber("isVip").equals(BigDecimal.ONE))
                .minOrder(e.getInt("minOrder"))
                .build()));

        return new GetTablesResponse(tables);
    }

    @Override
    public Tables getTablesById(Integer id) throws TableNotFoundException {
        Table table = dynamoDB.getTable(TABLES_TABLE);
        GetItemSpec getItemSpec = new GetItemSpec().withPrimaryKey("id", id);
        try {
            Item item = table.getItem(getItemSpec);

            return Tables.builder()
                    .id(item.getInt("id"))
                    .number(item.getInt("number"))
                    .places(item.getInt("places"))
                    .isVip(item.getNumber("isVip").equals(BigDecimal.ONE))
                    .minOrder(item.getInt("minOrder"))
                    .build();
        } catch (Exception e) {
            throw new TableNotFoundException();
        }


    }

    @Override
    public GetReservationsResponse getAllReservations() {
        Table table = dynamoDB.getTable(RESERVATIONS_TABLE);
        List<Reservations> reservations = new ArrayList<>();

        table.scan().forEach(e -> reservations.add(Reservations.builder()
                .id(e.getString("id"))
                .tableNumber(e.getInt("tableNumber"))
                .clientName(e.getString("clientName"))
                .phoneNumber(e.getString("phoneNumber"))
                .date(e.getString("date"))
                .slotTimeStart(e.getString("slotTimeStart"))
                .slotTimeEnd(e.getString("slotTimeEnd"))
                .build()));

        return new GetReservationsResponse(reservations);
    }

    @Override
    public PostTablesResponse createTables(PostTablesRequest request) {
        DynamoDBMapper dbMapper = new DynamoDBMapper(amazonDynamoDB);

        Tables tables = Tables.builder()
                .id(request.getId())
                .number(request.getNumber())
                .places(request.getPlaces())
                .isVip(request.isVip())
                .minOrder(request.getMinOrder())
                .build();

        dbMapper.save(tables);

        return new PostTablesResponse(tables.getId());
    }

    @Override
    public PostReservationsResponse createReservations(PostReservationsRequest request) throws TableNotFoundException, ReservationAlreadyExist {
        DynamoDBMapper dbMapper = new DynamoDBMapper(amazonDynamoDB);
        List<Tables> tablesList = getAllTables().getTables();
        List<Reservations> reservationsList = getAllReservations().getReservations();

        if (tablesList.stream().noneMatch(e -> e.getNumber() == request.getTableNumber())) {
            throw new TableNotFoundException();
        }

        Reservations reservations = Reservations.builder()
                .id(UUID.randomUUID().toString())
                .tableNumber(request.getTableNumber())
                .clientName(request.getClientName())
                .phoneNumber(request.getPhoneNumber())
                .date(request.getDate())
                .slotTimeStart(request.getSlotTimeStart())
                .slotTimeEnd(request.getSlotTimeEnd())
                .build();

        if (reservationsList.stream().anyMatch(e -> e.equalsWithoutId(reservations))) {
            throw new ReservationAlreadyExist();
        }

        dbMapper.save(reservations);
        return new PostReservationsResponse(reservations.getId());
    }
}
