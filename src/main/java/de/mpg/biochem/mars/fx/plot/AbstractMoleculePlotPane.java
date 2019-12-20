/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.plot;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROWS_V;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SQUARE_ALT;

import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.Zoomer;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionPlugin;
import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionPlugin;
//import de.mpg.biochem.mars.fx.plot.tools.MarsPositionSelectionTool;
//import de.mpg.biochem.mars.fx.plot.tools.MarsRegionSelectionTool;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;

public abstract class AbstractMoleculePlotPane<M extends Molecule, S extends SubPlot> extends AbstractPlotPane implements MoleculeSubPane {
	
	protected M molecule;
	
	protected BooleanProperty regionSelected;
	protected BooleanProperty positionSelected;
	
	protected MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive;
	
	public AbstractMoleculePlotPane() {
		super();

		addChart();
		
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
		getNode().addEventHandler(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   	if (e.getEventType().getName().equals("UPDATE_PLOT_AREA")) {
				   		e.consume();
				   		for (SubPlot subPlot : charts) 
							subPlot.fireEvent(new UpdatePlotAreaEvent());
				   	}
			   };
		});
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, new EventHandler<MoleculeArchiveEvent>() {
			@Override
			public void handle(MoleculeArchiveEvent e) {
				if (e.getEventType().getName().equals("INITIALIZE_MOLECULE_ARCHIVE")) {
					archive = e.getArchive();
			   		e.consume();
			   	}
			} 
        });
	}
	
	@Override
	protected void buildTools() {
		super.buildTools();
		
		regionSelected = new SimpleBooleanProperty();
		Action regionSelectionCursor = new Action("region", "Shortcut+R", SQUARE_ALT, 
				e -> setTool(regionSelected, () -> {
					MarsRegionSelectionPlugin tool = new MarsRegionSelectionPlugin(AxisMode.X);
					return tool;
				}, Cursor.DEFAULT), 
				null, regionSelected);
		addTool(regionSelectionCursor);
		
		positionSelected = new SimpleBooleanProperty();
		Action positionSelectionCursor = new Action("position", "Shortcut+P", de.jensd.fx.glyphs.octicons.OctIcon.MILESTONE, 
				e -> setTool(positionSelected, () -> {
					MarsPositionSelectionPlugin tool = new MarsPositionSelectionPlugin();
					return tool;
				}, Cursor.DEFAULT),
				null, positionSelected);
		addTool(positionSelectionCursor);
		
	}
	
	public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive() {
		return archive;
	}
	
	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public void handle(MoleculeEvent event) {
		event.invokeHandler(this);
		event.consume();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.molecule = (M) molecule;
		chartsPane.getChildren().clear();
		
		for (SubPlot subPlot : charts) {
			subPlot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
			chartsPane.getChildren().add(subPlot.getNode());
		}
		resetXYZoom();
	}

	@Override
	public void addChart() {
		SubPlot subplot = createSubPlot();
		if (molecule != null)
			subplot.fireEvent(new MoleculeSelectionChangedEvent(molecule));
		addChart(subplot);
	}
	
	public abstract S createSubPlot();
}
