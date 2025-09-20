package com.example.appbanhang.owner;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.example.appbanhang.R;

public class OwnerProductsFragment extends Fragment {
    private RecyclerView rv;
    private TextView tvEmpty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_owner_products, container, false);
        rv = v.findViewById(R.id.rvProducts);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        // TODO: setAdapter(...)
        return v;
    }
}
