package com.joelzhu.piechart;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final String[] columns = new String[]{"A", "B", "C", "D", "E"};
        final int[] weights = new int[]{15, 40, 23, 16, 21};
        final String[] colors = new String[]{"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#00FFFF"};

        JZPieChart pieChart = (JZPieChart) findViewById(R.id.pieChart);
        pieChart.initPieChart(columns, weights, colors);
        pieChart.drawPieChartWithAnimation();
        pieChart.setOnColumnClickListener(new JZPieChart.OnColumnClickListener() {
            @Override
            public void onColumnClick(int index) {
                Toast.makeText(MainActivity.this, columns[index], Toast.LENGTH_SHORT).show();
            }
        });
    }
}