package milkman.ui.main;

import static milkman.utils.fxml.FxmlBuilder.button;
import static milkman.utils.fxml.FxmlBuilder.hbox;
import static milkman.utils.fxml.FxmlBuilder.icon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.jfoenix.controls.JFXTabPane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import milkman.domain.RequestContainer;
import milkman.domain.ResponseContainer;
import milkman.ui.commands.UiCommand;
import milkman.ui.commands.UiCommand.CloseRequest.CloseType;
import milkman.ui.main.RequestComponent.RequestComponentFxml;
import milkman.ui.main.ResponseComponent.ResponseComponentFxml;
import milkman.utils.Event;
import milkman.utils.fxml.FxmlBuilder.HboxExt;
import milkman.utils.fxml.FxmlBuilder.VboxExt;

@Singleton
@RequiredArgsConstructor(onConstructor_={@Inject})
@Slf4j
public class WorkingAreaComponent implements Initializable {

	private static final String CSS_CLASS_REQUEST_ACTIVE = "mm-request-active";
	private static final String CSS_CLASS_REQUEST_DIRTY = "mm-request-dirty";
	private final RequestComponent restRequestComponent;
	private final ResponseComponent responseComponent;

	
	public final Event<UiCommand> onCommand = new Event<UiCommand>();
	@FXML JFXTabPane tabPane;
	private ChangeListener<? super Tab> tabChangeListener;
	
	public void display(RequestContainer activeRequest, List<RequestContainer> openedRequests) {
		restRequestComponent.display(activeRequest);
		setupTabs(activeRequest, openedRequests);
		log.info("request area refreshed");
	}

	private void setupTabs(RequestContainer activeRequest, List<RequestContainer> openedRequests) {
		tabPane.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
		tabPane.getTabs().clear();
		Tab activeTab = null;
		for(RequestContainer r : openedRequests){
			boolean isActive = activeRequest == r;
			Tab tab = createTestTab(r, isActive);
			tabPane.getTabs().add(tab);
			if (isActive)
				activeTab = tab;
		};
		if (activeTab != null) {
			Tab t = activeTab;
//			Platform.runLater(() -> {
				tabPane.getSelectionModel().select(t);	
//			});
		}
		
		tabPane.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
		tabPane.requestLayout();
		log.info("tab repainted");
	}
	private Tab createTestTab(RequestContainer r, boolean isActive) {
		Tab tab = new Tab();
		tab.setGraphic(new Label(r.getName()));
		if (isActive)
			tab.getStyleClass().add(CSS_CLASS_REQUEST_ACTIVE);
		
		if (r.isDirty())
			tab.getStyleClass().add(CSS_CLASS_REQUEST_DIRTY);
		
		r.onDirtyChange.add((o,n) -> {
			if (n) 
				tab.getStyleClass().add(CSS_CLASS_REQUEST_DIRTY);
			else
				tab.getStyleClass().remove(CSS_CLASS_REQUEST_DIRTY);
		});
		
		MenuItem closeEntry = new MenuItem("Close");
		closeEntry.setOnAction(a -> onCommand.invoke(new UiCommand.CloseRequest(r, CloseType.CLOSE_THIS)));
		
		MenuItem closeRightEntry = new MenuItem("Close Tabs to the Right");
		closeRightEntry.setOnAction(a -> onCommand.invoke(new UiCommand.CloseRequest(r, CloseType.CLOSE_RIGHT)));
		
		MenuItem closeAllEntry = new MenuItem("Close All");
		closeAllEntry.setOnAction(a -> onCommand.invoke(new UiCommand.CloseRequest(r, CloseType.CLOSE_ALL)));
		

		MenuItem closeOtherEntry = new MenuItem("Close Others");
		closeOtherEntry.setOnAction(a -> onCommand.invoke(new UiCommand.CloseRequest(r, CloseType.CLOSE_OTHERS)));
		
		MenuItem renameEntry = new MenuItem("Rename");
		renameEntry.setOnAction(a -> {
			onCommand.invoke(new UiCommand.RenameRequest(r));
		});
		
		tab.setContextMenu(new ContextMenu(closeEntry, closeRightEntry,closeAllEntry, closeOtherEntry, renameEntry));
		tab.setUserData(r);

		tab.getGraphic().setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.MIDDLE) {
				onCommand.invoke(new UiCommand.CloseRequest(r, CloseType.CLOSE_THIS));
				e.consume();
			}
		});
		
		return tab;
	}

	public void displayResponseFor(RequestContainer request, ResponseContainer response) {
		if (restRequestComponent.getCurrentRequest().getId().equals(request.getId()))
			responseComponent.display(request, response);
	}
	
	@FXML public void onNewRequestClick() {
		onCommand.invoke(new UiCommand.NewRequest(tabPane));
	}

	public void clearResponse() {
		hideSpinner();
		responseComponent.clear();
	}
	
	public void showSpinner(Runnable cancellationListener) {
		responseComponent.showSpinner(cancellationListener);
	}
	public void hideSpinner() {
		responseComponent.hideSpinner();
	}
	
	

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		tabChangeListener = (obs, o, n) -> {
			if (n != null && o != n) {
				RequestContainer r = (RequestContainer) n.getUserData();
				onCommand.invoke(new UiCommand.SwitchToRequest(r));
			}
		};
		tabPane.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
		//Bug in jfoenix: when not disabling this, the GUI does not refresh except if i hover over it with mouse
		//might be because we dont really use the content of the tabs
		tabPane.setDisableAnimation(true);
	}
	
	public static class WorkingAreaComponentFxml extends VboxExt {
		public WorkingAreaComponentFxml(WorkingAreaComponent controller) {
			HboxExt reqArea = add(hbox("openRequestArea"));
			
			JFXTabPane pane = new JFXTabPane();
			pane.setId("tabPane");
			controller.tabPane = reqArea.add(pane, true);
			
			
			reqArea.add(button(icon(FontAwesomeIcon.PLUS), controller::onNewRequestClick))
				.setPrefWidth(40);
			
			
			
			SplitPane splitPane = add(new SplitPane(), true);
			splitPane.setOrientation(Orientation.VERTICAL);
			
			splitPane.getItems().add(new RequestComponentFxml(controller.restRequestComponent));
			splitPane.getItems().add(new ResponseComponentFxml(controller.responseComponent));
			
			
			controller.initialize(null, null);
		}
	}
	
}
