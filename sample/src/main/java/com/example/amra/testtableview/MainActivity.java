package com.example.amra.testtableview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.amra.scalabletableview.ScalableTableView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ScalableTableView tableView = findViewById(R.id.tableview);
        tableView.setColumns(100);
        tableView.setRows(100);
        tableView.setCellWidth(80);
        tableView.setCellHeight(80);
        tableView.setHeaderHeight(100);
        tableView.setHeaderWidth(100);
    }


}
