package mplay

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, HBox}
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.media.{Media, MediaPlayer}
import scalafx.collections.ObservableBuffer
import scalafx.stage.DirectoryChooser
import scalafx.event.ActionEvent

import scala.io.Source
import java.nio.file.{Paths, Files}
import java.io._


object MPlay extends JFXApp {

  val cacheLog = ".cache"
  var selectedFile: String = _;
  var mediaPlayer: MediaPlayer = _;
  val musicFiles = findMusicFiles(new File("."))
  val cachedMedia = ObservableBuffer[Music](loadCachedPaths())

  stage = new JFXApp.PrimaryStage {
    title = "MPlayer"
    scene = new Scene(1000, 600) {
      val border = new BorderPane
      val label = new Label("Current media: ")
      val table = new TableView[Music](cachedMedia) {
        columns ++= List(
          new TableColumn[Music, String] {
            text = "Name"
            cellValueFactory = {_.value.name}
          },
          new TableColumn[Music, String] {
            text = "Path"
            cellValueFactory = {_.value.path}
          }
        )
        rowFactory = _ => {
          new TableRow[Music] {
            onMouseClicked = (event: MouseEvent) => {
              if (event.getClickCount == 2 && (!this.empty())) {
                createMediaPlayer(this.item.value.path.getValue)
                mediaPlayer.play()
              }
            }
          }
        }
      }
      val hbox = new HBox {
        val playButton = new Button("Play") {
          onAction = (event: ActionEvent) =>  {
            val model = table.selectionModel()
            if (!model.isEmpty) {
              val path = model.selectedItem.value.path.getValue
              if (path != selectedFile) {
                createMediaPlayer(path)
              }
              mediaPlayer.play()
            }
          }
        }
        val pauseButton = new Button("Pause") {
          onAction = _ => {mediaPlayer.pause()}
        }
        val stopButton = new Button("Stop") {
          onAction = _ => {mediaPlayer.stop()}
        }
        val importButton = new Button("Import") {
          onAction = _ => {
            val directoryChooser = new DirectoryChooser {
              title = "Open Resource File"
            }
            val selectedDir = directoryChooser.showDialog(stage)
            if (selectedDir != null) {
              val ss: Set[Music] = table.items().toSet
              val newSongs = findMusicFiles(selectedDir)
                .map(x => new Music(x.getCanonicalPath))
                .filter(!ss.contains(_))
              cacheMusicPaths(newSongs)
              table.items() ++= newSongs
            }
          }
        }
        spacing = 4.0
        alignment = Pos.CenterLeft
        children = List(playButton, pauseButton, stopButton, importButton)
      }

      // Play next song when finished
      def playNext(): Unit = {
        Thread.sleep(1000)
        val model = table.selectionModel()
        model.selectNext()
        val path = model.selectedItem.value.path.getValue
        createMediaPlayer(path)
        mediaPlayer.play()
      }

      def createMediaPlayer(path: String): Unit = {
        label.text = s"Current media: $path"
        selectedFile = path
        val hit = new Media(new File(path).toURI.toString)
        mediaPlayer = new MediaPlayer(hit)
        mediaPlayer.onEndOfMedia = {
          playNext()
        }
      }

      // Layout
      border.top = label
      border.center = table
      border.bottom = hbox
      root = border
    }
  }

  def cacheMusicPaths(newSongs: List[Music]): Unit = {
    val file = new File(cacheLog)
    if (!file.exists()) {
      file.createNewFile()
    }
    val fw = new FileWriter(cacheLog, true)
    newSongs.map(x => {fw.write(x.path_ + "\n")})
    fw.close()
  }

  def loadCachedPaths(): List[Music] = {
    val file = new File(cacheLog)
    if (file.exists())
      Source.fromFile(file).getLines.toList.map(Music(_))
    else
      List[Music]()
  }

  def findMusicFiles(file: File): List[File] = {
    if (file.isDirectory)
      file.listFiles.foldLeft(List[File]())((b, a) => b ++ findMusicFiles(a))
    else if (isMusicFile(file))
      List(file)
    else
      List[File]()
  }

  def isMusicFile(file: File): Boolean =
    file.isFile && List(".mp3", ".wav").exists(file.toString.endsWith)
}