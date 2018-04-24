package annotation

/**
 * アノテーション定義
 */

/**
 * CitrusObjectのプロパティに付与するアノテーション
 * 付与されたプロパティは編集画面から制御できるようになる
 * @param displayName 実際に表示されるパラメーター名
 * @param index 表示される順番
 */
@Target(AnnotationTarget.PROPERTY)
annotation class CProperty(val displayName: String, val index: Int)

/**
 * このアノテーションを付与すると
 * 右クリック時に表示されるサブメニューに
 * ミニコントローラーが表示されるようになる
 */
@Target(AnnotationTarget.PROPERTY)
annotation class CQuickProperty(val displayName : String,val index : Int)

/**
 * CitrusObject自体に付与するアノテーション
 * そのオブジェクトの表示名を定義する意図もある
 * @param name 表示名
 */
@Target(AnnotationTarget.CLASS)
annotation class CObject(val name: String,val color : String = "757575FF" ,val iconUrl: String = "")

/***
 * キーフレーム補間クラスへの和名設定用
 */
@Target(AnnotationTarget.CLASS)
annotation class CInterpolation(val name: String)

/**
 * Citrus Objectでファイルドロップに反応するクラスに対して指定する
 * フィルタに指定した拡張子のファイルがドロップされた場合
 * onFireDroppedが発生する
 */
@Target(AnnotationTarget.CLASS)
annotation class CDroppable(val filter: Array<String>)