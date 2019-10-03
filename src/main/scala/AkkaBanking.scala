import akka.actor._
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

trait Failure

trait Success

case class CreateAccount(accountNo: Int, amt: Int)

case class Deposit(amount: Int)

case class Withdraw(amount: Int)

case class Transfer(from: Int, to: Int, amount: Int)

case class WithdrawSuccess() extends Success

case class CreateSuccess() extends Success

case class CreateFailure() extends Failure

case class WithdrawFailure() extends Failure

case class DepositSuccess() extends Success

case class DepositFailure() extends Failure

case class TransferSuccess() extends Success

case class TransferFailure() extends Failure

case object GetAccountBalance

case object GetSystemBalance

class AkkaAccount extends Actor {
  var balance: Int = 0

  override def receive: Receive = {

    case GetAccountBalance =>
      sender ! balance

    case Deposit(amount) =>
      balance = balance + amount
      sender() ! DepositSuccess

    case Withdraw(amount) =>
      if (amount > balance) {
        sender() ! WithdrawFailure
      } else {
        balance = balance - amount
        sender() ! WithdrawSuccess
      }

  }
}

class AkkaBank(implicit val ec: ExecutionContext, timeout: Timeout)
    extends Actor {
  override def receive: Receive = {
    case CreateAccount(accountNo: Int, initialAmt: Int) =>
      val system = sender()
      (createAccount(accountNo) ? Deposit(initialAmt)).map {
        case DepositSuccess => system ! CreateSuccess
      }

    case Transfer(to, from, amount) =>
      val system = sender()

      (getAccount(from) ? Withdraw(amount)).map {
        case WithdrawSuccess =>
          (getAccount(to) ? Deposit(amount)).map {
            case DepositSuccess => system ! TransferSuccess
            case DepositFailure => system ! TransferFailure
          }
        case WithdrawFailure => system ! TransferFailure
      }

    case GetSystemBalance =>
      val system = sender()
      val balancesFuture = context.children.map(getAccountBalance)
      val totalBalance = Future.sequence(balancesFuture).map(_.sum)

      totalBalance.map(system ! _)

  }

  def getAccount(accountNo: Int): ActorRef =
    context.child(s"account-$accountNo").get

  def createAccount(accountNo: Int) =
    context.actorOf(Props(new AkkaAccount), s"account-$accountNo")

  def getAccountBalance(child: ActorRef) = {
    (child ? GetAccountBalance).map { case balance: Int => balance }
  }
}

object AkkaTransaction {
  implicit val timeout = new Timeout(30 seconds)
  implicit val ec: ExecutionContext = ExecutionContext.global

  def transfer(bank: ActorRef, from: Int, to: Int, amount: Int) = {
    (bank ? Transfer(from, to, amount)).map {
      case TransferSuccess =>
        (bank ? GetSystemBalance).map { totalBalance =>
          println(
            s"Transferring $amount dollars from Account-$from to Account-$to. Total balance: $totalBalance"
          )
        }
      case TransferFailure =>
        println(
          s"Transfer of $amount dollars from Account-$from to Account-$to failed"
        )
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("Banking")
    val bank = system.actorOf(Props(classOf[AkkaBank], ec, timeout))

    Await.result(bank ? CreateAccount(1, 10), 10 seconds)
    Await.result(bank ? CreateAccount(2, 10), 10 seconds)
    Await.result(bank ? CreateAccount(3, 10), 10 seconds)
    Await.result(bank ? CreateAccount(4, 10), 10 seconds)

    //10 10 10 10
    transfer(bank, 1, 2, 9) // 1  19  10  10
    transfer(bank, 2, 4, 3) // 1  16  10  13
    transfer(bank, 3, 1, 8) // 9  16  2   13
    transfer(bank, 1, 3, 1) // 8  16  3   13
    transfer(bank, 2, 4, 2) // 8  14  3   15
    transfer(bank, 3, 4, 1) // 8  14  2   16
    transfer(bank, 4, 1, 5) // 13 14  2   11
    transfer(bank, 3, 1, 9) // -----X -----

    system.terminate()

  }
}
