package core.app.views;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import bt.db.constants.SqlValue;
import bt.db.statement.impl.SelectStatement;
import bt.db.statement.result.SqlResult;
import bt.db.statement.result.SqlResultSet;
import bt.gui.fx.core.FxView;
import core.app.ViewManager;
import core.db.ActiveDatabase;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * @author &#8904
 *
 */
public class DataView extends FxView
{
    private Stage stage;
    private ActiveDatabase db;
    private TableView table;
    private ListView sysTableList;
    private ListView userTableList;
    private TextField sqlField;
    private TextField tableFilterField;
    private TitledPane userTablesTab;
    private Accordion tableListAccordion;
    private Button commitButton;
    private Button rollbackButton;
    private Button disconnectButton;

    public DataView(ActiveDatabase db)
    {
        this.shouldMaximize = true;
        this.db = db;
        this.db.registerExceptionHandler(this::handleException);
    }

    /**
     * @see bt.gui.fx.core.FxView#prepareView(javafx.fxml.FXMLLoader)
     */
    @Override
    protected void prepareView()
    {
        this.sqlField = getElement(TextField.class, "sql_field");
        this.sqlField.setOnKeyPressed(e ->
        {
            this.sqlField.setTooltip(null);
            this.sqlField.getStyleClass().remove("error");

            if (e.getCode().equals(KeyCode.ENTER))
            {
                executeSql(this.sqlField.getText());
            }
        });

        this.tableFilterField = getElement(TextField.class, "table_search");
        this.tableFilterField.setOnKeyPressed(e ->
        {
            if (e.getCode().equals(KeyCode.ENTER))
            {
                fillTableLists(this.tableFilterField.getText().strip());
            }
        });

        this.userTablesTab = getElement(TitledPane.class, "user_tables_tab");

        this.tableListAccordion = getElement(Accordion.class, "table_search_accordion");
        this.tableListAccordion.setExpandedPane(this.userTablesTab);

        this.userTableList = getElement(ListView.class, "user_tables");
        this.userTableList.setOnMouseClicked(e ->
        {
            if (e.getClickCount() >= 2)
            {
                this.sqlField.setTooltip(null);
                this.sqlField.getStyleClass().remove("error");

                String table = this.userTableList.getSelectionModel().getSelectedItem().toString();
                this.sqlField.setText(this.db.select().from(table).toString());
                executeSql(this.sqlField.getText());
            }
        });

        this.sysTableList = getElement(ListView.class, "sys_tables");
        this.sysTableList.setOnMouseClicked(e ->
        {
            if (e.getClickCount() >= 2)
            {
                this.sqlField.setTooltip(null);
                this.sqlField.getStyleClass().remove("error");

                String table = "SYS." + this.sysTableList.getSelectionModel().getSelectedItem().toString();
                this.sqlField.setText(this.db.select().from(table).toString());
                executeSql(this.sqlField.getText());
            }
        });

        this.commitButton = getElement(Button.class, "commit_button");
        this.commitButton.setOnAction(e ->
        {
            this.db.commit();
            this.commitButton.setDisable(true);
            this.rollbackButton.setDisable(true);
            this.stage.setTitle("Transaction commited");
        });

        this.rollbackButton = getElement(Button.class, "rollback_button");
        this.rollbackButton.setOnAction(e ->
        {
            this.db.rollback();
            this.commitButton.setDisable(true);
            this.rollbackButton.setDisable(true);
            this.stage.setTitle("Transaction rolled back");
        });

        this.disconnectButton = getElement(Button.class, "disconnect_button");
        this.disconnectButton.setOnAction(e ->
        {
            this.disconnectButton.setDisable(true);
            disable(true);
            this.db.rollback();
            this.db.kill();

            ViewManager.get().setView(SelectionView.class, true);
        });

        this.table = getElement(TableView.class, "table");

        fillTableLists(null);
    }

    /**
     * @see bt.gui.fx.core.FxView#prepareStage(javafx.stage.Stage)
     */
    @Override
    protected void prepareStage(Stage stage)
    {
        this.stage = stage;
        stage.setTitle("View data");
        stage.setResizable(true);
    }

    private void fillTableLists(String filter)
    {
        this.sysTableList.getItems().clear();

        SelectStatement select = this.db.select("tablename")
                                        .from(SqlValue.SYSTABLE)
                                        .where("tabletype").equals("S")
                                        .orderBy("tablename").asc();

        if (filter != null && !filter.isBlank())
        {
            select.and("lower(tablename)").like("%" + filter.toLowerCase() + "%");
        }

        var tables = select.execute();

        for (SqlResult result : tables)
        {
            this.sysTableList.getItems().add(result.get("tablename"));
        }

        this.userTableList.getItems().clear();

        select = this.db.select("tablename")
                        .from(SqlValue.SYSTABLE)
                        .where("tabletype").equals("T")
                        .orderBy("tablename").asc();

        if (filter != null && !filter.isBlank())
        {
            select.and("lower(tablename)").like("%" + filter.toLowerCase() + "%");
        }

        tables = select.execute();

        for (SqlResult result : tables)
        {
            this.userTableList.getItems().add(result.get("tablename"));
        }
    }

