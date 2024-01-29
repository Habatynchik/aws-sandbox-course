package com.task10.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.task10.dto.*;
import com.task10.model.Reservations;
import com.task10.model.Tables;

import java.math.BigDecimal;
import java.util.*;


public class DynamoDbService {

    public TablesResponse getAllTables(AmazonDynamoDB amazonDynamoDB) {
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        Table table = dynamoDB.getTable("cmtr-c04fb6ad-Tables-test");

        Iterator<Item> iterator = table.scan().iterator();
        TablesResponse response = new TablesResponse();
        ArrayList<Tables> tables = new ArrayList<>();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            Tables tableItem = new Tables();
            tableItem.setId(item.getInt("id"));
            tableItem.setVip((item.getNumber("isVip").equals(BigDecimal.ONE)));
            tableItem.setNumber(item.getInt("number"));
            tableItem.setMinOrder(item.getInt("minOrder"));
            tableItem.setPlaces(item.getInt("places"));
            tables.add(tableItem);
        }
        response.setTables(tables);
        return response;
    }

    public Tables getTableById(AmazonDynamoDB amazonDynamoDB, int tableId) {
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        Table table = dynamoDB.getTable("cmtr-c04fb6ad-Tables-test");

        GetItemSpec getItemSpec = new GetItemSpec().withPrimaryKey("id", tableId);
        Item item = table.getItem(getItemSpec);

        Tables tables = new Tables();
        tables.setId(item.getInt("id"));
        tables.setVip((item.getNumber("isVip").equals(BigDecimal.ONE)));
        tables.setNumber(item.getInt("number"));
        tables.setMinOrder(item.getInt("minOrder"));
        tables.setPlaces(item.getInt("places"));
        return tables;
    }

    public TableCreationResponse createTable(AmazonDynamoDB amazonDynamoDB, TablesRequest tablesRequest) {
        Tables tables = new Tables();
        tables.setId(tablesRequest.getId());
        tables.setNumber(tablesRequest.getNumber());
        tables.setMinOrder(tablesRequest.getMinOrder());
        tables.setVip(tablesRequest.isVip());
        tables.setPlaces(tablesRequest.getPlaces());

        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);
        mapper.save(tables);
        TableCreationResponse tableCreationResponse = new TableCreationResponse();
        tableCreationResponse.setId(tablesRequest.getId());

        return tableCreationResponse;
    }

    public ReservationsResponse getAllReservations(AmazonDynamoDB amazonDynamoDB) {
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        Table table = dynamoDB.getTable("cmtr-c04fb6ad-Reservations-test");

        Iterator<Item> iterator = table.scan().iterator();
        ReservationsResponse response = new ReservationsResponse();
        ArrayList<Reservations> reservations = new ArrayList<>();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            Reservations reservation = new Reservations();
            reservation.setId(item.getString("id"));
            reservation.setDate(item.getString("date"));
            reservation.setClientName(item.getString("clientName"));
            reservation.setSlotTimeEnd(item.getString("slotTimeEnd"));
            reservation.setSlotTimeStart(item.getString("slotTimeStart"));
            reservation.setPhoneNumber(item.getString("phoneNumber"));
            reservation.setTableNumber(item.getInt("tableNumber"));
            reservations.add(reservation);
        }
        response.setReservations(reservations);
        return response;
    }


    public ReservationCreationResponse createReservation(AmazonDynamoDB amazonDynamoDB, ReservationsRequest reservationsRequest) {
        if(getAllTables(amazonDynamoDB).getTables().stream().noneMatch(x->x.getNumber() == reservationsRequest.getTableNumber())){
            System.out.println("Table does not exist");
            throw new RuntimeException();
        }
        ReservationsResponse checkReservationsResponse = getAllReservations(amazonDynamoDB);

        checkReservationsResponse.getReservations().stream().filter(x ->
                x.getClientName().equals(reservationsRequest.getClientName())
                        && x.getDate().equals(reservationsRequest.getDate())
                        && x.getTableNumber() == reservationsRequest.getTableNumber()
                        && x.getSlotTimeEnd().equals(reservationsRequest.getSlotTimeEnd())
                        && x.getSlotTimeStart().equals(reservationsRequest.getSlotTimeStart())
                        && x.getPhoneNumber().equals(reservationsRequest.getPhoneNumber())
        ).findFirst().ifPresent(x -> {
            System.out.println("Item already exists");
            throw new RuntimeException();
        });

        Reservations reservations = new Reservations();
        String reservationId = UUID.randomUUID().toString();
        reservations.setId(reservationId);
        reservations.setPhoneNumber(reservationsRequest.getPhoneNumber());
        reservations.setDate(reservationsRequest.getDate());
        reservations.setSlotTimeStart(reservationsRequest.getSlotTimeStart());
        reservations.setSlotTimeEnd(reservationsRequest.getSlotTimeEnd());
        reservations.setClientName(reservationsRequest.getClientName());
        reservations.setTableNumber(reservationsRequest.getTableNumber());

        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);
        mapper.save(reservations);

        ReservationCreationResponse response = new ReservationCreationResponse();
        response.setReservationId(reservationId);
        return response;
    }

}
