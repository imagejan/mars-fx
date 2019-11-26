package de.mpg.biochem.mars.fx.molecule;

import de.mpg.biochem.mars.molecule.MarsImageMetadata;
import de.mpg.biochem.mars.molecule.Molecule;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import de.mpg.biochem.mars.molecule.MoleculeArchiveProperties;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.prefs.Preferences;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import de.mpg.biochem.mars.fx.*;
import de.mpg.biochem.mars.fx.editor.SmartEdit;
import de.mpg.biochem.mars.fx.event.MoleculeArchiveEvent;
import de.mpg.biochem.mars.fx.options.MarkdownExtensionsPane;
import de.mpg.biochem.mars.fx.options.Options;
import de.mpg.biochem.mars.fx.options.Options.RendererType;
import de.mpg.biochem.mars.fx.util.Action;
import de.mpg.biochem.mars.fx.util.ActionUtils;
import de.mpg.biochem.mars.fx.util.Utils;

public class CommentsTab extends AbstractMoleculeArchiveTab {

	private BorderPane borderPane;
	
	private Scene scene;
	private Node extensionsButton;
	private ToolBar nonEditToolBar;
	private ToolBar editToolBar;
    final BooleanProperty stageFocusedProperty = new SimpleBooleanProperty();

    private CommentEditor commentEditor;
	
	private ArrayList<Menu> menus;
	
	public CommentsTab() {
		super();
		
		Region bookIcon = new Region();
        bookIcon.getStyleClass().add("bookIcon");
		
		setIcon(bookIcon);
		
		Options.load(getOptions());
    	
    	menus = new ArrayList<Menu>();
    	
    	//Build markdown gui
    	borderPane = new BorderPane();
    	borderPane.getStyleClass().add("main");
    	borderPane.setPrefSize(800, 800);
		commentEditor = new CommentEditor();
		borderPane.setCenter(commentEditor);
		
    	initializeToolBars();
    	
    	borderPane.setTop(nonEditToolBar);
    	
		scene = new Scene(borderPane);
		scene.getStylesheets().add("org/markdownwriterfx/MarkdownWriter.css");
		
		Utils.fixSpaceAfterDeadKey(scene);
		
		//Platform.runLater(() -> stageFocusedProperty.bind(scene.getWindow().focusedProperty()));
		
		getNode().addEventHandler(MoleculeArchiveEvent.MOLECULE_ARCHIVE_EVENT, this);
		
		setContent(borderPane);
	}
    
	static private Preferences getPrefsRoot() {
		return Preferences.userRoot().node("markdownwriterfx");
	}

	static Preferences getOptions() {
		return getPrefsRoot().node("options");
	}
    
