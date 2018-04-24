package ui

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Spinner
import util.Statics

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
        Statics.project.width = widthSpinner.value
        Statics.project.height = heightSpinner.value
        Statics.project.fps = fpsSpinner.value
        Statics.project.initialized = true
        widthSpinner.scene.window.hide()
    }
}