    private void disable(boolean disable)
    {
        this.userTableList.setDisable(disable);
        this.sysTableList.setDisable(disable);
        this.sqlField.setDisable(disable);
    }

    private void createTable(SqlResultSet results)
    {
        this.table.getColumns().clear();
        double colWidth = this.table.getWidth() / results.getColumnOrder().size();
        int[] colSizes = results.getColumnSizes();

        for (int i = 0; i < results.getColumnOrder().size(); i ++ )
        {
            String colName = results.getColumnOrder().get(i);

            TableColumn<SqlResult, String> col = new TableColumn<>(colName);
            col.setPrefWidth(colWidth);
            col.setMinWidth(colSizes[i] * 5);
            col.setStyle("-fx-alignment: CENTER;");
            col.setCellValueFactory(new Callback<CellDataFeatures<SqlResult, String>, ObservableValue<String>>()
            {
                @Override
                public ObservableValue<String> call(CellDataFeatures<SqlResult, String> r)
                {
                    Object value = r.getValue().get(colName);
                    return new ReadOnlyStringWrapper(value != null ? value.toString() : null);
                }
            });
            this.table.getColumns().add(col);

            Callback<TableColumn<SqlResult, String>, TableCell<SqlResult, String>> existingCellFactory = col.getCellFactory();

            col.setCellFactory(c ->
            {
                TableCell<SqlResult, String> cell = existingCellFactory.call(c);

                Tooltip tooltip = new Tooltip();
                tooltip.textProperty().bind(cell.itemProperty().asString());
                cell.setTooltip(tooltip);
                return cell;
            });
        }
    }

    private void fillTable(SqlResultSet results)
    {
        this.table.getItems().clear();
        List<SqlResult> resultList = new ArrayList<>();

        for (SqlResult result : results)
        {
            resultList.add(result);
        }

        ObservableList<SqlResult> tableValues = FXCollections.observableList(resultList);
        this.table.setItems(tableValues);
        this.table.refresh();
    }

    private void executeSql(String sql)
    {
        disable(true);
        this.stage.setTitle("Executing...");

        SqlResultSet results = null;

        if (!sql.isBlank())
        {
            try
            {
                int count = 0;
                long startTime = 0;
                long endTime = 0;

                if (sql.strip().toLowerCase().startsWith("select"))
                {
                    startTime = System.currentTimeMillis();
                    results = this.db.executeQuery(this.sqlField.getText());
                    endTime = System.currentTimeMillis();
                    count = results.size();
                    createTable(results);
                    if (results.size() > 0)
                    {
                        fillTable(results);
                    }
                    else
                    {
                        this.table.getItems().clear();
                        this.table.refresh();
                    }
                }
                else
                {
                    this.table.getColumns().clear();
                    startTime = System.currentTimeMillis();
                    count = this.db.executeUpdate(this.sqlField.getText());
                    endTime = System.currentTimeMillis();
                    fillTableLists(this.tableFilterField.getText().isBlank() ? null : this.tableFilterField.getText().strip());
                    this.commitButton.setDisable(false);
                    this.rollbackButton.setDisable(false);
                }

                this.stage.setTitle((endTime - startTime) + "ms  (" + count + ")      " + sql.strip());
            }
            catch (Exception ex)
            {
                this.table.getColumns().clear();
                this.stage.setTitle(ex.getMessage());
                Tooltip tooltip = new Tooltip(ex.getMessage());
                this.sqlField.setTooltip(tooltip);
                this.sqlField.getStyleClass().add("error");
            }
        }
        else
        {
            this.stage.setTitle("View data");
        }

        disable(false);
    }

    public void handleException(SQLException e)
    {
        this.table.getColumns().clear();
        this.stage.setTitle(e.getMessage());
        Tooltip tooltip = new Tooltip(e.getMessage());
        this.sqlField.setTooltip(tooltip);
        this.sqlField.getStyleClass().add("error");
    }

    /**
     * @see bt.gui.fx.core.FxView#prepareScene(javafx.scene.Scene)
     */
    @Override
    protected void prepareScene(Scene scene)
    {
        scene.getStylesheets().add(getClass().getResource("/textfield_error.css").toString());
    }
}