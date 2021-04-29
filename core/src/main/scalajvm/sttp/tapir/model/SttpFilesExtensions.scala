package sttp.tapir.model

import java.nio.file.{Files, Path}

trait SttpFileExtensions { self: SttpFile =>
  def toPath: Path = underlying.asInstanceOf[Path]
  def toFile: java.io.File = toPath.toFile
}

trait SttpFileCompanionExtensions {
  def fromPath(path: Path): SttpFile =
    new SttpFile(path) {
      val name: String = path.getFileName.toString
      def size: Long = Files.size(path)
    }
  def fromFile(file: java.io.File): SttpFile = fromPath(file.toPath)
}