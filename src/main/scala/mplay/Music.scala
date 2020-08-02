package mplay

import scalafx.beans.property.StringProperty
import org.apache.commons.io.FilenameUtils


case class Music(path_ : String) {
  val path = new StringProperty(this, "path", path_)
  val name = new StringProperty(this, "name", FilenameUtils.getBaseName(path_))
}
