package app.revanced.extension.dcinside.patches.management.floatingButton.dcBanList;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses a CSV export while retaining every column by its header name. */
final class DcBanListCsvParser {
    static final String IDENTIFIER_COLUMN = "식별코드";
    static final String CONTENT_COLUMN = "게시글 / 댓글";

    private DcBanListCsvParser() {
    }

    static CsvTable parse(String csv) throws JSONException {
        List<List<String>> records = parseRecords(csv);
        if (records.isEmpty()) return new CsvTable(Collections.emptyList(), Collections.emptyList());

        List<String> headers = new ArrayList<>(records.get(0));
        if (!headers.isEmpty() && headers.get(0).startsWith("\uFEFF")) {
            headers.set(0, headers.get(0).substring(1));
        }

        List<JSONObject> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> values = records.get(rowIndex);
            if (isEmptyRow(values)) continue;

            JSONObject row = new JSONObject();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String header = headers.get(columnIndex).trim();
                if (header.isEmpty()) header = "column_" + (columnIndex + 1);
                row.put(header, columnIndex < values.size() ? values.get(columnIndex) : "");
            }
            rows.add(row);
        }
        return new CsvTable(headers, rows);
    }

    static List<JSONObject> findExactIdentifierMatches(CsvTable table, String identifier) {
        List<JSONObject> matches = new ArrayList<>();
        String normalizedTarget = normalizeIdentifier(identifier);
        for (JSONObject row : table.rows) {
            if (normalizedTarget.equals(normalizeIdentifier(row.optString(IDENTIFIER_COLUMN, "")))) {
                matches.add(row);
            }
        }
        return matches;
    }

    static String normalizeIdentifier(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.endsWith("+ IP")) {
            normalized = normalized.substring(0, normalized.length() - "+ IP".length());
        }
        return normalized.trim();
    }

    private static boolean isEmptyRow(List<String> values) {
        for (String value : values) {
            if (!value.trim().isEmpty()) return false;
        }
        return true;
    }

    private static List<List<String>> parseRecords(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < csv.length(); index++) {
            char current = csv.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    currentField.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++;
                currentRecord.add(currentField.toString());
                records.add(currentRecord);
                currentRecord = new ArrayList<>();
                currentField.setLength(0);
            } else {
                currentField.append(current);
            }
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString());
            records.add(currentRecord);
        }
        return records;
    }

    static final class CsvTable {
        final List<String> headers;
        final List<JSONObject> rows;

        CsvTable(List<String> headers, List<JSONObject> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }
}
