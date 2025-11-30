// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package epox.swing;

public class TableSorter {
    TableModelSortable tableModel;
    private int lastSortedColumn;
    private int lastRowCount;
    private int primarySortColumn;
    private int secondarySortColumn;
    private boolean isDescending;
    private boolean shouldToggleDirection;

    public TableSorter(TableModelSortable model) {
        tableModel = model;
        reset();
    }

    public void reset() {
        lastSortedColumn = primarySortColumn = secondarySortColumn = 0;
        lastRowCount = -1;
        isDescending = shouldToggleDirection = false;
    }

    public void sort(int[] array, int column, boolean refresh) {
        if (array.length < 1) {
            return;
        }
        int rowCount = tableModel.getRowCount();
        if (refresh) {
            performSort(array, 0, rowCount - 1);
            isDescending = shouldToggleDirection;
            if (isDescending) {
                invertArray(array);
            }
        } else {
            if (primarySortColumn != column) {
                secondarySortColumn = primarySortColumn;
            }
            primarySortColumn = column;

            if (column == lastSortedColumn || lastRowCount < 0) {
                shouldToggleDirection = !shouldToggleDirection;
            }

            if (column != lastSortedColumn || rowCount != lastRowCount) {
                performSort(array, 0, rowCount - 1);
                isDescending = false;
            }
            if (shouldToggleDirection || isDescending) {
                invertArray(array);
                isDescending = true;
            }

            lastSortedColumn = column;
            lastRowCount = rowCount;
        }
    }

    private void invertArray(int[] array) {
        int lastIndex = tableModel.getRowCount() - 1;
        for (int i = 0; i <= lastIndex / 2; i++) {
            swapElements(array, i, lastIndex - i);
        }
    }

    public void performSort(int[] array, int low, int high) {
        quickSort(array, low, high);
        insertionSort(array, low, high);
    }

    private void quickSort(int[] array, int left, int right) {
        final int insertionSortThreshold = 4;
        int leftIndex;
        int rightIndex;
        int pivotIndex;

        if ((right - left) > insertionSortThreshold) {
            leftIndex = (right + left) / 2;
            if (compareRows(left, leftIndex) > 0) {
                swapElements(array, left, leftIndex);
            }
            if (compareRows(left, right) > 0) {
                swapElements(array, left, right);
            }
            if (compareRows(leftIndex, right) > 0) {
                swapElements(array, leftIndex, right);
            }

            rightIndex = right - 1;
            swapElements(array, leftIndex, rightIndex);
            leftIndex = left;
            pivotIndex = rightIndex;
            for (; ; ) {
                do {
                    leftIndex++;
                } while (compareRows(leftIndex, pivotIndex) < 0);
                do {
                    rightIndex--;
                } while (rightIndex > 0 && compareRows(pivotIndex, rightIndex) < 0);
                if (rightIndex < leftIndex) {
                    break;
                }
                swapElements(array, leftIndex, rightIndex);
            }
            swapElements(array, leftIndex, right - 1);
            quickSort(array, left, rightIndex);
            quickSort(array, leftIndex + 1, right);
        }
    }

    private void swapElements(int[] array, int firstIndex, int secondIndex) {
        int temp = array[firstIndex];
        array[firstIndex] = array[secondIndex];
        array[secondIndex] = temp;
    }

    private void insertionSort(int[] array, int low, int high) {
        int outerIndex;
        int innerIndex;
        for (outerIndex = low; outerIndex <= high; outerIndex++) {
            for (innerIndex = outerIndex;
                    innerIndex > low && compareRows(innerIndex - 1, innerIndex) > 0;
                    innerIndex--) {
                swapElements(array, innerIndex, innerIndex - 1);
            }
        }
    }

    public int compareRows(int rowIndex1, int rowIndex2) {
        int result = compareByColumn(primarySortColumn, rowIndex1, rowIndex2);
        if (result == 0) {
            return compareByColumn(secondarySortColumn, rowIndex1, rowIndex2);
        }
        return result;
    }

    public int compareByColumn(int column, int row1, int row2) {
        Object value1 = tableModel.getValueAt(row1, column);
        Object value2 = tableModel.getValueAt(row2, column);
        Class<?> columnType = tableModel.getColumnClass(column);
        if (columnType.getSuperclass() == Number.class) {
            return (int) (((Number) value1).floatValue() - ((Number) value2).floatValue());
        } else if (columnType == String.class) {
            return ((String) value1).compareToIgnoreCase((String) value2);
        } else {
            return (value1.toString()).compareToIgnoreCase(value2.toString());
        }
    }

    public int compareNumbers(Number first, Number second) {
        double firstValue = first.doubleValue();
        double secondValue = second.doubleValue();
        if (firstValue < secondValue) {
            return -1;
        } else if (firstValue > secondValue) {
            return 1;
        } else {
            return 0;
        }
    }
}
