package de.mpg.biochem.mars.fx.molecule;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import de.mpg.biochem.mars.table.MarsTable;
import de.mpg.biochem.mars.table.MarsTableService;
import ij.WindowManager;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.MaskerPane;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import de.mpg.biochem.mars.fx.event.InitializeMoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MetadataSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEventHandler;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveLockedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveLockingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveSavingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockedEvent;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveUnlockingEvent;
import de.mpg.biochem.mars.fx.event.MoleculeSelectionChangedEvent;
import de.mpg.biochem.mars.fx.event.RefreshMetadataEvent;
import de.mpg.biochem.mars.fx.event.RefreshMoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.metadataTab.MetadataSubPane;
import de.mpg.biochem.mars.fx.molecule.moleculesTab.MoleculeSubPane;
import de.mpg.biochem.mars.fx.plot.event.NewMetadataRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.NewMoleculeRegionEvent;
import de.mpg.biochem.mars.fx.plot.event.PlotEvent;
import de.mpg.biochem.mars.fx.plot.event.UpdatePlotAreaEvent;
import de.mpg.biochem.mars.fx.util.*;

import de.mpg.biochem.mars.molecule.*;

public abstract class AbstractMoleculeArchiveFxFrame<I extends MarsImageMetadataTab<? extends MetadataSubPane, ? extends MetadataSubPane>, 
		M extends MoleculesTab<? extends MoleculeSubPane, ? extends MoleculeSubPane>> implements MoleculeArchiveWindow, MoleculeArchiveEventHandler {
	
	@Parameter
    protected MoleculeArchiveService moleculeArchiveService;
	
    @Parameter
    protected UIService uiService;

	protected MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive;
	
	protected JFrame frame;
	protected String title;
	protected JFXPanel fxPanel;

	protected StackPane maskerStackPane;
	protected MaskerPane masker;
	
	protected BorderPane borderPane;
	protected StackPane stackPane;
    protected JFXTabPane tabsContainer;
    
    protected boolean lockArchive = false;
    
	protected MenuBar menuBar;
	
	protected DashboardTab dashboardTab;
    protected CommentsTab commentsTab;
    protected SettingsTab settingsTab; 
    
    protected I imageMetadataTab;
    protected M moleculesTab;

    protected double tabWidth = 60.0;

	public AbstractMoleculeArchiveFxFrame(MoleculeArchive<Molecule,MarsImageMetadata,MoleculeArchiveProperties> archive, MoleculeArchiveService moleculeArchiveService) {
		this.title = archive.getName();
		this.archive = archive;
		this.uiService = moleculeArchiveService.getUIService();
		this.moleculeArchiveService = moleculeArchiveService;
		
		archive.setWindow(this);
	}

	/**
	 * JFXPanel creates a link between Swing and JavaFX.
	 */
	public void init() {
		frame = new JFrame(title);
		
		frame.addWindowListener(new WindowAdapter() {
	         public void windowClosing(WindowEvent e) {
				close();
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
				//Stage primaryStage = new Stage();
				//primaryStage.setScene(buildScene());
		        //primaryStage.show();
				initFX(fxPanel);
			}
		});

	}

	public void initFX(JFXPanel fxPanel) {	
		Scene scene = buildScene();
		this.fxPanel.setScene(scene);
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
	
	protected Scene buildScene() {
		borderPane = new BorderPane();
    	//borderPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
    	
    	masker = new MaskerPane();
    	masker.setVisible(false);
    	
    	maskerStackPane = new StackPane();
    	maskerStackPane.getStylesheets().add("de/mpg/biochem/mars/fx/molecule/MoleculeArchiveFxFrame.css");
    	maskerStackPane.getChildren().add(borderPane);
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
    	
        buildMenuBar();
        buildTabs();
        
        stackPane = new StackPane();
        stackPane.getChildren().add(tabsContainer);
        borderPane.setCenter(stackPane);
        
        //Let's catch Plot events and pass them to the metadata panel if needed
        //For now we should just make sure it updates on tab change.
        /*
        getNode().addEventFilter(PlotEvent.PLOT_EVENT, new EventHandler<PlotEvent>() { 
			   @Override 
			   public void handle(PlotEvent e) { 
				   	if (e.getEventType().getName().equals("NEW_METADATA_REGION")) {
				   		moleculePropertiesPane.fireEvent(new MetadataSelectionChangedEvent(molecule));
				   		e.consume();
				   	}
			   };
     	});
        */
        Scene scene = new Scene(maskerStackPane);

        return scene;
	}
	
	private void buildTabs() {
		dashboardTab = new DashboardTab();
        dashboardTab.setStyle("-fx-background-color: -fx-focus-color;");

        commentsTab = new CommentsTab();
        settingsTab = new SettingsTab();
        
        imageMetadataTab = createImageMetadataTab();
        moleculesTab = createMoleculesTab();

        //fire save events for tabs as they are left and update events for new tabs
        tabsContainer.getSelectionModel().selectedItemProperty().addListener(
    		new ChangeListener<Tab>() {
    			@Override
    			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
    				updateMenus(((MoleculeArchiveTab)newValue).getMenus());
    				if (oldValue == commentsTab) {
    					commentsTab.fireEvent(new MoleculeArchiveSavingEvent(archive));
    				} else if (oldValue == imageMetadataTab) {
    					imageMetadataTab.fireEvent(new MoleculeArchiveSavingEvent(archive));
    				} else if (oldValue == moleculesTab) {
    					moleculesTab.fireEvent(new MoleculeArchiveSavingEvent(archive));
    				}
    				
	    			if (newValue == imageMetadataTab) {
						imageMetadataTab.fireEvent(new RefreshMetadataEvent());
					} else if (newValue == moleculesTab) {
						moleculesTab.fireEvent(new RefreshMoleculeEvent());
					}
    			}
    		});
        
        tabsContainer.getTabs().add(dashboardTab);
        tabsContainer.getTabs().add((Tab)imageMetadataTab);
        tabsContainer.getTabs().add((Tab)moleculesTab);
        tabsContainer.getTabs().add(commentsTab);
        tabsContainer.getTabs().add(settingsTab);
        
        fireEvent(new InitializeMoleculeArchiveEvent(archive));
    }
	
	protected void buildMenuBar() {
		Action fileSaveAction = new Action("save", "Shortcut+S", FLOPPY_ALT, e -> save());
		Action fileSaveCopyAction = new Action("Save a Copy...", null, null, e -> saveCopy());
		Action fileSaveVirtualStoreAction = new Action("Save a Virtual Store Copy...", null, null, e -> saveVirtualStoreCopy());
		Action fileCloseAction = new Action("close", null, null, e -> handleClose());
		
		Menu fileMenu = ActionUtils.createMenu("File",
				fileSaveAction,
				fileSaveCopyAction,
				fileSaveVirtualStoreAction,
				null,
				fileCloseAction);

		menuBar = new MenuBar(fileMenu);
		borderPane.setTop(menuBar);
	}
	
	public MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> getArchive() {
		return archive;
	}
	
	public JFrame getFrame() {
		return frame;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void close() {
		moleculeArchiveService.removeArchive(archive.getName());

		if (!uiService.isHeadless())
			WindowManager.removeWindow(frame);
		
		frame.setVisible(false);
		frame.dispose();
	}

	public void updateMenus(ArrayList<Menu> menus) {	
    	while (menuBar.getMenus().size() > 1)
    		menuBar.getMenus().remove(1);
    	if(menus.size() > 0) {
    		for (Menu menu : menus)
    			menuBar.getMenus().add(menu);
    	}
    }
    
    private void handleClose() {
    	archive.getWindow().close();
    	save();
    }

    public void save() {
    	 if (!lockArchive) {
    		 lock();
    		 fireEvent(new MoleculeArchiveSavingEvent(archive));
    		 moleculesTab.saveCurrentRecord();
    		 imageMetadataTab.saveCurrentRecord();
        	 try {
	 			 if (archive.getFile() != null) {
	 				 if(archive.getFile().getName().equals(archive.getName())) {
	 				 	try {
							archive.save();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
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
        	 fireEvent(new MoleculeArchiveSavedEvent(archive));
        	 unlock();
    	 }
    }
    
    public void saveCopy() {
    	if (!lockArchive) {
    		lock();
    	    String fileName = archive.getName();
    	    if (fileName.endsWith(".store"))
    	    	fileName = fileName.substring(0, fileName.length() - 5);
    	    
    	    try {
 				if (archive.getFile() != null) {
					saveAs(new File(archive.getFile().getParentFile(), fileName));
 				} else {
 					saveAs(new File(System.getProperty("user.home"), fileName));
 				}
    	    } catch (IOException e1) {
				e1.printStackTrace();
			}
    	    unlock();
    	}
    }
    
	private boolean saveAs(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(exporter.getExtensionDescription(),
		//		exporter.getExtensionFilters());
		//fileChooser.getExtensionFilters().add(extFilter);

		File file = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (file != null) {
			fireEvent(new MoleculeArchiveSavingEvent(archive));
			archive.saveAs(file);
			fireEvent(new MoleculeArchiveSavedEvent(archive));
			return true;
		}
		return false;
	}
    
    public void saveVirtualStoreCopy() {
    	 if (!lockArchive) {
    		lock();
    		moleculesTab.saveCurrentRecord();
    		imageMetadataTab.saveCurrentRecord();
 		 	
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
			unlock();
    	 }
    }
    
	private void saveAsVirtualStore(File saveAsFile) throws IOException {
		FileChooser fileChooser = new FileChooser();
		
		if (saveAsFile == null) {
			saveAsFile = new File(System.getProperty("user.home"));
		}
		fileChooser.setInitialDirectory(saveAsFile.getParentFile());
		fileChooser.setInitialFileName(saveAsFile.getName());
		//FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(exporter.getExtensionDescription(),
		//		exporter.getExtensionFilters());
		//fileChooser.getExtensionFilters().add(extFilter);

		File virtualDirectory = fileChooser.showSaveDialog(this.tabsContainer.getScene().getWindow());
		
		if (virtualDirectory != null) {	
			fireEvent(new MoleculeArchiveSavingEvent(archive));
			archive.saveAsVirtualStore(virtualDirectory);
			fireEvent(new MoleculeArchiveSavedEvent(archive));
		}
	}
	
	public Node getNode() {
		return maskerStackPane;
	}
	
	public abstract I createImageMetadataTab();
	
	public abstract M createMoleculesTab();
	
	public DashboardTab getDashboard() {
		return dashboardTab;
	}
	
    public void lock() {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
		    	fireEvent(new MoleculeArchiveLockingEvent(archive));
				masker.setVisible(true);
		    	lockArchive = true;
		    	fireEvent(new MoleculeArchiveLockedEvent(archive));
			}
    	});
    }
    
    public void unlock() {
    	Platform.runLater(new Runnable() {
			@Override
			public void run() {
		    	fireEvent(new MoleculeArchiveUnlockingEvent(archive));
				lockArchive = false;
				masker.setVisible(false);
				fireEvent(new MoleculeArchiveUnlockedEvent(archive));
			}
    	});
    }
    
    public void update() {
    	fireEvent(new MoleculeArchiveUnlockedEvent(archive));
    }

    public void fireEvent(Event event) {
    	dashboardTab.fireEvent(event);
        imageMetadataTab.fireEvent(event);
        moleculesTab.fireEvent(event);
        commentsTab.fireEvent(event);
        settingsTab.fireEvent(event);
    }

	@Override
	public void handle(MoleculeArchiveEvent event) {
	}

	@Override
	public void onInitializeMoleculeArchiveEvent(
			MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveLockingEvent() {
    	masker.setVisible(true);
	}

	@Override
	public void onMoleculeArchiveLockedEvent() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMoleculeArchiveUnlockingEvent() {
    	masker.setVisible(false);
	}

	@Override
	public void onMoleculeArchiveUnlockedEvent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {
		lock();
	}

	@Override
	public void onMoleculeArchiveSavedEvent() {
		unlock();
	}
}
