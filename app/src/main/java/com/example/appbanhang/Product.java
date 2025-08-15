package com.example.appbanhang;

public class Product {
    private final String name;
    private final String description;
    private final String price;
    private final int imageResId; // thêm

    public Product(String name, String description, String price) {
        this(name, description, price, 0);
    }

    public Product(String name, String description, String price, int imageResId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrice() { return price; }
    public int getImageResId() { return imageResId; }
}
