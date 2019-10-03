import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.ExecutionContexts._
import akka.pattern.ask
import akka.util.Timeout

import scala.io.Source._

/***
  *
  * Given a file, count the no of words in it.
  */
case class ProcessLine(line: String)
case class WordsCounted(words: Int)

class StringCounterActor extends Actor {
  def receive = {
    case ProcessLine(line) =>
      val wordsInLine = line.split(" ").length
      sender ! WordsCounted(wordsInLine)

    case _ => println("Error: message not recognized")
  }
}

class FileIterator(filename: String) extends Actor {
  private var totalLines = 0
  private var linesProcessed = 0
  private var result = 0
  private var rootActor: Option[ActorRef] = None

  def receive = {
    case Start =>
      rootActor = Some(sender) // Rmbr the sender to reply(main actor)
      fromFile(filename).getLines.foreach { line =>
        val secondRef = context.actorOf(Props[StringCounterActor])
        println(s"Second ref: $secondRef")
        secondRef ! ProcessLine(line)
        totalLines += 1
      }

    case WordsCounted(num) =>
      result += num
      linesProcessed += 1

      if (linesProcessed == totalLines)
        rootActor.get ! result

    case _ => println("Message not recognized!")
  }
}

case object Start

object WordCount {

  implicit val ec = global
  implicit val timeout = Timeout(25, TimeUnit.SECONDS)

  def main(args: Array[String]) {
    val system: ActorSystem = ActorSystem("System")
    val firstRef = system.actorOf(
      Props(new FileIterator("/Users/ayushi.sharma/Documents/sampleSpace/POC/src/main/resources/cups.txt"))
    )
    println(s"First ref: $firstRef")

    val future = firstRef ? Start
    future.map { result =>
      println("Total number of words " + result)
    }
    system.terminate()
  }
}
