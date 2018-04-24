package ui

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import java.net.URL
import java.util.*


class SplashController : Initializable {
    companion object {
        var instance: SplashController? = null
        fun notifyProgress(progress: Double, text: String) {
            Platform.runLater {
                instance?.progressBar?.progress = progress
                instance?.messageLabel?.text = text
            }
        }
    }


    @FXML
    lateinit var progressBar: ProgressBar
    @FXML
    lateinit var messageLabel: Label

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        instance = this
    }

}