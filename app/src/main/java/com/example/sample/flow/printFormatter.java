package com.example.sample.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class printFormatter {
    public static void printSection(String title, char separator) {
        int lineLength = Math.max(50, title.length() + 4);
        String line = "";
        for (int i = 0; i < lineLength; i++) {
            line += String.valueOf(separator);
        }

        if (!title.isEmpty()) {
            System.out.println(line);
            System.out.println(" " + title);
        }
        System.out.println(line);
    }

    // キーバリューマッピングのテーブル形式出力
    public static <K, V> void printKeyValueTable(Map<K, V> map, String keyHeader, String valueHeader) {
        if (map == null || map.isEmpty()) {
            System.out.println("No entries found.");
            return;
        }

        List<List<String>> tableData = new ArrayList<>();
        tableData.add(Arrays.asList(keyHeader, valueHeader));

        for (Map.Entry<K, V> entry : map.entrySet()) {
            tableData.add(Arrays.asList(entry.getKey().toString(), entry.getValue().toString()));
        }

        printTable(tableData);
    }

    // テーブル形式の出力
    private static void printTable(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) return;

        int[] colWidths = new int[rows.get(0).size()];
        for (List<String> row : rows) {
            for (int col = 0; col < row.size(); col++) {
                colWidths[col] = Math.max(colWidths[col], row.get(col).length());
            }
        }

        for (List<String> row : rows) {
            StringBuilder formattedRow = new StringBuilder();
            for (int col = 0; col < row.size(); col++) {
                formattedRow.append(String.format(" %-" + colWidths[col] + "s |", row.get(col)));
            }
            System.out.println(formattedRow.toString());
        }
    }
}
