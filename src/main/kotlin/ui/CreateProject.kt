package ui

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Spinner

class CreateProject {

    @FXML
    lateinit var widthSpinner : Spinner<Int>
    @FXML
    lateinit var heightSpinner : Spinner<Int>
    @FXML
    lateinit var fpsSpinner : Spinner<Int>
    @FXML
    lateinit var samplerateSpinner : Spinner<Int>

    fun onOkClicked(actionEvent: ActionEvent) {
        Main.project.width = widthSpinner.value
        Main.project.height = heightSpinner.value
        Main.project.fps = fpsSpinner.value
        Main.project.initialized = true
        widthSpinner.scene.window.hide()
    }
}