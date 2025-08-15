package com.example.appbanhang;



import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView rvProducts;
    List<Product> productList;
    ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 3)); // 2 cột

        productList = new ArrayList<>();
        productList.add(new Product("Tiện Lợi 53K", "01 Gà rán\n01 Khoai tây chiên", "53.000 đ", R.drawable.r1));
        productList.add(new Product("Tiện Lợi 59K", "01 Cơm gà sốt H&S\n01 Súp gà", "59.000 đ",R.drawable.r1));
        productList.add(new Product("Tiện Lợi 70K", "01 Gà rán\n01 Mì Ý (M)", "70.000 đ",R.drawable.r1));




        adapter = new ProductAdapter(this, productList);
        rvProducts.setAdapter(adapter);
    }
}
