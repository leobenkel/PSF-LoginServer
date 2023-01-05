// Copyright (c) 2020 PSForever
package net.psforever.objects.serverobject.structures

import akka.actor.Actor
import net.psforever.actors.zone.BuildingActor

trait PoweredAmenityControl extends Actor {
  private var powered: Boolean = true

  final def receive: Receive = powerOnCondition

  final def powerOnCondition: Receive = {
    case BuildingActor.PowerOff() =>
      powered = false
      context.become(powerOffCondition)
      powerTurnOffCallback()
    case msg =>
      poweredStateLogic.apply(msg)
  }

  final def powerOffCondition: Receive = {
    case BuildingActor.PowerOn() =>
      powered = true
      context.become(powerOnCondition)
      powerTurnOnCallback()
    case msg =>
      unpoweredStateLogic.apply(msg)
  }

private def isPowered: Boolean = powered

private def poweredStateLogic: Receive

private def unpoweredStateLogic: Receive

private def powerTurnOnCallback(): Unit

private def powerTurnOffCallback(): Unit
}
