package com.example.musicapp.fragment;
import androidx.annotation.*;
import android.os.Bundle;
import android.support.*;
import android.view.*;
import androidx.fragment.app.Fragment;
import android.support.v4.app.*;

import com.example.musicapp.R;

public class Fragment_Home extends Fragment {
    View view;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.layout_home, container,false);
        return view;
    }
}
