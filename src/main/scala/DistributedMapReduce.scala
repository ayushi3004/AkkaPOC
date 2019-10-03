import java.io._
import akka.actor._
import scala.io.Source

case class ProcessDirectory(name: String)
case class ProcessFile(name: String)

case class MapperOutput(countList: List[(String, Int)])
case class WordGroup(word: String, countList: List[(String, Int)])
case class ReducerOutput(word: String, count: Int)

class Mapper() extends Actor() {

  def receive = {
    case ProcessFile(name) =>
      println(s"Processing $name")
      val lines = Source.fromFile(name).getLines()
      val eachFileWordMap = lines.foldLeft(List.empty[(String, Int)]) {
        (aggList, line) =>
          val m = map(line)
          m ::: aggList
      }

      sender ! MapperOutput(eachFileWordMap)
  }

  def map(line: String) = {
    val words = line.split(" ")
    val cleanWords = processWords(words.toList)
    cleanWords.map(part => (part, 1.toInt))
  }

  def processWords(words: List[String]) = {
    val punc = Seq(".", ";", ";", "?", "!", "\'", "\\")
    words.map { word =>
      word.trim.toLowerCase
        .stripPrefix(punc.find(x => word.contains(x)).getOrElse(""))
        .stripSuffix(punc.find(x => word.contains(x)).getOrElse(""))
    }
  }
}

class Reducer() extends Actor() {

  def receive = {
    case WordGroup(word, countList) =>
      val (_, wordCount) = reduce(word, countList)
      sender ! ReducerOutput(word, wordCount)
  }

  def reduce(word: String, countList: List[(String, Int)]) = {
    val sum = countList.map(_._2).sum
    (word, sum)
  }
}

class MapReduceFramework() extends Actor() {
  val start = System.nanoTime()
  val userRef =
    context.system.actorOf(Props(new Mapper))

  var pending = 0
  var completeData: List[(String, Int)] = Nil

  def receive = {
    case ProcessDirectory(name) =>
      val children = new File(name).listFiles()
      if (children != null) {
        children
          .filter(!_.isDirectory())
          .foreach { f =>
            userRef ! ProcessFile(f.getAbsolutePath)
            pending += 1
          }
      }

    case MapperOutput(lst) =>
      pending = pending - 1
      completeData = lst ::: completeData

      if (pending == 0) {
        println("Obtained mapper output. Starting shuffling and reducing...")

        // Shuffling
        val groupedByWord = completeData.groupBy(t => t._1)

        val summarizerRef =
          context.system.actorOf(Props(new Reducer))

        groupedByWord.foreach {
          case (word, countList) => {
            summarizerRef ! WordGroup(word, countList)
            pending += 1
          }
        }
      }

    case ReducerOutput(word, count) =>
      pending -= 1
      println(f"$word $count")

      if (pending == 0) {
        context.system.terminate()
        val end = System.nanoTime()
        println(s"Total time taken is ${(end - start) / 1.0e9} seconds")
        println(s"${completeData.length} words found")
      }
  }
}

object DistributedMapReduce {

  def main(args: Array[String]) = {
    val dataPath = "/Users/ayushi.sharma/Documents/sampleSpace/POC/src/main/resources/"

    val system = ActorSystem("Aggregator")
    val act = system.actorOf(Props[MapReduceFramework])
    act ! ProcessDirectory(dataPath)
  }

}
