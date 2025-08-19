package com.example.appbanhang;

public class Product {
    private String name;
    private String description;
    private Double price;
    private int imageResId;     // ảnh local (drawable)
    private String imageUrl;    // ảnh online (URL từ Firestore)
    private String category;    // nhóm món (bestseller, promo, pasta, drink...)

    // BẮT BUỘC: constructor rỗng cho Firestore
    public Product() {}

    // Constructor đầy đủ
    public Product(String name, String description, Double price,
                   int imageResId, String imageUrl, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    // Getter (Firestore cần có để map dữ liệu)
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Double getPrice() { return price; }
    public int getImageResId() { return imageResId; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }

    // Setter
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(Double price) { this.price = price; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCategory(String category) { this.category = category; }
}
