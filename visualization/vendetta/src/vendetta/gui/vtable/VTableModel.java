/* Copyright (c) 2008 Uppsala Universitet.
 * All rights reserved.
 * 
 * This file is part of Vendetta.
 *
 * Vendetta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Vendetta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Vendetta.  If not, see <http://www.gnu.org/licenses/>.
 */

package vendetta.gui.vtable;

import vendetta.MonitorNode;

public class VTableModel extends javax.swing.table.AbstractTableModel {
	private String[] columnNames = { "none" };
	private int nr = 0;
	private MonitorNode[] nodes;

	public VTableModel(String[] cNames, MonitorNode[] nodes) {
		super();
		this.nodes = nodes;
		columnNames = new String[cNames.length];
		for (int i = 0; i < cNames.length; i++)
			columnNames[i] = cNames[i];
	}

	public int getRowCount() {
		return nr;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (row >= nr)
			return null;
		return nodes[row].getTableValue(col);
	}

	public MonitorNode getNodeAt(int row) {
		return nodes[row];
	}

	public int getNodeRow(MonitorNode node) {
		for (int i = 0; i < nr; i++)
			if (nodes[i] == node)
				return i;
		return -1;
	}

	public Class<?> getColumnClass(int c) {
		if (nr == 0)
			return null;
		return getValueAt(0, c).getClass();
	}

	/** Called when a monitor node is created */
	public void addMonitorNode(MonitorNode newNode) {
		nr++;
		fireTableRowsInserted(nr - 1, nr - 1);
	}

	/** Called when a monitor node is removed */
	public void removeMonitorNode(MonitorNode node) {
		nr--;
		//fireTableDataChanged();
		repaintRow(nr);
	}

	public void repaintRow(int row) {
		fireTableRowsUpdated(row, row);
	}

}
