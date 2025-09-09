package com.example.appbanhang.model;

public class Product {
    private String id;
    private String name;
    private String description;
    private Double price;
    private int imageResId;     // ảnh local
    private String imageUrl;    // ảnh online (URL từ Firestore)
    private String category;    // nhóm món (bestseller, promo, pasta, drink...)
    private int qty = 0;        // Số lượng cục bộ, mặc định là 0

    // BẮT BUỘC: constructor rỗng cho Firestore
    public Product() {}

    // Constructor dựa trên dữ liệu từ Firestore (food_001)
    public Product(String id, String name, String description, Double price,
                   String imageUrl, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.qty = 0; // Mặc định qty = 0 khi tải từ food_001
    }

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; } // Dùng để cập nhật qty cục bộ khi cần
}