package com.lightbend.training.coffeehouse

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}

import scala.concurrent.duration.FiniteDuration

object Guest {

  case object CoffeeFinished

  case object CaffeineException extends IllegalStateException

  def props(waiter: ActorRef, favoriteCoffee: Coffee, finishCoffeeDuration: FiniteDuration, caffeineLimit: Int): Props =
    Props(new Guest(waiter, favoriteCoffee, finishCoffeeDuration, caffeineLimit))
}

class Guest(waiter: ActorRef, favoriteCoffee: Coffee, finishCoffeeDuration: FiniteDuration, caffeineLimit: Int) extends Actor with ActorLogging with
  Timers {

  import Guest._

  orderCoffee()

  private var coffeeCount: Int = 0

  private def orderCoffee(): Unit = {
    waiter ! Waiter.ServeCoffee(favoriteCoffee)
  }

  override def receive: Receive = {
    case Waiter.CoffeeServed(`favoriteCoffee`) =>
      coffeeCount += 1
      log.info(s"Enjoying my $coffeeCount yummy $favoriteCoffee!")
      timers.startSingleTimer("timer", CoffeeFinished, finishCoffeeDuration)
    case Waiter.CoffeeServed(otherCoffee) =>
      log.info(s"Expected a $favoriteCoffee, but got a $otherCoffee!")
      waiter ! Waiter.Complaint(favoriteCoffee)
    case CoffeeFinished if coffeeCount > caffeineLimit => throw CaffeineException
    case CoffeeFinished => orderCoffee()
  }

  override def postStop(): Unit = {
    log.info("Goodbye!")
    super.postStop()
  }
}
