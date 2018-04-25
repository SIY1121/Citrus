package ui

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.stage.Modality
import javafx.stage.Stage
import util.Statics
import java.net.URL
import java.util.*

class Welcome : Initializable {

    @FXML
    lateinit var listView: ListView<String>

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        listView.items.addAll("project1", "project2")
    }

    fun clickNewProject(actionEvent: ActionEvent) {
        val dialog = WindowFactory.createWindow("layout/createProject.fxml")
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(listView.scene.window)
        dialog.showAndWait()

        if (Statics.project.initialized)
            listView.scene.window.hide()
    }

    fun clickHelp(actionEvent: ActionEvent) {
        val stage = Stage()
        stage.scene = Scene(FXMLLoader.load<Parent>(ClassLoader.getSystemResource("layout/about.fxml")))
        stage.isResizable = false
        stage.title = "Citrusについて"
        stage.initOwner(listView.scene.window)
        stage.initModality(Modality.WINDOW_MODAL)
        stage.show()
    }
}