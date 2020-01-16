package de.mpg.biochem.mars.fx.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.PopupWindow;
import de.mpg.biochem.mars.fx.syntaxhighlighter.JavaSyntaxHighlighter;
import de.mpg.biochem.mars.molecule.MoleculeArchive;
import impl.org.controlsfx.skin.AutoCompletePopup;
import impl.org.controlsfx.skin.AutoCompletePopupSkin;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import static org.reactfx.EventStreams.nonNullValuesOf;

import java.util.Optional;

public class MarsScriptEditor extends CodeArea {
	
	// Delay for async formatting, in milliseconds
	private static int delayMillis = 100;
	
	private Subscription cleanupWhenNoLongerNeedIt;

	private final Collection<KeyCombination> suggestKey = Arrays.asList(
		      new KeyCodeCombination(KeyCode.PERIOD));
	
	private static final Set<String> METHOD_NAMES = new HashSet<>();
	static {
		for (Method method : MoleculeArchive.class.getMethods()) {
			METHOD_NAMES.add(method.getName());
		}
	}
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public MarsScriptEditor() {
		super();
		setParagraphGraphicFactory(LineNumberFactory.get(this));
		
		getStylesheets().add("de/mpg/biochem/mars/fx/syntaxhighlighter/java-keywords.css");
		
		cleanupWhenNoLongerNeedIt = this
				.multiPlainChanges()
				.successionEnds(Duration.ofMillis(delayMillis))
				.supplyTask(() -> computeHighlightingAsync(this.getText()))
				.awaitLatest(this.multiPlainChanges())
				.filterMap(t -> {
					if (t.isSuccess()) {
						return Optional.of(t.get());
					} else {
						return Optional.empty();
					}
				})
				.subscribe(change -> this.setStyleSpans(0, change));
		
		InnerController innerController = new InnerController(this);
	}
	
	private boolean shouldSuggest(KeyEvent e) {
	    if (suggestKey.stream().filter(c -> c.match(e)).count() > 0) {
	      return true;
	    } /*else if (keys.suggest().getValue().match(e)) {
	      return true;
	    } */else {
	      return false;
	    }
	  }
	
	public void cleanup() {
		cleanupWhenNoLongerNeedIt.unsubscribe();
	}
	
	private Task<StyleSpans<Collection<String>>> computeHighlightingAsync(final String text) {
		Task task = new Task<StyleSpans<Collection<String>>>() {
			@Override
			protected StyleSpans<Collection<String>> call() {
				return JavaSyntaxHighlighter.computeHighlighting(text);
			}
		};
		executor.execute(task);
		return task;
	}
	
	private class InnerController {

	    CodeArea editor;

	    AutoCompletePopup<String> popup = new AutoCompletePopup<>();

	    public InnerController(CodeArea editor) {
	      this.editor = editor;

	      editor.textProperty().addListener((ob, o, n) -> {
	        if (editor.isFocused() && popup.isShowing()) {
	          showPopup();
	        }
	      });
	      editor.focusedProperty().addListener((ob, o, n) -> {
	        if (n == false) {
	          hidePopup();
	        }
	      });

	      popup.setOnSuggestion(sce -> {
	        completeUserInput(sce.getSuggestion());
	        hidePopup();
	      });

	      editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
	    	  if (shouldSuggest(e))
	    		  showPopup();
	    	  
	    	  //e.consume();
	      });
	    }

	    public void showPopup() {
	      Collection<String> suggestions = METHOD_NAMES;//cssSuggestion.getSuggestion(editor.getText(), editor.getCaretPosition(), context);
	      if (suggestions.isEmpty()) {
	        hidePopup();
	      } else {
	        popup.getSuggestions().setAll(suggestions);
	        
	        selectFirstSuggestion(popup);
	        if (editor.getCaretBounds().isPresent()) {	
	        	popup.setX(editor.getCaretBounds().get().getMinX());
	        	popup.setY(editor.getCaretBounds().get().getMinY());
	        }
	        
	        if (popup.isShowing() == false) {
	          popup.show(editor.getScene().getWindow());
	        }
	      }
	    }

	    public void hidePopup() {
	      popup.hide();
	    }

	    private void completeUserInput(String suggestion) {
	      IndexRange range = new IndexRange(10,11);//cssSuggestion.getReplaceRange(editor.getText(), editor.getCaretPosition(), context);
	      editor.deleteText(range);
	      editor.insertText(range.getStart(), suggestion);
	      editor.moveTo(range.getStart() + suggestion.length());
	    }

	    private void selectFirstSuggestion(AutoCompletePopup<?> autoCompletionPopup) {
	      Skin<?> skin = autoCompletionPopup.getSkin();
	      if (skin instanceof AutoCompletePopupSkin) {
	        AutoCompletePopupSkin<?> au = (AutoCompletePopupSkin<?>) skin;
	        ListView<?> li = (ListView<?>) au.getNode();
	        if (li.getItems() != null && !li.getItems().isEmpty()) {
	          li.getSelectionModel().select(0);
	        }
	      }
	    }
	}
}
