import scala.util.Random

/***
  * Idea is to demonstrate what could go wrong in a concurrent system.
  * Scenario: There is a bank which has multiple, say 10 accounts in it. Each account has 100$ initially. They all transfer <=10$ to each other.
  * So by the end of the day, the total amount in the system should remain 1000$, but as we see here it is not the case.
  */
class Account {
  var balance: Int = 0

  def deposit(amount: Int) = {
    balance = balance + amount
  }

  def withdraw(amount: Int) =  synchronized {
    balance = balance - amount
  }
}

class Bank {
  val MAX_AMOUNT = 10
  val MAX_ACCOUNTS = 10
  val INITIAL_BALANCE = 100

  val accounts = Array.fill[Account](MAX_ACCOUNTS)(new Account)

  def inititalizeAccounts() = {
    accounts.foreach { account =>
      account.balance = INITIAL_BALANCE
    }
  }


  def transfer(from: Int, to: Int, amount: Int) =  synchronized {
    if (accounts(from).balance >= amount) {
      accounts(from).withdraw(amount)
      accounts(to).deposit(amount)

      println(
        s"${Thread.currentThread().getName} Transferred $amount dollars from account $from to account $to. Total balance - $getSystemBalance"
      )
    }
  }

  def getSystemBalance() = {
    var total = 0

    accounts.foreach { account =>
      total += account.balance
    }
    total
  }
}

class Transaction(bank: Bank, from: Int) extends Runnable {
  override def run(): Unit = {
    while (true) {

      val to = Random.nextInt(bank.MAX_ACCOUNTS) // take any to account

      if (to != from) {
        val amount = 1 + Random.nextInt(bank.MAX_AMOUNT) // transfer any amount between 1-10

        bank.transfer(from, to, amount)
      }

      try {
        Thread.sleep(50)
      } catch {
        case e: Exception => println(s"Error: $e")
      }
    }
  }
}

object Transaction {
  def main(args: Array[String]): Unit = {
    val bank = new Bank
    bank.inititalizeAccounts()
    for (i <- 0 until bank.MAX_AMOUNT) {
      val t = new Thread(new Transaction(bank, i))
      t.start()
    }
  }
}