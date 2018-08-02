package de.mpg.biochem.sdmm.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.ops.Initializable;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>SDMM Plugins>Table Utils>Results Sorter")
public class ResultsTableSorter extends DynamicCommand implements Initializable {
	
	@Parameter
    private ResultsTableService resultsTableService;
	
    @Parameter
    private UIService uiService;
    
    @Parameter(label="Table", choices = {"a", "b", "c"})
	private String tableName;
	
    @Parameter(label="Column", choices = {"a", "b", "c"})
	private String column;
    
	@Parameter(label="Group Column", choices = {"no group"})
	private String group;

	@Parameter(label="ascending")
	private boolean ascending;
	
	private SDMMResultsTable table;
	
	// -- Initializable methods --

	@Override
	public void initialize() {
        final MutableModuleItem<String> tableItems = getInfo().getMutableInput("tableName", String.class);
		tableItems.setChoices(resultsTableService.getTableNames());
		
		final MutableModuleItem<String> columnItems = getInfo().getMutableInput("column", String.class);
		columnItems.setChoices(resultsTableService.getColumnNames());
		
		//headings2[0] = "no grouping"; Needs to be added...
		
		final MutableModuleItem<String> groupItems = getInfo().getMutableInput("group", String.class);
		
		ArrayList<String> colNames = resultsTableService.getColumnNames();
		colNames.add(0, "no group");
		groupItems.setChoices(colNames);
	}
	
	// -- Runnable methods --
	
	@Override
	public void run() {
		table = resultsTableService.getResultsTable(tableName);
		
		if (group.equals("no group")) {
			sort(table, ascending, column);
		} else {
			sort(table, ascending, group, column);
		}
		
		uiService.show(tableName, table);
	}
	
	public static void sort(SDMMResultsTable table, final boolean ascending, String... columns) {
		
		ResultsTableList list = new ResultsTableList(table);
		
		final int[] columnIndexes = new int[columns.length];
		
		for (int i = 0; i < columns.length; i++)
			columnIndexes[i] = table.getColumnIndex(columns[i]);
		
		Collections.sort(list, new Comparator<double[]>() {
			
			@Override
			public int compare(double[] o1, double[] o2) {				
				for (int columnIndex: columnIndexes) {
					int groupDifference = Double.compare(o1[columnIndex], o2[columnIndex]); 
				
					if (groupDifference != 0)
						return ascending ? groupDifference : -groupDifference;
				}
				return 0;
			}
			
		});
	}
}
