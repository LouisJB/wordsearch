import math._
import util._
import org.jline.terminal.TerminalBuilder
import java.awt.Color
import scala.io.Source._

object WordSearch {
  val defaultSize = 40
  val showCheat = false
  val rand = new Random()

  def main(args: Array[String]): Unit =
    run(Try(args(0).toInt).getOrElse(defaultSize))
  
  def run(size: Int = defaultSize): Unit = {
    // put term in raw char by char mode
    val terminal = TerminalBuilder.terminal()
    terminal.enterRawMode()
    val reader = terminal.reader()

    val file = new java.io.File(".", "/words")
    val words = if (file.exists)
      fromFile(file.getAbsolutePath).getLines.toSet
    else
      Set("globe", "earth", "atmosphere", "cloud", "snow", "wind", "star").map(_.toUpperCase())

    val grid: Array[Array[Letter]] = Array.fill[Letter](size, size)(Letter('.'))

    map(grid) { (_, _) => 
      val rnd = randInt(26)
      val l = ('A' + rnd).toChar
      Letter(l)
    }
    val randPos = randomPosition(size)
    
    assert(words.map(_.length <= size).forall(_ == true), s"words too long, max is $size")
    val placedWords: Set[Word] = words.foldLeft(Set[Word]()) { case (set, w) =>
      val or = randomOrientation
      var newWord = Word(w, or, randPos(w, or))
      while (set.map(_.collided(newWord)).exists(_ == true)) {
        //println(s"collision for $newWord relocating")
        newWord = Word(w, or, randPos(w, or))
      }
      set + newWord
    }

    clrScr()

    if (showCheat)
      placedWords.foreach(pw => println(pw.toString() + " " + pw.letterPositions()))
    
    addPlacedWords(placedWords.map(_.letterPositions()), grid)
    showGrid(grid)
    showWords(words)

    val startTimeMs = System.currentTimeMillis()
    def elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000
    var done = false
    var revealMode = false
    while (!done) {
      print(s"\r" + Console.YELLOW + s" time: ${elapsedSeconds}s")
      Thread.sleep(2000)

      clrScr()
      val colour = if (revealMode)
        Console.RED
      else
        Console.WHITE

      placedWords.map { w =>
        w.letterPositions(Some(colour)).map( l => grid(l.xy.y)(l.xy.x) = l.letter)
      }
      addPlacedWords(placedWords.map(_.letterPositions(Some(colour))), grid)
      showGrid(grid)
      showWords(words)
      println()

      val char = reader.read() match {
        case 'r' =>
          revealMode = !revealMode
        case 'x' =>
          done = true
        case _ =>
      }
    }
    println(s"Finished after ${elapsedSeconds}s")
  }

  def clrScr() = println("\u001b[2J")

  def showGrid(grid: Array[Array[Letter]]): Unit = {
    print(Console.WHITE)
    grid.foreach { r =>
      val line = r.map(l => l.colourCode.getOrElse("") + l.c + Console.WHITE).mkString(" ")
      println(line)
    }
  }

  def addPlacedWords(wordLettersAt: Set[Seq[LetterAt]], grid: Array[Array[Letter]]) =
    wordLettersAt.map(_.map(l => grid(l.xy.y)(l.xy.x) = l.letter))

  def showWords(words: Set[String]) = {
    println()
    println("Words are:\n")
    words.foreach(println)
    println()
    println("Happy Searching, good luck!")
    println
    println("press r to reveal answer, x to end or any key to display updated time")
    println
  }

  def map[A](grid: Array[Array[A]])(fn: (x: Int, y: Int) => A): Unit = {
    (0 until grid.length).foreach { y =>
      (0 until grid(0).length) foreach { x =>
        grid(y)(x) = fn(x, y)
      }
    }
  }

  def randomOrientation =
    Orientation.all.toSeq(randInt(Orientation.all.size))

  def randomPosition(size: Int)(word: String, orientation: Orientation) = {
    orientation match {
      case LR | RL =>
        val range = size - word.length()
        XY(randInt(range), randInt(size))
      case TB | BT =>
        val range = size - word.length()
        XY(randInt(size), randInt(range))
      case LRD | LRU | RLD | RLU =>
        val range = size - word.length()
        XY(randInt(range), randInt(range))
    }
  }

  def randInt: Int = abs(rand.nextInt())
  def randInt(m: Int): Int = randInt % m
}

trait Orientation
object Orientation {
  val all = Set(LR, RL, TB, BT, LRD, LRU, RLD, RLU)
}
case object LR extends Orientation
case object RL extends Orientation

case object TB extends Orientation
case object BT extends Orientation

case object LRD extends Orientation
case object LRU extends Orientation

case object RLD extends Orientation
case object RLU extends Orientation

case class XY(x: Int, y: Int) {
  def x_(newX: Int) = this.copy(x = newX)
  def y_(newY: Int) = this.copy(y = newY)
}
case class Letter(c: Char, colourCode: Option[String] = None) {
  def toLower = Letter(c.toLower)
}
case class LetterAt(letter: Letter, xy: XY)
case class Word(word: String, orientation: Orientation, location: XY) {
  def letterPositions(colourCode : Option[String] = None): Seq[LetterAt] = {
    val wordDir = orientation match {
      case RL | BT | RLU | RLD => word.reverse
      case _ => word
    }
    orientation match {
      case LR | RL =>
        wordDir.zipWithIndex.map { (c, idx) =>
          LetterAt(Letter(c, colourCode), location.x_(location.x + idx))
        }
      case TB | BT =>
        wordDir.zipWithIndex.map { (c, idx) =>
          LetterAt(Letter(c, colourCode), location.y_(location.y + idx))
        }
      case LRD | RLU =>
        wordDir.zipWithIndex.map { (c, idx) =>
          LetterAt(Letter(c, colourCode), XY(location.x + idx, location.y + idx))
        }
      case LRU | RLD =>
        wordDir.zipWithIndex.map { (c, idx) =>
          LetterAt(Letter(c, colourCode), XY(location.x + idx, word.length + location.y - idx))
        }
    }
  }
  def collided(other: Word) = {
    val otherPositionsSet: Set[XY] = other.letterPositions().map(_.xy).toSet
    (letterPositions().map(_.xy).toSet intersect (otherPositionsSet)).nonEmpty
  }
}
