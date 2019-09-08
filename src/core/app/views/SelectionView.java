package core.app.views;

import java.io.File;

import bt.db.config.DatabaseConfiguration;
import bt.db.constants.SqlType;
import bt.db.constants.SqlValue;
import bt.db.statement.result.SqlResult;
import bt.gui.fx.core.FxView;
import core.app.ViewManager;
import core.db.ActiveDatabase;
import core.db.RecentDatabase;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * @author &#8904
 *
 */
public class SelectionView extends FxView
{
    private Stage stage;
    private RecentDatabase db;
    private TextField databaseField;
    private Button chooseButton;
    private Button continueButton;
    private ListView recentList;

    public SelectionView()
    {
        this.width = 600;
        this.height = 400;

        var config = new DatabaseConfiguration().path("./db")
                                                .create()
                                                .useUnicode()
                                                .characterEncoding("utf8")
                                                .autoReconnect();

        this.db = new RecentDatabase(config);
    }

    /**
     * @see bt.gui.fx.core.FxView#prepareView(javafx.fxml.FXMLLoader)
     */
    @Override
    protected void prepareView()
    {
        this.databaseField = getElement(TextField.class, "database_field");
        this.databaseField.setDisable(true);

        this.recentList = getElement(ListView.class, "recent_list");
        this.recentList.setOnMouseClicked(e ->
        {
            if (e.getClickCount() >= 2)
            {
                if (this.recentList.getSelectionModel().getSelectedItem() != null)
                {
                    this.databaseField.getStyleClass().remove("error");
                    String path = this.recentList.getSelectionModel().getSelectedItem().toString();
                    this.databaseField.setText(path);
                    this.continueButton.setDisable(false);
                    selectDatabase(path);
                }
            }
        });

        fillRecentDatabases();

        this.chooseButton = getElement(Button.class, "choose_button");
        this.chooseButton.setOnAction(e ->
        {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(this.stage);

            if (selectedDirectory != null)
            {
                this.databaseField.getStyleClass().remove("error");
                this.databaseField.setText(selectedDirectory.getAbsolutePath());
                this.continueButton.setDisable(false);
            }
        });

        this.continueButton = getElement(Button.class, "continue_button");
        this.continueButton.setOnAction(e ->
        {
            this.continueButton.setDisable(true);
            selectDatabase(this.databaseField.getText());
        });
    }

    private void fillRecentDatabases()
    {
        this.recentList.getItems().clear();

        var databases = this.db.select("database_path")
                               .from("recent_database")
                               .first(5)
                               .orderBy("timestamp").desc()
                               .onLessThan(1, (i, result) ->
                               {
                                   return result;
                               })
                               .execute();

        for (SqlResult result : databases)
        {
            this.recentList.getItems().add(result.get("database_path"));
        }
    }

    private void selectDatabase(String dbPath)
    {
        if (new File(dbPath).exists())
        {
            this.databaseField.getStyleClass().remove("error");
            var config = new DatabaseConfiguration().path(dbPath)
                                                    .autoReconnect();

            this.db.insert().into("recent_database")
                   .set("database_path", dbPath)
                   .onDuplicateKey(this.db.update("recent_database")
                                          .set("timestamp", SqlValue.SYSTIMESTAMP, SqlType.TIMESTAMP)
                                          .where("database_path").equals(dbPath)
                                          .commit())
                   .commit()
                   .execute();

            this.db.kill();

            try
            {
                ActiveDatabase database = new ActiveDatabase(config);

                database.executeQuery("select tablename from sys.systables");

                ViewManager.get().addView(DataView.class, new DataView(database));
                ViewManager.get().setView(DataView.class);
            }
            catch (Exception e)
            {
                this.continueButton.setDisable(true);
                this.databaseField.getStyleClass().add("error");

                config = new DatabaseConfiguration().path("./db")
                                                    .create()
                                                    .useUnicode()
                                                    .characterEncoding("utf8")
                                                    .autoReconnect();

                this.db = new RecentDatabase(config);
                this.db.delete()
                       .from("recent_database")
                       .where("database_path").equals(dbPath)
                       .commit()
                       .execute();
            }
        }
        else
        {
            this.continueButton.setDisable(true);
            this.databaseField.getStyleClass().add("error");
        }
    }

    /**
     * @see bt.gui.fx.core.FxView#prepareStage(javafx.stage.Stage)
     */
    @Override
    protected void prepareStage(Stage stage)
    {
        this.stage = stage;
        stage.setTitle("Select a database");
        stage.setResizable(false);
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