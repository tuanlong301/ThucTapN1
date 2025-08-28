package com.example.appbanhang;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Order {
    private String id;
    private String userId;
    private String tableNumber;
    private List<Map<String, Object>> items;
    private double total;
    private String notes;
    private String paymentMethod;
    private double cashAmount;
    private double change;
    private String status;
    private Date timestamp;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public double getCashAmount() { return cashAmount; }
    public void setCashAmount(double cashAmount) { this.cashAmount = cashAmount; }
    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}