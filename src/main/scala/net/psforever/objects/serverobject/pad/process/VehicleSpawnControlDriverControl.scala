// Copyright (c) 2017 PSForever
package net.psforever.objects.serverobject.pad.process

import akka.actor.Props
import net.psforever.objects.serverobject.pad.{VehicleSpawnControl, VehicleSpawnPad}

/**
  * An `Actor` that handles vehicle spawning orders for a `VehicleSpawnPad`.
  * The basic `VehicleSpawnControl` is the root of a simple tree of "spawn control" objects that chain to each other.
  * Each object performs on (or more than one related) actions upon the vehicle order that was submitted.<br>
  * <br>
  * A certain amount of time after the server has asserted control over a newly-spawned vehicle,
  * control of that vehicle is given over to the driver.
  * It has failure cases should the driver be in an incorrect state.
  * @param pad the `VehicleSpawnPad` object being governed
  */
class VehicleSpawnControlDriverControl(pad: VehicleSpawnPad) extends VehicleSpawnControlBase(pad) {
  def LogId = "-overrider"

private val finalClear =
    context.actorOf(Props(classOf[VehicleSpawnControlFinalClearance], pad), s"${context.parent.path.name}-final")

  def receive: Receive = {
    case order @ VehicleSpawnControl.Order(driver, vehicle) =>
      trace(s"returning control of ${vehicle.Definition.Name} to its current driver")
      if (vehicle.PassengerInSeat(driver).nonEmpty) {
        pad.Zone.VehicleEvents ! VehicleSpawnPad.ServerVehicleOverrideEnd(driver.Name, vehicle, pad)
      }
      finalClear ! order

    case msg @ (VehicleSpawnControl.ProcessControl.Reminder | VehicleSpawnControl.ProcessControl.GetNewOrder) =>
      context.parent ! msg

    case _ => ;
  }
}
