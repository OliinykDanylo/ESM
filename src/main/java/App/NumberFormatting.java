package App;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class NumberFormatting extends DefaultTableCellRenderer {
    private final DecimalFormat oneDecimalFormat;
    private final DecimalFormat threeDecimalFormat;

    public NumberFormatting() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' '); // a thousand separator
        symbols.setDecimalSeparator(','); // a decimal separator

        oneDecimalFormat = new DecimalFormat("#,##0.0", symbols);  // "1 234,5"
        threeDecimalFormat = new DecimalFormat("#,##0.000", symbols); // "1 234,567"
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (value != null) {
            try {
                double number = Double.parseDouble(value.toString());

                if (isRowBelowOrAtKI(table, row)) {
                    // to 1 decimal place
                    String formatted = oneDecimalFormat.format(number);

                    // for numbers starting with "0,"
                    if (formatted.startsWith("0,")) {
                        formatted = threeDecimalFormat.format(number); // 3 decimals

                        // to remove unnecessary zeros
                        if (formatted.endsWith("0")) {
                            formatted = formatted.replaceAll(",?0+$", "");
                        }
                    } else if (formatted.endsWith(",0")) {
                        // to remove ",0" if the number is whole
                        formatted = formatted.substring(0, formatted.length() - 2);
                    }
                    value = formatted;
                } else {
                    // to replace '.' with ',' for rows above "KI"
                    value = value.toString().replace('.', ',');
                }
            } catch (NumberFormatException ignored) {
                // nothing for non-numeric values
            }
        }

        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    // method to check if the current row is "KI" or below
    private boolean isRowBelowOrAtKI(JTable table, int row) {
        for (int i = 0; i <= row; i++) {
            Object valueAtFirstColumn = table.getValueAt(i, 0);
            if (valueAtFirstColumn != null && "KI".equals(valueAtFirstColumn.toString())) {
                return true; // row is at or below "KI"
            }
        }
        return false; // above "KI"
    }
}