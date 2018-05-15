package properties

import javafx.scene.layout.Pane

/**
 * キーフレームを実装したプロパティインターフェース
 */
interface CitrusAnimatableProperty<T>:CitrusProperty<T> {

    /**
     * キーフレーム編集用のエディットペイン
     */
    val editPane : Pane

    /**
     * タイムラインのスケールが変更されたときに呼び出される
     */
    fun onTimelineScaleChanged()

    /**
     * フレーム番号
     * セットすることで帰る値が確定する
     */
    var frame: Int

    /**
     * キーフレームのリスト
     */
    val keyFrames: MutableList<KeyFrame<T>>

    /**
     * 指定したフレームがキーフレームかを判定
     */
    fun isKeyFrame(frame: Int): Boolean {
        return keyFrames.any { it.frame == frame }
    }

    /**
     * 指定したフレームの直前のキーフレームのインデックスを返す
     */
    fun getKeyFrameIndex(frame: Int): Int {
        val r = keyFrames.lastOrNull { it.frame <= frame }
        return if (r != null) keyFrames.indexOf(r)
        else -1
    }
}