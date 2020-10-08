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
package de.mpg.biochem.mars.fx.molecule;

import static java.util.stream.Collectors.toList;

import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.jfoenix.controls.JFXTabPane;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.image.Image;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.image.ImageView;
import javafx.scene.control.ButtonType;

import javafx.concurrent.Task;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.controlsfx.control.MaskerPane;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.mpg.biochem.mars.fx.dialogs.PropertySelectionDialog;
import de.mpg.biochem.mars.fx.dialogs.SegmentTableSelectionDialog;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveLockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockEvent;
import de.mpg.biochem.mars.fx.event.MoleculeTagsChangedEvent;
import de.mpg.biochem.mars.fx.event.RefreshMetadataEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculeEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculePropertiesEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MarsBdvFrame;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.util.*;
import de.mpg.biochem.mars.metadata.MarsMetadata;
import de.mpg.biochem.mars.molecule.*;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.util.MarsUtil;

import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollBar;

public abstract class AbstractMoleculeArchiveFxFrame<I extends MarsMetadataTab<? extends MetadataSubPane, ? extends MetadataSubPane>, 
		M extends MoleculesTab<? extends MoleculeSubPane, ? extends MoleculeSubPane>> extends AbstractJsonConvertibleRecord implements MoleculeArchiveWindow {
	
	@Parameter
    protected MoleculeArchiveService moleculeArchiveService;
	
    @Parameter
    protected UIService uiService;
    
    @Parameter
    protected Context context;
    
    @Parameter
    protected LogService logService;

	protected MoleculeArchive<Molecule,MarsMetadata,MoleculeArchiveProperties> archive;
	
	protected JFrame frame;
	protected String title;
	protected JFXPanel fxPanel;

	protected StackPane maskerStackPane;
	protected MaskerPane masker;
	protected MarsAnimation marsSpinning;
	
	protected BorderPane borderPane;
    protected JFXTabPane tabsContainer;
    
    protected TextArea lockLogArea;
    
	protected MenuBar menuBar;
	
	protected DashboardTab dashboardTab;
    protected CommentsTab commentsTab; 
    
    protected I imageMetadataTab;
    protected M moleculesTab;
    protected SettingsTab settingsTab;
    
    protected boolean windowStateLoaded = false;
    
    protected static JsonFactory jfactory;
	
    protected Set<MoleculeArchiveTab> tabSet;
    
    protected MarsBdvFrame<?> marsBdvFrame;

    protected double tabWidth = 50.0;
    
    protected Menu fileMenu, toolsMenu;
    
    protected final AtomicBoolean archiveLocked = new AtomicBoolean(false);

	public AbstractMoleculeArchiveFxFrame(MoleculeArchive<Molecule,MarsMetadata,MoleculeArchiveProperties> archive, final Context context) {
		super();
		context.inject(this);

		this.title = archive.getName();
		this.archive = archive;
		
		archive.setWindow(this);
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		frame = new JFrame(title);
		
		//frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent windowEvent) {
				SwingUtilities.invokeLater(() -> {
					close();
				});
			}
		});
		
		this.fxPanel = new JFXPanel();
		frame.add(this.fxPanel);
		
		if (!uiService.isHeadless())
			WindowManager.addWindow(frame);

		// The call to runLater() avoid a mix between JavaFX thread and Swing thread.
		// Allows multiple runLaters in the same session...
		// Suggested here - https://stackoverflow.com/questions/29302837/javafx-platform-runlater-never-running
		Platform.setImplicitExit(false);
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
			}
		});

	}

	public void initFX(JFXPanel fxPanel) {	
		Scene scene = buildScene();
		this.fxPanel.setScene(scene);
	}
	
	protected Scene buildScene() {
		borderPane = new BorderPane();
    	
		lockLogArea = new TextArea();
		lockLogArea.getStyleClass().add("log-text-area");
		lockLogArea.setStyle("-fx-font-family: 'Courier'; -fx-font-size: 10pt");
		lockLogArea.setWrapText(true);
		lockLogArea.setVisible(false);
		
    	masker = new MaskerPane();
    	masker.setVisible(false);
    	
    	marsSpinning = new MarsAnimation();
    	
    	masker.setProgressNode(marsSpinning);
    	
    	maskerStackPane = new StackPane();
    	maskerStackPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
    	maskerStackPane.getChildren().add(borderPane);
    	maskerStackPane.getChildren().add(lockLogArea);
    	maskerStackPane.getChildren().add(masker);
    	
    	tabsContainer = new JFXTabPane();
		tabsContainer.prefHeight(128.0);
		tabsContainer.prefWidth(308.0);
		tabsContainer.setSide(Side.LEFT);
		tabsContainer.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabsContainer.setTabMinWidth(tabWidth);
        tabsContainer.setTabMaxWidth(tabWidth);
        tabsContainer.setTabMinHeight(tabWidth);
        tabsContainer.setTabMaxHeight(tabWidth);
        tabsContainer.setRotateGraphic(true);
        
        if (jfactory == null)
        	jfactory = new JsonFactory();

        buildMenuBar();
        buildTabs();
        
        //Now add tabs to container
        tabSet.forEach(maTab -> tabsContainer.getTabs().add(maTab.getTab()));
        
        fireEvent(new InitializeMoleculeArchiveEvent(archive));
        
        borderPane.setCenter(tabsContainer);
        Scene scene = new Scene(maskerStackPane);
        
        try {
			loadState();
			
			if (!windowStateLoaded)
				SwingUtilities.invokeLater(() -> { 
	    			frame.setSize(800, 600);
	    			frame.setVisible(true);
				});
		} catch (IOException e) {
			logService.warn("A problem was encountered when loading the cfg file " 
					+ archive.getFile().getAbsolutePath() + ".cfg" + " containing the mars-fx display settings. "
					+ "Please check the file to make sure the syntax is correct."
					+ "Aborting and opening with the default settings.");
		}
        
        updateAccelerators();

        return scene;
	}
	
	protected void buildTabs() {
		tabSet = new LinkedHashSet<MoleculeArchiveTab>();
		
		dashboardTab = new DashboardTab(context);
        dashboardTab.getTab().setStyle("-fx-background-color: -fx-focus-color;");
        tabSet.add(dashboardTab);

        imageMetadataTab = createImageMetadataTab(context);
        tabSet.add(imageMetadataTab);
        
        moleculesTab = createMoleculesTab(context);
        tabSet.add(moleculesTab);
        
        commentsTab = new CommentsTab(context);
        tabSet.add(commentsTab);
        
        settingsTab = new SettingsTab(context);
        tabSet.add(settingsTab);

        //fire save events for tabs as they are left and update events for new tabs
        tabsContainer.getSelectionModel().selectedItemProperty().addListener(
    		new ChangeListener<Tab>() {
    			@Override
    			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
    				tabSet.stream().filter(maTab -> newValue == maTab.getTab()).findFirst().ifPresent(maTab -> updateMenus(maTab.getMenus()));
    					
    				if (oldValue == commentsTab.getTab()) {
    					commentsTab.saveComments();
    				} else if (oldValue == imageMetadataTab.getTab()) {
    					imageMetadataTab.saveCurrentRecord();
    				} else if (oldValue == moleculesTab.getTab()) {
    					moleculesTab.saveCurrentRecord();
    				} else if (oldValue == settingsTab.getTab()) {
    					settingsTab.save();
    					updateAccelerators();
    				}
    				
	    			if (newValue == imageMetadataTab.getTab()) {
						imageMetadataTab.fireEvent(new RefreshMetadataEvent());
					} else if (newValue == moleculesTab.getTab()) {
						moleculesTab.fireEvent(new RefreshMoleculeEvent());
					} 
    			}
    		});
    }
	
	protected void updateAccelerators() {
		//Update global accelerators...
		for (HotKeyEntry hotKeyEntry : settingsTab.getHotKeyList()) {
			Runnable rn = ()-> {
				if (tabsContainer.getSelectionModel().getSelectedItem() == moleculesTab.getTab()) {
           	 		moleculesTab.getSelectedMolecule().addTag(hotKeyEntry.getTag());
           	 		moleculesTab.fireEvent(new RefreshMoleculePropertiesEvent());
           	 		moleculesTab.fireEvent(new MoleculeTagsChangedEvent(moleculesTab.getSelectedMolecule()));
				}
            };
            	getNode().getScene().getAccelerators().put(hotKeyEntry.getShortcut(), rn);
		}
	}
	
	protected void buildMenuBar() {
		//Build file menu
		Action fileSaveAction = new Action("Save", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveCopyAction = new Action("Save a Copy...", null, null, e -> saveCopy());
		Action fileSaveJsonCopyAction = new Action("Save a Json Copy...", null, null, e -> saveJsonCopy());
		Action fileSaveVirtualStoreAction = new Action("Save a Virtual Store Copy...", null, null, e -> saveVirtualStoreCopy());
		Action fileSaveJsonVirtualStoreAction = new Action("Save a Json Virtual Store Copy...", null, null, e -> saveJsonVirtualStoreCopy());
		Action fileCloseAction = new Action("Close", null, null, e -> close());
		
		fileMenu = ActionUtils.createMenu("File",
				fileSaveAction,
				fileSaveCopyAction,
				fileSaveJsonCopyAction,
				fileSaveVirtualStoreAction,
				fileSaveJsonVirtualStoreAction,
				null,
				fileCloseAction);
		
		//Build tools menu
		Action showVideoAction = new Action("Show Video", null, null,
				e -> {
			        SwingUtilities.invokeLater(new Runnable() {
			            @Override
			            public void run() {
			            	GenericDialog dialog = new GenericDialog("Mars Bdv view");
			     			dialog.addStringField("x_parameter", "roi_x", 25);
			     			dialog.addStringField("y_parameter", "roi_y", 25);
			          		dialog.showDialog();
			          		
			          		if (dialog.wasCanceled())
			          			return;
			          		
			          		String xParameter = dialog.getNextString();
			          		String yParameter = dialog.getNextString();
			          		
			            	if (archive != null && moleculesTab.getSelectedMolecule() != null) {
			            		marsBdvFrame = new MarsBdvFrame(archive, moleculesTab.getSelectedMolecule(), xParameter, yParameter);
			            		marsBdvFrame.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
			            		    @Override
			            		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
			            		        marsBdvFrame = null;
			            		    }
			            		});
			            		moleculesTab.setMarsBdvFrame(marsBdvFrame);
			            	}
			            }
			        });
				}); 
		
		Action exportVideoAction = new Action("Export Video", null, null,
				e -> {
					if (marsBdvFrame == null) {
						showErrorMessage("There is no BigDataView available for export. "
								+ "Please configure the BDV settings in the metadata record, "
								+ "run \"Show Video\" and then try again.");
						return;
					}
					
			        SwingUtilities.invokeLater(new Runnable() {
			            @Override
			            public void run() {
			            	if (marsBdvFrame == null)
			            		return;
			            		
			            	GenericDialog dialog = new GenericDialog("Export BDV to ImageJ");
			     			dialog.addNumericField("x0", -10, 2);
			     			dialog.addNumericField("y0", -10, 2);
			     			dialog.addNumericField("width", 20, 2);
			     			dialog.addNumericField("height", 60, 2);
			          		dialog.showDialog();
			          		
			          		if (dialog.wasCanceled())
			          			return;
			          		
			          		int x0 = (int)dialog.getNextNumber();
			          		int y0 = (int)dialog.getNextNumber();
			          		int width = (int)dialog.getNextNumber();
			          		int height = (int)dialog.getNextNumber();
			          		
			          		ImagePlus ip = marsBdvFrame.exportView(x0, y0, width, height);
			          		
			          		//Now Show it!
			          		ip.show();
			            }
			        });
				}); 
		
		Action deleteMoleculesAction = new Action("Delete Molecules", null, null, e -> deleteMolecules());
		Action deleteMoleculeTagsAction = new Action("Delete Molecule Tags", null, null, e -> deleteMoleculeTags());
		Action deleteMoleculeParametersAction = new Action("Delete Molecule Parameters", null, null, e -> deleteMoleculeParameters());
		Action deleteMoleculeRegionsAction = new Action("Delete Molecule Regions", null, null, e -> deleteMoleculeRegions());
		Action deleteMoleculePositionsAction = new Action("Delete Molecule Positions", null, null, e -> deleteMoleculePositions());
		Action deleteSegmentTablesAction = new Action("Delete Segment Tables", null, null, e -> deleteSegmentTables());
		
		Action mergeMoleculesAction = new Action("Merge Molecules", null, null, e -> mergeMolecules());
		
		Action rebuildIndexesAction = new Action("Rebuild Indexes", null, null,
			e -> {
				runTask(() -> {
    	            	try {
    						archive.rebuildIndexes();
    					} catch (IOException e1) {
    						e1.printStackTrace();
    					}
    	            }, "Rebuilding Indexes...");					
			});
			
		toolsMenu = ActionUtils.createMenu("Tools",
					deleteMoleculesAction,
					deleteMoleculeTagsAction,
					deleteMoleculeParametersAction,
					deleteMoleculeRegionsAction,
					deleteMoleculePositionsAction,
					deleteSegmentTablesAction,
					mergeMoleculesAction,
					null,
					showVideoAction,
					exportVideoAction,
					null,
					rebuildIndexesAction);
		
		menuBar = new MenuBar(fileMenu, toolsMenu);
		borderPane.setTop(menuBar);
	}
	
	protected void deleteMolecules() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getTagSet(), "Delete Molecules", "Delete molecules with tags:", "Delete molecules with no tags");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				ArrayList<String> deleteUIDs = (ArrayList<String>)archive.getMoleculeUIDs().parallelStream().filter(UID -> {
	        	 	if (result.removeAll() && archive.get(UID).getTags().size() == 0) {
	        	 		return true;
	        	 	}
	        	 
	 				boolean hasTag = false;
	 				List<String> tagList = result.getList();
	 				for (int i=0; i<tagList.size(); i++) {
	 		        	for (String tag : archive.get(UID).getTags()) {
	 		        		if (tagList.get(i).equals(tag)) {
	 		        			hasTag = true;
	 		        		}
	 		        	}
	 		        }
	 				return hasTag;
	 			}).collect(toList());
	             
				deleteUIDs.parallelStream().forEach(UID -> archive.remove(UID));
			}, "Deleting Molecules...");
		});
	}
	
	protected void deleteMoleculeTags() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getTagSet(), "Delete Molecule Tags", "Delete molecule tags:", "Delete all tags");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> tagList = result.getList();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	if (result.removeAll()) {
	            	 		molecule.removeAllTags();
	            	 	} else {
	     		        	for (int i=0;i<tagList.size();i++) {
	     		        		molecule.removeTag(tagList.get(i));
	     		        	}
	            	 	}
	            	 	archive.put(molecule);
	     			});
			}, "Deleting Molecule Tags...");
		});
	}
	
	protected void deleteMoleculeParameters() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getParameterSet(), "Delete Molecule Parameters", "Delete molecule parameters:", "Delete all parameters");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> parameterList = result.getList();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	if (result.removeAll()) {
	            	 		molecule.removeAllParameters();
	            	 	} else {
	            	 		for (int i=0;i<parameterList.size();i++) {
	     		        		molecule.removeParameter(parameterList.get(i));
	     		        	}
	            	 	}
	            	 	archive.put(molecule);
	     			});
			}, "Deleting Molecule Parameters...");
		});
	}
	
	protected void deleteMoleculeRegions() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getRegionSet(), "Delete Molecule Regions", "Delete molecule regions:", "Delete all regions");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> regionList = result.getList();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	if (result.removeAll()) {
	            	 		molecule.removeAllRegions();
	            	 	} else {
	            	 		for (int i=0;i<regionList.size();i++) {
	     		        		molecule.removeRegion(regionList.get(i));
	     		        	}
	            	 	}
	            	 	archive.put(molecule);
	     			});
			}, "Deleting Molecule Regions...");
		});
	}
	
	protected void deleteMoleculePositions() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getPositionSet(), "Delete Molecule Positions", "Delete molecule positions:", "Delete all positions");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				List<String> regionList = result.getList();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	if (result.removeAll()) {
	            	 		molecule.removeAllPositions();
	            	 	} else {
	            	 		for (int i=0;i<regionList.size();i++) {
	     		        		molecule.removePosition(regionList.get(i));
	     		        	}
	            	 	}
	            	 	archive.put(molecule);
	     			});
			}, "Deleting Molecule Positions...");
		});
	}
	
	protected void deleteSegmentTables() {
		SegmentTableSelectionDialog dialog = new SegmentTableSelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getSegmentsTableNames(), "Delete segments table");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				ArrayList<String> segmentTableName = result.getSegmentTableName();
	            archive.getMoleculeUIDs().parallelStream().forEach(UID -> {
	            		Molecule molecule = archive.get(UID);
	            	 	molecule.removeSegmentsTable(segmentTableName);
	            	 	archive.put(molecule);
	     			});
	            
	            archive.properties().getSegmentsTableNames().remove(segmentTableName);
			}, "Deleting Segments Tables...");
		});
	}
	
	protected void mergeMolecules() {
		PropertySelectionDialog dialog = new PropertySelectionDialog(getNode().getScene().getWindow(), 
				archive.properties().getTagSet(), "Merge Molecules", "Merge molecules with tag:");

		dialog.showAndWait().ifPresent(result -> {
			runTask(() -> {
				if (result.getList().size() == 0)
					return;
				
				String tag = result.getList().get(0);
	     		 
	     		ArrayList<String> mergeUIDs = (ArrayList<String>)archive.getMoleculeUIDs().stream().filter(UID -> archive.moleculeHasTag(UID, tag)).collect(toList());
             
	     		if (mergeUIDs.size() < 2) 
	     			return;
	     		
	     		String mergeNote = "Merged " + mergeUIDs.size() + " molecules \n";
	     		
	     		MarsTable mergedDataTable = archive.get(mergeUIDs.get(0)).getTable();
	     		
	     		HashSet<Double> tNumbers = new HashSet<Double>();
	     		
	     		//First add all current T to set
	     		for (int row=0;row<mergedDataTable.getRowCount();row++) {
            		tNumbers.add(mergedDataTable.getValue("T", row));
            	}
	     		
	     		mergeNote += mergeUIDs.get(0).substring(0, 5) + " : Ts " + mergedDataTable.getValue("T", 0) + " " + mergedDataTable.getValue("T", mergedDataTable.getRowCount()-1) + "\n";
	     		
	            for (int i = 1; i < mergeUIDs.size() ; i++) {
	            	MarsTable nextDataTable = archive.get(mergeUIDs.get(i)).getTable();
	            	
	            	for (int row=0;row<nextDataTable.getRowCount();row++) {
	            		if (!tNumbers.contains(nextDataTable.getValue("T", row))) {
	            			mergedDataTable.appendRow();
	            			int mergeLastRow = mergedDataTable.getRowCount() - 1;
	            			
	            			for (int col=0;col<mergedDataTable.getColumnCount();col++) {
	            				String column = mergedDataTable.getColumnHeader(col);
	    	            		mergedDataTable.setValue(column, mergeLastRow, nextDataTable.getValue(column, row));
	    	            	}
	            			
	            			tNumbers.add(nextDataTable.getValue("T", row));
	            		}
	            	}
	            	mergeNote += mergeUIDs.get(i).substring(0, 5) + " : Ts " + nextDataTable.getValue("T", 0) + " " + nextDataTable.getValue("T", nextDataTable.getRowCount()-1) + "\n";
	            	
	            	archive.remove(mergeUIDs.get(i));
	            }
	            
	            //sort by T
	            mergedDataTable.sort(true, "T");
	            
	            String previousNotes = "";
	            if (archive.get(mergeUIDs.get(0)).getNotes() != null)
	            	previousNotes = archive.get(mergeUIDs.get(0)).getNotes() + "\n";
	            
	            archive.get(mergeUIDs.get(0)).setNotes(previousNotes + mergeNote);
			}, "Merging Molecules...");
		});
	}
	
	protected void runTask(Runnable process, String message) {
		masker.setText(message);
		//masker.setProgress(-1);
		marsSpinning.play();
		marsSpinning.setProgress(-1);
		masker.setVisible(true);
		lockLogArea.setVisible(true);
    	fireEvent(new MoleculeArchiveLockEvent(archive));
		Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
            	process.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> { 
        	fireEvent(new MoleculeArchiveUnlockEvent(archive));
			masker.setVisible(false);
			lockLogArea.setVisible(false);
        });

        new Thread(task).start();
	}
	
	public MoleculeArchive<Molecule, MarsMetadata, MoleculeArchiveProperties> getArchive() {
		return archive;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public String getTitle() {
		return title;
	}

	public void updateMenus(ArrayList<Menu> menus) {	
    	while (menuBar.getMenus().size() > 1)
    		menuBar.getMenus().remove(1);
    	if(menus != null && menus.size() > 0) {
    		for (Menu menu : menus)
    			menuBar.getMenus().add(menu);
    	}
    	menuBar.getMenus().add(toolsMenu);
    }

    public void save() {
	   	 try {
			 if (archive.getFile() != null) {
				 if(archive.getFile().getName().equals(archive.getName())) {
					lockFX("Saving...");
	    		    fireEvent(new MoleculeArchiveSavingEvent(archive));
		    		Task<Void> task = new Task<Void>() {
	     	            @Override
	     	            public Void call() throws Exception {
	     	            	archive.save();	 
	     	            	saveState(archive.getFile().getAbsolutePath());
	     	                return null;
	     	            }
	     	        };
	
	     	        task.setOnSucceeded(event -> {
			           	fireEvent(new MoleculeArchiveSavedEvent(archive));
			           	unlockFX();
	     	        });
	
	     	        new Thread(task).run();
				 } else {
				    //the archive name has changed... so let's check with the user about the new name...
					saveAs(archive.getFile());
				 }
			 } else {
				saveAs(new File(archive.getName()));
			 }
	   	 } catch (IOException e1) {
			e1.printStackTrace();
		 }
	   	 settingsTab.save();
    }
    
    public void saveCopy() {
	    String fileName = archive.getName();
	    if (fileName.endsWith(".store"))
	    	fileName = fileName.substring(0, fileName.length() - 6);
	    
	    try {
			if (archive.getFile() != null) {
				saveAsCopy(new File(archive.getFile().getParentFile(), fileName));
			} else {
				saveAsCopy(new File(System.getProperty("user.home"), fileName));
			}
	    } catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    public void saveJsonCopy() {
	    String fileName = archive.getName();
	    if (fileName.endsWith(".store"))
	    	fileName = fileName.substring(0, fileName.length() - 6);
	    
	    try {
			if (archive.getFile() != null) {
				saveAsJsonCopy(new File(archive.getFile().getParentFile(), fileName));
			} else {
				saveAsJsonCopy(new File(System.getProperty("user.home"), fileName));
			}
	    } catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    private boolean saveAs(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File newFile = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());

		if (newFile != null) {
			lockFX("Saving...");
			fireEvent(new MoleculeArchiveSavingEvent(archive));

			Task<Void> task = new Task<Void>() {
 	            @Override
 	            public Void call() throws Exception {
 	            	archive.saveAs(newFile);
 	            	saveState(newFile.getAbsolutePath());
 	                return null;
 	            }
 	        };

 	        task.setOnSucceeded(event -> {
	           	fireEvent(new MoleculeArchiveSavedEvent(archive));
	           	
	           	if (moleculeArchiveService.contains(archive.getName()))
					moleculeArchiveService.removeArchive(archive);
	           	
				archive.setFile(newFile);
				archive.setName(newFile.getName());
				SwingUtilities.invokeLater(() -> {
					frame.setTitle(newFile.getName());
				});

				moleculeArchiveService.addArchive(archive);
	           	
	           	unlockFX();
 	        });

 	        new Thread(task).run();
 	        
 	        return true;
		}
		return false;
	}
    
	private boolean saveAsCopy(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());

		if (file != null) {
			lockFX("Saving...");
			fireEvent(new MoleculeArchiveSavingEvent(archive));

			Task<Void> task = new Task<Void>() {
 	            @Override
 	            public Void call() throws Exception {
 	            	archive.saveAs(file);	
 	            	saveState(file.getAbsolutePath());
 	                return null;
 	            }
 	        };

 	        task.setOnSucceeded(event -> {
	           	fireEvent(new MoleculeArchiveSavedEvent(archive));
	           	unlockFX();
 	        });

 	        new Thread(task).run();
 	        
 	        return true;
		}
		return false;
	}
	
	private boolean saveAsJsonCopy(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		} else if (!saveAsFile.getAbsolutePath().endsWith(".json")) {
			saveAsFile = new File(saveAsFile.getAbsolutePath() + ".json");
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());

		if (file != null) {
			lockFX("Saving...");
			fireEvent(new MoleculeArchiveSavingEvent(archive));

			Task<Void> task = new Task<Void>() {
 	            @Override
 	            public Void call() throws Exception {
 	            	archive.saveAsJson(file);	
 	            	saveState(file.getAbsolutePath());
 	                return null;
 	            }
 	        };

 	        task.setOnSucceeded(event -> {
	           	fireEvent(new MoleculeArchiveSavedEvent(archive));
	           	unlockFX();
 	        });

 	        new Thread(task).run();
 	        
 	        return true;
		}
		return false;
	}
    
    public void saveVirtualStoreCopy() {
	 	String name = archive.getName();
	 	
	 	if (name.endsWith(".yama")) {
	 		name += ".store";
	 	} else if (!name.endsWith(".yama.store")) {
 		 	name += ".yama.store";
 		}
	 
		try {
			saveAsVirtualStore(new File(name));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    public void saveJsonVirtualStoreCopy() {
	 	String name = archive.getName();
	 	
	 	if (name.endsWith(".yama")) {
	 		name += ".store";
	 	} else if (!name.endsWith(".yama.store")) {
 		 	name += ".yama.store";
 		}
	 
		try {
			saveAsJsonVirtualStore(new File(name));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
	private void saveAsVirtualStore(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (virtualDirectory != null) {	
			lockFX("Saving Virtual Store Copy...");
			
			fireEvent(new MoleculeArchiveSavingEvent(archive));
			Task<Void> task = new Task<Void>() {
 	            @Override
 	            public Void call() throws Exception {
 	            	archive.saveAsVirtualStore(virtualDirectory);	
 	            	saveState(virtualDirectory.getAbsolutePath());
 	                return null;
 	            }
 	        };

 	        task.setOnSucceeded(event -> {
	           	fireEvent(new MoleculeArchiveSavedEvent(archive));
	           	unlockFX();
 	        });
			
 	       new Thread(task).run();
		}
	}
	
	private void saveAsJsonVirtualStore(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (virtualDirectory != null) {	
			lockFX("Saving Virtual Store Copy...");
			
			fireEvent(new MoleculeArchiveSavingEvent(archive));
			Task<Void> task = new Task<Void>() {
 	            @Override
 	            public Void call() throws Exception {
 	            	archive.saveAsJsonVirtualStore(virtualDirectory);	
 	            	saveState(virtualDirectory.getAbsolutePath());
 	                return null;
 	            }
 	        };

 	        task.setOnSucceeded(event -> {
	           	fireEvent(new MoleculeArchiveSavedEvent(archive));
	           	unlockFX();
 	        });
			
 	       new Thread(task).run();
		}
	}
	
	public Node getNode() {
		return maskerStackPane;
	}
	
	public abstract I createImageMetadataTab(final Context context);
	
	public abstract M createMoleculesTab(final Context context);
	
	public DashboardTab getDashboard() {
		return dashboardTab;
	}
	
	protected void showErrorMessage(String message) {
		Alert alert = new Alert(AlertType.ERROR);
	    alert.initModality(Modality.WINDOW_MODAL);
	    alert.initOwner(getNode().getScene().getWindow());
	    Image image1 = new Image("de/mpg/biochem/mars/fx/dialogs/RoverError.png");
	    ImageView imageView = new ImageView(image1);
	    imageView.setFitWidth(80);
	    imageView.setFitHeight(80);
	    alert.setGraphic(imageView);
	    alert.setHeaderText(null);
	    alert.setContentText(message);

	    alert.show();
	}
	
	//Lock, unlock and update event might be called by swing threads
	//so we use Platform.runLater to ensure they are executed on 
	//the javafx thread.
	
	public void lock(String message) {
		if (archiveLocked.get())
    		return;
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lockFX(message);
			}
    	});
		
		//Make sure we block the calling (swing) thread until
		//the archive has actually been locked...
		while (!archiveLocked.get()) {}
	}
	
	private void lockFX(String message) {
		masker.setText(message);
		marsSpinning.play();
		marsSpinning.setProgress(-1);
		//masker.setProgress(-1);
		masker.setVisible(true);
		lockLogArea.setVisible(true);
    	fireEvent(new MoleculeArchiveLockEvent(archive));
    	archiveLocked.set(true);
	}
	
    public void lock() {
    	if (archiveLocked.get())
    		return;
    	
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lockFX();
			}
    	});
    	
    	//Make sure we block the calling (swing) thread until
		//the archive has actually been locked...
		while (!archiveLocked.get()) {}
    }
    
    private void lockFX() {
    	//masker.setProgress(-1);
		masker.setVisible(true);
		lockLogArea.setVisible(true);
		marsSpinning.play();
		marsSpinning.setProgress(-1);
    	fireEvent(new MoleculeArchiveLockEvent(archive));
    	archiveLocked.set(true);
    }
    
    @Override
    public void updateLockMessage(String message) {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				masker.setText(message);
			}
    	});
    }
    
    @Override
    public void addLogMessage(String message) {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lockLogArea.appendText(message + "\n");
				lockLogArea.setScrollTop(Double.MAX_VALUE);
				ScrollBar scroll = (ScrollBar)lockLogArea.lookup(".scroll-bar:vertical");
				if (scroll != null)
					scroll.setDisable(true);
			}
    	});
    }
    
    @Override
    public void logln(String message) {
		log(message + "\n");
    }
    
    @Override
    public void log(String message) {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lockLogArea.appendText(message);
				lockLogArea.setScrollTop(Double.MAX_VALUE);
				ScrollBar scroll = (ScrollBar)lockLogArea.lookup(".scroll-bar:vertical");
				if (scroll != null)
					scroll.setDisable(true);
			}
    	});
    }
    
    //Not really ideal since a Task and updateProgress would be the best
    //But this is the only way for direct interaction through swing threads.
    @Override
    public void setProgress(double progress) {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (masker.isVisible()) {
					marsSpinning.setProgress(progress);
				}
			}
    	});
    }
    
    @Override
    public void unlock() {
    	if (!archiveLocked.get())
    		return;
    	
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
		    	unlockFX();
			}
    	});
    	
    	//Make sure we block the calling (swing) thread until
		//the archive has actually been unlocked...
		while (archiveLocked.get()) {}
    }
    
    private void unlockFX() {
    	fireEvent(new MoleculeArchiveUnlockEvent(archive));
		masker.setVisible(false);
		lockLogArea.setVisible(false);
		marsSpinning.stop();
		archiveLocked.set(false);
		masker.setText("Please Wait...");
    }
    
    @Override
    public void update() {
    	unlock();
    }
    
    @Override
    public void close() {
    	if (moleculeArchiveService.contains(archive.getName()))
			moleculeArchiveService.removeArchive(archive);

		if (frame != null)
			WindowManager.removeWindow(frame);
		
		if (frame != null)
			frame.dispose();
    }
    
    //Creates settings input and output maps to save the current state of the program.
    @Override
	protected void createIOMaps() {
    	
		setJsonField("window", 
			jGenerator -> {
				jGenerator.writeObjectFieldStart("window");
				jGenerator.writeNumberField("x", frame.getX());
				jGenerator.writeNumberField("y", frame.getY());
				jGenerator.writeNumberField("width", frame.getWidth());
				jGenerator.writeNumberField("height", frame.getHeight());
				jGenerator.writeEndObject();
			}, 
			jParser -> {
				Rectangle rect = new Rectangle(0, 0, 800, 600);
				while (jParser.nextToken() != JsonToken.END_OBJECT) {
					if ("x".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						rect.x = jParser.getIntValue();
					}
					if ("y".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						rect.y = jParser.getIntValue();
					}
					if ("width".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						rect.width = jParser.getIntValue();
					}
					if ("height".equals(jParser.getCurrentName())) {
						jParser.nextToken();
						rect.height = jParser.getIntValue();
					}
				}
				
				windowStateLoaded = true;
				
				SwingUtilities.invokeLater(() -> { 
					frame.setBounds(rect);
					frame.setVisible(true);
				});
			});
    	
		for (MoleculeArchiveTab moleculeArchiveTab : tabSet)
			setJsonField(moleculeArchiveTab.getName(), 
				jGenerator -> {
					jGenerator.writeFieldName(moleculeArchiveTab.getName());
					moleculeArchiveTab.toJSON(jGenerator);
				}, 
				jParser -> {
					moleculeArchiveTab.fromJSON(jParser);
			 	});
		
		/*
		 * 
		 * The fields below are needed for backwards compatibility.
		 * 
		 * Please remove for a future release.
		 * 
		 */
		
		setJsonField("Window", null, 
				jParser -> {
					Rectangle rect = new Rectangle(0, 0, 800, 600);
					while (jParser.nextToken() != JsonToken.END_OBJECT) {
						if ("x".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							rect.x = jParser.getIntValue();
						}
						if ("y".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							rect.y = jParser.getIntValue();
						}
						if ("width".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							rect.width = jParser.getIntValue();
						}
						if ("height".equals(jParser.getCurrentName())) {
							jParser.nextToken();
							rect.height = jParser.getIntValue();
						}
					}
					
					windowStateLoaded = true;
					
					SwingUtilities.invokeLater(() -> { 
						frame.setBounds(rect);
						frame.setVisible(true);
					});
				});
		
		
	}
    
    protected void saveState(String path) throws IOException {
		OutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(path + ".cfg")));
		JsonGenerator jGenerator = jfactory.createGenerator(stream);
		jGenerator.useDefaultPrettyPrinter();
		toJSON(jGenerator);
		jGenerator.close();
		stream.flush();
		stream.close();
    }
    
    protected void loadState() throws IOException {
    	if (archive.getFile() == null)
    		return;
    	
    	File stateFile = new File(archive.getFile().getAbsolutePath() + ".cfg");
    	if (!stateFile.exists())
    		return;
    	
		InputStream inputStream = new BufferedInputStream(new FileInputStream(stateFile));
	    JsonParser jParser = jfactory.createParser(inputStream);
	    fromJSON(jParser);
		jParser.close();
		inputStream.close();
    }

    public void fireEvent(Event event) {
    	dashboardTab.fireEvent(event);
        imageMetadataTab.fireEvent(event);
        moleculesTab.fireEvent(event);
        commentsTab.fireEvent(event);
        settingsTab.fireEvent(event);
    }
}
