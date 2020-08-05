package com.lightbend.training.coffeehouse

import java.util.concurrent.TimeUnit.MILLISECONDS

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.routing.FromConfig

import scala.concurrent.duration._

object CoffeeHouse {

  case class CreateGuest(favoriteCoffee: Coffee, caffeineLimit: Int)

  case class ApproveCoffee(coffee: Coffee, guest: ActorRef)

  case object GetStatus

  case class Status(guestCount: Int)

  def props(caffeineLimit: Int): Props = Props(new CoffeeHouse(caffeineLimit))
}

class CoffeeHouse(caffeineLimit: Int) extends Actor with ActorLogging {

  import CoffeeHouse._

  log.debug("CoffeeHouse Open")

  private val finishCoffeeDuration =
    context.system.settings.config.getDuration("coffee-house.guest.finish-coffee-duration", MILLISECONDS).millis
  private val prepareCoffeeDuration =
    context.system.settings.config.getDuration("coffee-house.barista.prepare-coffee-duration", MILLISECONDS).millis
  private val baristaAccuracy = context.system.settings.config.getInt("coffee-house.barista.accuracy")
  private val waiterMaxComplaintCount = context.system.settings.config.getInt("coffee-house.waiter.max-complaint-count")
  private val barista: ActorRef = createBarista
  private val waiter = createWaiter
  private var guestBook: Map[ActorRef, Int] = Map.empty.withDefaultValue(0)

  protected def createBarista: ActorRef = context.actorOf(FromConfig.props(Barista.props(prepareCoffeeDuration, baristaAccuracy)), "barista")

  protected def createWaiter: ActorRef = context.actorOf(Waiter.props(self, barista, waiterMaxComplaintCount), "waiter")

  protected def createGuest(favoriteCoffee: Coffee, caffeineLimit: Int): ActorRef =
    context.actorOf(Guest.props(waiter, favoriteCoffee, finishCoffeeDuration, caffeineLimit))

  override def receive: Receive = {
    case CreateGuest(favoriteCoffee, caffeineLimit) =>
      val guest = createGuest(favoriteCoffee, caffeineLimit)
      context.watch(guest)
      guestBook += guest -> 0
      log.info(s"Guest $guest added to guest book")
    case ApproveCoffee(coffee, guest) if (guestBook(guest) < caffeineLimit) =>
      guestBook += guest -> (guestBook(guest) + 1)
      log.info(s"Guest $guest caffeine count incremented.")
      barista.forward(Barista.PrepareCoffee(coffee, guest))
    case ApproveCoffee(_, guest) =>
      log.info(s"Sorry, $guest, but you have reached your limit.")
      context.stop(guest)
    case Terminated(guest) =>
      guestBook -= guest
      log.info(s"Thanks $guest, for being our guest!")
    case GetStatus => sender() ! Status(guestBook.size)
    case _ => sender() ! "Coffee Brewing"
  }

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: SupervisorStrategy.Decider = {
      case Guest.CaffeineException => SupervisorStrategy.Stop
      case Waiter.FrustratedException(coffee, guest) =>
        barista.forward(Barista.PrepareCoffee(coffee, guest))
        SupervisorStrategy.Restart
    }

    OneForOneStrategy()(decider.orElse(super.supervisorStrategy.decider))
  }
}
