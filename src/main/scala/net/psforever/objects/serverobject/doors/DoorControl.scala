// Copyright (c) 2017 PSForever
package net.psforever.objects.serverobject.doors

import net.psforever.objects.Player
import net.psforever.objects.serverobject.{CommonMessages, PlanetSideServerObject}
import net.psforever.objects.serverobject.affinity.{FactionAffinity, FactionAffinityBehavior}
import net.psforever.objects.serverobject.locks.IFFLock
import net.psforever.objects.serverobject.structures.PoweredAmenityControl
import net.psforever.services.Service
import net.psforever.services.local.{LocalAction, LocalResponse, LocalServiceMessage, LocalServiceResponse}

/**
  * An `Actor` that handles messages being dispatched to a specific `Door`.
  * @param door the `Door` object being governed
  */
class DoorControl(door: Door)
  extends PoweredAmenityControl
  with FactionAffinityBehavior.Check {
private def FactionObject: FactionAffinity = door
private var isLocked: Boolean = false
private var lockingMechanism: Door.LockingMechanismLogic = DoorControl.alwaysOpen

private def commonBehavior: Receive = checkBehavior
    .orElse {
      case Door.Lock =>
        isLocked = true
        if (door.isOpen) {
          val zone = door.Zone
          door.Open = None
          zone.LocalEvents ! LocalServiceMessage(zone.id, LocalAction.DoorSlamsShut(door))
        }

      case Door.Unlock =>
        isLocked = false

      case Door.UpdateMechanism(logic) =>
        lockingMechanism = logic
    }

private def poweredStateLogic: Receive =
    commonBehavior
      .orElse {
        case CommonMessages.Use(player, _) =>
          if (lockingMechanism(player, door) && !isLocked) {
            openDoor(player)
          }

        case IFFLock.DoorOpenResponse(target: Player) if !isLocked =>
          openDoor(target)

        case _ => ;
      }

private def unpoweredStateLogic: Receive = {
    commonBehavior
      .orElse {
        case CommonMessages.Use(player, _) if !isLocked =>
          //without power, the door opens freely
          openDoor(player)

        case _ => ;
      }
  }

private def openDoor(player: Player): Unit = {
    val zone = door.Zone
    val doorGUID = door.GUID
    if (!door.isOpen) {
      //global open
      door.Open = player
      zone.LocalEvents ! LocalServiceMessage(
        zone.id,
        LocalAction.DoorOpens(Service.defaultPlayerGUID, zone, door)
      )
    }
    else {
      //the door should already open, but the requesting player does not see it as open
      sender() ! LocalServiceResponse(
        player.Name,
        Service.defaultPlayerGUID,
        LocalResponse.DoorOpens(doorGUID)
      )
    }
  }

  override def powerTurnOffCallback() : Unit = { }

  override def powerTurnOnCallback() : Unit = { }
}

object DoorControl {
private def alwaysOpen(obj: PlanetSideServerObject, door: Door): Boolean = true
}
