// Copyright (c) 2017 PSForever
package net.psforever.objects.serverobject.pad.process

import akka.actor.Props
import net.psforever.objects.serverobject.pad.{VehicleSpawnControl, VehicleSpawnPad}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * An `Actor` that handles vehicle spawning orders for a `VehicleSpawnPad`.
  * The basic `VehicleSpawnControl` is the root of a simple tree of "spawn control" objects that chain to each other.
  * Each object performs on (or more than one related) actions upon the vehicle order that was submitted.<br>
  * <br>
  * This object is the first link in the process chain that spawns the ordered vehicle.
  * It is devoted to causing the prospective driver to become hidden during the first part of the process
  * with the goal of appearing to be "teleported" into the driver mount.
  * It has failure cases should the driver be in an incorrect state.
  * @param pad the `VehicleSpawnPad` object being governed
  */
class VehicleSpawnControlConcealPlayer(pad: VehicleSpawnPad) extends VehicleSpawnControlBase(pad) {
  def LogId = "-concealer"

private val loadVehicle =
    context.actorOf(Props(classOf[VehicleSpawnControlLoadVehicle], pad), s"${context.parent.path.name}-load")

  def receive: Receive = {
    case order @ VehicleSpawnControl.Order(driver, vehicle) =>
      if (VehicleSpawnControl.validateOrderCredentials(pad, driver, vehicle).isEmpty) {
        trace(s"hiding ${driver.Name}")
        pad.Zone.VehicleEvents ! VehicleSpawnPad.ConcealPlayer(driver.GUID)
        context.system.scheduler.scheduleOnce(2000 milliseconds, loadVehicle, order)
      } else {
        trace(s"integral component lost; abort order fulfillment")
        context.parent ! VehicleSpawnControl.ProcessControl.OrderCancelled
      }

    case msg @ (VehicleSpawnControl.ProcessControl.Reminder |
                VehicleSpawnControl.ProcessControl.GetNewOrder |
                VehicleSpawnControl.ProcessControl.OrderCancelled) =>
      context.parent ! msg

    case _ => ;
  }
}
