//import java.util.concurrent.TimeUnit
//
//import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//import akka.event.LoggingReceive
//import akka.util.Timeout
//
//class BankAccount extends Actor {
//    import BankAccount._
//    var balance = BigInt(10)
//
//    def receive = LoggingReceive {
//      case Deposit(amount) =>
//        balance += amount
//        sender ! Done
//      case Withdraw(amount) =>
//        balance -= amount
//        sender ! Done
//      case _ => sender ! Failed
//
//    }
//  }
//
//object BankAccount {
//    case class Withdraw(amount: BigInt) {
//      require(amount > 0)
//    }
//
//    case class Deposit(amount: BigInt) {
//      require(amount > 0)
//    }
//
//    case object Done
//    case object Failed
//  }
//
////Class representing wire transfer:
//
//object WireTransfer {
//    case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
//    case object Done
//    case object Failed
//  }
//
//class WireTransfer extends Actor {
//    import WireTransfer._
//
//    def receive = {
//      case Transfer(from, to, amount) =>
//        from ! BankAccount.Withdraw(amount)
//        context.become(awaitWithdraw(to, amount, sender))
//    }
//
//    def awaitTo(customer: ActorRef) : Receive= LoggingReceive {
//      case BankAccount.Done =>
//        customer ! Done
//        context.stop(self)
//      case _ => sender ! Failed
//    }
//
//    def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive =
//      LoggingReceive {
//        case BankAccount.Done =>
//          to ! BankAccount.Deposit(amount)
//          context.become(awaitTo(client))
//        case BankAccount.Failed =>
//          client ! Failed
//          context.stop(self)
//      }
//
//  }
//
////Main class
//
//class TransferMain extends Actor {
//    val accountA: ActorRef = context.actorOf(Props[BankAccount], "accountA")
//    val accountB = context.actorOf(Props[BankAccount], "accountB")
//    accountA ! BankAccount.Deposit(50)
//
//    def receive = LoggingReceive {
//      case BankAccount.Done => transfer(10)
//    }
//
//    def transfer(amount: BigInt): Unit = {
//      val transaction = context.actorOf(Props[WireTransfer], "transfer")
//      transaction ! WireTransfer.Transfer(accountA, accountB, amount)
//      context.become(LoggingReceive {
//        case WireTransfer.Done =>
//          println("Success")
//          context.stop(self)
//      })
//    }
//  }
//
//object TransferMain{
//  def main(args: Array[String]): Unit = {
//    val system = ActorSystem("System")
//    val actor = system.actorOf(Props(new TransferMain))
//    implicit val timeout = Timeout(25, TimeUnit.SECONDS)
//    val future = actor ? BankAccount
//    future.map { result =>
//      println("Total number of words " + result)
//      system.shutdown
//    }
//  }
//}