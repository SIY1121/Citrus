package ui

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.stage.Stage
import kotlinx.coroutines.experimental.launch
import project.ProjectRenderer
import java.time.Duration

class EncodingProgress {

    @FXML
    lateinit var progressBar: ProgressBar
    @FXML
    lateinit var textArea: TextArea
    @FXML
    lateinit var timeLabel: Label

    var start = 0L

    private val callback = object : ProjectRenderer.EncodingInfoCallback {
        override fun onInfo(msg: String) {
            Platform.runLater { textArea.appendText(msg + "\n") }
        }

        override fun onProgress(progress: Int, max: Int) {
            //println("progress $progress / $max")
            Platform.runLater { progressBar.progress = progress.toDouble() / max }

        }

        override fun onFinish() {
            Platform.runLater {
                Alert(Alert.AlertType.INFORMATION, "エンコードが完了しました", ButtonType.OK).show()
                (progressBar.scene.window as Stage).close()
            }
        }
    }

    fun init(projectRenderer: ProjectRenderer) {
        start = System.currentTimeMillis()
        projectRenderer.startEncode(callback)
        launch {
            while (progressBar.progress < 1.0) {
                Thread.sleep(1000)
                Platform.runLater { timeLabel.text = "経過時間 ${(System.currentTimeMillis() - start) / 1000L}s" }

            }
        }
    }
}