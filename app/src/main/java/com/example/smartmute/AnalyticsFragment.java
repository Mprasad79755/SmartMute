package com.example.smartmute;



import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.example.smartmute.R;
import com.example.smartmute.SmartMuteDatabaseHelper;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private SmartMuteDatabaseHelper databaseHelper;

    private BarChart barChart;
    private PieChart pieChart;
    private TextView tvTotalSilentTime, tvTotalVibrateTime, tvEmergencyCount;

    public AnalyticsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        initializeViews(view);
        setupDatabase();
        setupCharts();
        loadAnalyticsData();

        return view;
    }

    private void initializeViews(View view) {
        barChart = view.findViewById(R.id.bar_chart);
        pieChart = view.findViewById(R.id.pie_chart);
        tvTotalSilentTime = view.findViewById(R.id.tv_total_silent_time);
        tvTotalVibrateTime = view.findViewById(R.id.tv_total_vibrate_time);
        tvEmergencyCount = view.findViewById(R.id.tv_emergency_count);
    }

    private void setupDatabase() {
        databaseHelper = new SmartMuteDatabaseHelper(requireContext());
    }

    private void setupCharts() {
        setupBarChart();
        setupPieChart();
    }

    private void setupBarChart() {
        // Configure bar chart appearance
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);

        // X-axis configuration
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(getDaysOfWeek()));

        // Y-axis configuration
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Legend
        barChart.getLegend().setEnabled(true);
        barChart.getLegend().setTextColor(Color.WHITE);

        barChart.animateY(1000);
    }

    private void setupPieChart() {
        // Configure pie chart appearance
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.setHoleRadius(45f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Mode Usage");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setEntryLabelColor(Color.WHITE);

        // Legend
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);

        pieChart.animateY(1000);
    }

    private void loadAnalyticsData() {
        loadBarChartData();
        loadPieChartData();
        loadSummaryStats();
    }

    private void loadBarChartData() {
        // Sample data - in real app, this would come from database
        List<BarEntry> silentEntries = new ArrayList<>();
        List<BarEntry> vibrateEntries = new ArrayList<>();
        List<BarEntry> normalEntries = new ArrayList<>();

        // Weekly data (7 days)
        float[] silentHours = {4f, 6f, 3f, 5f, 7f, 2f, 8f};
        float[] vibrateHours = {2f, 3f, 4f, 2f, 1f, 3f, 1f};
        float[] normalHours = {18f, 15f, 17f, 17f, 16f, 19f, 15f};

        for (int i = 0; i < 7; i++) {
            silentEntries.add(new BarEntry(i, silentHours[i]));
            vibrateEntries.add(new BarEntry(i, vibrateHours[i]));
            normalEntries.add(new BarEntry(i, normalHours[i]));
        }

        BarDataSet silentDataSet = new BarDataSet(silentEntries, "Silent");
        silentDataSet.setColor(getResources().getColor(R.color.electric_blue));
        silentDataSet.setValueTextColor(Color.WHITE);

        BarDataSet vibrateDataSet = new BarDataSet(vibrateEntries, "Vibrate");
        vibrateDataSet.setColor(getResources().getColor(R.color.aqua_glow));
        vibrateDataSet.setValueTextColor(Color.WHITE);

        BarDataSet normalDataSet = new BarDataSet(normalEntries, "Normal");
        normalDataSet.setColor(getResources().getColor(R.color.metallic_silver));
        normalDataSet.setValueTextColor(Color.WHITE);

        BarData barData = new BarData(silentDataSet, vibrateDataSet, normalDataSet);
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        float groupSpace = 0.08f;
        float barSpace = 0.02f;
        float barWidth = 0.3f;

        barData.setBarWidth(barWidth);
        barChart.setData(barData);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.invalidate();
    }

    private void loadPieChartData() {
        List<PieEntry> entries = new ArrayList<>();

        // Sample data
        entries.add(new PieEntry(45f, "Normal"));
        entries.add(new PieEntry(30f, "Silent"));
        entries.add(new PieEntry(25f, "Vibrate"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                getResources().getColor(R.color.metallic_silver),
                getResources().getColor(R.color.electric_blue),
                getResources().getColor(R.color.aqua_glow)
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value) + "%";
            }
        });

        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void loadSummaryStats() {
        // Sample data - in real app, calculate from database logs
        tvTotalSilentTime.setText("35 hours");
        tvTotalVibrateTime.setText("18 hours");
        tvEmergencyCount.setText("3 overrides");
    }

    private String[] getDaysOfWeek() {
        return new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