    private void initializeToolBars() {
    	Action editModeAction = new Action("Edit", "Shortcut+E", PENCIL,
				null, null, commentEditor.editMode);
		Node editModeButton = ActionUtils.createToolBarButton(editModeAction);

    	nonEditToolBar = new ToolBar();
    	nonEditToolBar.getItems().add(0, editModeButton);
    	
		// Edit actions
		Action editUndoAction = new Action(Messages.get("MainWindow.editUndoAction"), "Shortcut+Z", UNDO,
				e -> commentEditor.getEditor().undo());
		Action editRedoAction = new Action(Messages.get("MainWindow.editRedoAction"), "Shortcut+Y", REPEAT,
				e -> commentEditor.getEditor().redo());
		Action editCutAction = new Action(Messages.get("MainWindow.editCutAction"), "Shortcut+X", CUT,
				e -> commentEditor.getEditor().cut());
		Action editCopyAction = new Action(Messages.get("MainWindow.editCopyAction"), "Shortcut+C", COPY,
				e -> commentEditor.getEditor().copy());
		Action editPasteAction = new Action(Messages.get("MainWindow.editPasteAction"), "Shortcut+V", PASTE,
				e -> commentEditor.getEditor().paste());
		Action editSelectAllAction = new Action(Messages.get("MainWindow.editSelectAllAction"), "Shortcut+A", null,
				e -> commentEditor.getEditor().selectAll());
		Action editFindAction = new Action(Messages.get("MainWindow.editFindAction"), "Shortcut+F", SEARCH,
				e -> commentEditor.getEditor().find(false));
		Action editReplaceAction = new Action(Messages.get("MainWindow.editReplaceAction"), "Shortcut+H", RETWEET,
				e -> commentEditor.getEditor().find(true));
		Action editFindNextAction = new Action(Messages.get("MainWindow.editFindNextAction"), "F3", null,
				e -> commentEditor.getEditor().findNextPrevious(true));
		Action editFindPreviousAction = new Action(Messages.get("MainWindow.editFindPreviousAction"), "Shift+F3", null,
				e -> commentEditor.getEditor().findNextPrevious(false));

		Action editFormatAllAction = new Action(Messages.get("MainWindow.editFormatAll"), "Shortcut+Shift+F", null,
				e -> commentEditor.getEditor().getSmartEdit().format(false, null));
		Action editFormatSelectionAction = new Action(Messages.get("MainWindow.editFormatSelection"), "Shortcut+Shift+Alt+F", null,
				e -> commentEditor.getEditor().getSmartEdit().format(true, null));
		
		// View actions
		Action viewPreviewAction = new Action(Messages.get("MainWindow.viewPreviewAction"), null, EYE,
				null, null, commentEditor.previewVisible);
		Action viewShowLineNoAction = new Action(Messages.get("MainWindow.viewShowLineNoAction"), null, null,
				null, null, Options.showLineNoProperty());
		Action viewShowWhitespaceAction = new Action(Messages.get("MainWindow.viewShowWhitespaceAction"), "Alt+W", null,
				null, null, Options.showWhitespaceProperty());
		Action viewShowImagesEmbeddedAction = new Action(Messages.get("MainWindow.viewShowImagesEmbeddedAction"), "Alt+I", null,
				null, null, Options.showImagesEmbeddedProperty());

		// Insert actions
		Action insertBoldAction = new Action(Messages.get("MainWindow.insertBoldAction"), "Shortcut+B", BOLD,
				e -> commentEditor.getEditor().getSmartEdit().insertBold(Messages.get("MainWindow.insertBoldText")));
		Action insertItalicAction = new Action(Messages.get("MainWindow.insertItalicAction"), "Shortcut+I", ITALIC,
				e -> commentEditor.getEditor().getSmartEdit().insertItalic(Messages.get("MainWindow.insertItalicText")));
		Action insertStrikethroughAction = new Action(Messages.get("MainWindow.insertStrikethroughAction"), "Shortcut+T", STRIKETHROUGH,
				e -> commentEditor.getEditor().getSmartEdit().insertStrikethrough(Messages.get("MainWindow.insertStrikethroughText")));
		Action insertCodeAction = new Action(Messages.get("MainWindow.insertCodeAction"), "Shortcut+K", CODE,
				e -> commentEditor.getEditor().getSmartEdit().insertInlineCode(Messages.get("MainWindow.insertCodeText")));

		Action insertLinkAction = new Action(Messages.get("MainWindow.insertLinkAction"), "Shortcut+L", LINK,
				e -> commentEditor.getEditor().getSmartEdit().insertLink());
		Action insertImageAction = new Action(Messages.get("MainWindow.insertImageAction"), "Shortcut+G", PICTURE_ALT,
				e -> commentEditor.getEditor().getSmartEdit().insertImage());

		Action insertUnorderedListAction = new Action(Messages.get("MainWindow.insertUnorderedListAction"), "Shortcut+U", LIST_UL,
				e -> commentEditor.getEditor().getSmartEdit().insertUnorderedList());
		Action insertOrderedListAction = new Action(Messages.get("MainWindow.insertOrderedListAction"), "Shortcut+Shift+U", LIST_OL,
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n1. ", ""));
		Action insertBlockquoteAction = new Action(Messages.get("MainWindow.insertBlockquoteAction"), "Ctrl+Q", QUOTE_LEFT, // not Shortcut+Q because of conflict on Mac
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n> ", ""));
		Action insertFencedCodeBlockAction = new Action(Messages.get("MainWindow.insertFencedCodeBlockAction"), "Shortcut+Shift+K", FILE_CODE_ALT,
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n```\n", "\n```\n\n", Messages.get("MainWindow.insertFencedCodeBlockText")));

		Action insertHeader1Action = new Action(Messages.get("MainWindow.insertHeader1Action"), "Shortcut+1", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(1, Messages.get("MainWindow.insertHeader1Text")));
		Action insertHeader2Action = new Action(Messages.get("MainWindow.insertHeader2Action"), "Shortcut+2", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(2, Messages.get("MainWindow.insertHeader2Text")));
		Action insertHeader3Action = new Action(Messages.get("MainWindow.insertHeader3Action"), "Shortcut+3", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(3, Messages.get("MainWindow.insertHeader3Text")));
		Action insertHeader4Action = new Action(Messages.get("MainWindow.insertHeader4Action"), "Shortcut+4", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(4, Messages.get("MainWindow.insertHeader4Text")));
		Action insertHeader5Action = new Action(Messages.get("MainWindow.insertHeader5Action"), "Shortcut+5", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(5, Messages.get("MainWindow.insertHeader5Text")));
		Action insertHeader6Action = new Action(Messages.get("MainWindow.insertHeader6Action"), "Shortcut+6", HEADER,
				e -> commentEditor.getEditor().getSmartEdit().insertHeading(6, Messages.get("MainWindow.insertHeader6Text")));

		Action insertHorizontalRuleAction = new Action(Messages.get("MainWindow.insertHorizontalRuleAction"), null, null,
				e -> commentEditor.getEditor().getSmartEdit().surroundSelection("\n\n---\n\n", ""));

		Menu editMenu = ActionUtils.createMenu("Edit",
				editUndoAction,
				editRedoAction,
				null,
				editCutAction,
				editCopyAction,
				editPasteAction,
				editSelectAllAction,
				null,
				editFindAction,
				editReplaceAction,
				null,
				editFindNextAction,
				editFindPreviousAction,
				null,
				editFormatAllAction,
				editFormatSelectionAction);

		Menu viewMenu = ActionUtils.createMenu("View",
				viewPreviewAction,
				null,
				viewShowLineNoAction,
				viewShowWhitespaceAction,
				viewShowImagesEmbeddedAction);

		Menu insertMenu = ActionUtils.createMenu("Insert",
				insertBoldAction,
				insertItalicAction,
				insertStrikethroughAction,
				insertCodeAction,
				null,
				//insertLinkAction,
				//insertImageAction,
				//null,
				insertUnorderedListAction,
				insertOrderedListAction,
				insertBlockquoteAction,
				insertFencedCodeBlockAction,
				null,
				insertHeader1Action,
				insertHeader2Action,
				insertHeader3Action,
				insertHeader4Action,
				insertHeader5Action,
				insertHeader6Action,
				null,
				insertHorizontalRuleAction);
		
		menus.add(editMenu);
		menus.add(viewMenu);
		menus.add(insertMenu);
/*
		Menu toolsMenu = ActionUtils.createMenu(Messages.get("MainWindow.toolsMenu"),
				toolsOptionsAction);

		Menu helpMenu = ActionUtils.createMenu(Messages.get("MainWindow.helpMenu"),
				helpAboutAction);
		*/
		// Tools actions
		//Action toolsOptionsAction = new Action(Messages.get("MainWindow.toolsOptionsAction"), "Shortcut+,", null, e -> toolsOptions());

		//---- ToolBar ----

		editToolBar = ActionUtils.createToolBar(
				editUndoAction,
				editRedoAction,
				null,
				new Action(insertBoldAction, createActiveEditBooleanProperty(SmartEdit::boldProperty)),
				new Action(insertItalicAction, createActiveEditBooleanProperty(SmartEdit::italicProperty)),
				new Action(insertCodeAction, createActiveEditBooleanProperty(SmartEdit::codeProperty)),
				//null,
				//new Action(insertLinkAction, createActiveEditBooleanProperty(SmartEdit::linkProperty)),
				//new Action(insertImageAction, createActiveEditBooleanProperty(SmartEdit::imageProperty)),
				//null,
				new Action(insertUnorderedListAction, createActiveEditBooleanProperty(SmartEdit::unorderedListProperty)),
				new Action(insertOrderedListAction, createActiveEditBooleanProperty(SmartEdit::orderedListProperty)),
				new Action(insertBlockquoteAction, createActiveEditBooleanProperty(SmartEdit::blockquoteProperty)),
				new Action(insertFencedCodeBlockAction, createActiveEditBooleanProperty(SmartEdit::fencedCodeProperty)),
				null,
				new Action(insertHeader1Action, createActiveEditBooleanProperty(SmartEdit::headerProperty)));
		
		editToolBar.getItems().add(0, new Separator());

		// horizontal spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		editToolBar.getItems().add(spacer);

		// preview renderer type choice box
		ChoiceBox<RendererType> previewRenderer = new ChoiceBox<>();
		previewRenderer.setFocusTraversable(false);
		previewRenderer.getItems().addAll(RendererType.values());
		previewRenderer.getSelectionModel().select(Options.getMarkdownRenderer());
		previewRenderer.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			Options.setMarkdownRenderer(n);
		});
		Options.markdownRendererProperty().addListener((ob, o, n) -> {
			previewRenderer.getSelectionModel().select(n);
		});
		
		//For the moment we just leave the CommonMark Editor
		//toolBar.getItems().add(previewRenderer);

		// markdown extensions popover
		String title = Messages.get("MainWindow.MarkdownExtensions");
		extensionsButton = ActionUtils.createToolBarButton(
				new Action(title, null, COG, e -> {
					PopOver popOver = new PopOver();
					popOver.setTitle(title);
					popOver.setHeaderAlwaysVisible(true);
					popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
					popOver.setContentNode(new MarkdownExtensionsPane(true));
					popOver.show(extensionsButton);
				}));
		editToolBar.getItems().add(extensionsButton);
		editToolBar.getItems().add(new Separator());

		Action editModeAction2 = new Action("Edit", "Shortcut+E", PENCIL,
				null, null, commentEditor.editMode);
		Node editModeButton2 = ActionUtils.createToolBarButton(editModeAction2);

    	editToolBar.getItems().add(0, editModeButton2);
		
		// preview actions
		Node previewButton = ActionUtils.createToolBarButton(viewPreviewAction);
		editToolBar.getItems().add(previewButton);
		
    	ChangeListener editModeListener = (observable, oldValue, newValue) -> updateToolsAndMenus();
    	commentEditor.editMode.addListener(editModeListener);
	}
    
    private boolean updateToolsAndMenus;
    private void updateToolsAndMenus() {
    	// avoid too many (and useless) runLater() invocations
		if (updateToolsAndMenus)
			return;
		updateToolsAndMenus = true;
		
		Platform.runLater(() -> {
			updateToolsAndMenus = false;

			if (commentEditor.editMode.get()) {
				borderPane.setTop(editToolBar);
			} else {
				commentEditor.showPreview();
				borderPane.setTop(nonEditToolBar);
			}
		});
    }
    
	/**
	 * Creates a boolean property that is bound to another boolean value
	 * of the active editor's SmartEdit.
	 */
	private BooleanProperty createActiveEditBooleanProperty(Function<SmartEdit, ObservableBooleanValue> func) {
		BooleanProperty b = new SimpleBooleanProperty() {
			@Override
			public void set(boolean newValue) {
				// invoked when the user invokes an action
				// do not try to change SmartEdit properties because this
				// would throw a "bound value cannot be set" exception
			}
		};
		
		b.unbind();
		
		b.bind(func.apply(commentEditor.getEditor().getSmartEdit()));
		
		return b;
	}
	
	Alert createAlert(AlertType alertType, String title,
			String contentTextFormat, Object... contentTextArgs)
		{
			Alert alert = new Alert(alertType);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.setContentText(MessageFormat.format(contentTextFormat, contentTextArgs));
			alert.initOwner(scene.getWindow());
			return alert;
		}

	@Override
    public void onInitializeMoleculeArchiveEvent(MoleculeArchive<Molecule, MarsImageMetadata, MoleculeArchiveProperties> archive) {
    	super.onInitializeMoleculeArchiveEvent(archive);
		commentEditor.setArchive(archive);
	}
	
	public Node getNode() {
		return borderPane;
	}
	
	public ArrayList<Menu> getMenus() {
		return menus;
	}

	@Override
	public void onMoleculeArchiveLockingEvent() {
		commentEditor.save();
	}

	@Override
	public void onMoleculeArchiveSavingEvent() {
		commentEditor.save();
	}
}
