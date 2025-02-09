// Copyright (c) 2017 PSForever
package net.psforever.services.vehicle

import akka.actor.{Actor, ActorRef, Props}
import net.psforever.objects.serverobject.pad.VehicleSpawnPad
import net.psforever.objects.zones.Zone
import net.psforever.packet.game.ObjectCreateMessage
import net.psforever.packet.game.objectcreate.ObjectCreateMessageParent
import net.psforever.services.vehicle.support.TurretUpgrader
import net.psforever.types.DriveState
import net.psforever.services.{GenericEventBus, Service}

class VehicleService(zone: Zone) extends Actor {
  private val turretUpgrade: ActorRef = context.actorOf(Props[TurretUpgrader](), s"${zone.id}-turret-upgrade-agent")
  private[this] val log               = org.log4s.getLogger

private val VehicleEvents = new GenericEventBus[VehicleServiceResponse]

  def receive = {
    case Service.Join(channel) =>
      val path = s"/$channel/Vehicle"
      VehicleEvents.subscribe(sender(), path)

    case Service.Leave(None) =>
      VehicleEvents.unsubscribe(sender())

    case Service.Leave(Some(channel)) =>
      val path = s"/$channel/Vehicle"
      VehicleEvents.unsubscribe(sender(), path)

    case Service.LeaveAll() =>
      VehicleEvents.unsubscribe(sender())

    case VehicleServiceMessage(forChannel, action) =>
      action match {
        case VehicleAction.ChildObjectState(player_guid, object_guid, pitch, yaw) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.ChildObjectState(object_guid, pitch, yaw)
            )
          )
        case VehicleAction.DeployRequest(player_guid, object_guid, state, unk1, unk2, pos) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.DeployRequest(object_guid, state, unk1, unk2, pos)
            )
          )
        case VehicleAction.DismountVehicle(player_guid, bailType, unk2) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.DismountVehicle(bailType, unk2)
            )
          )
        case VehicleAction.EquipmentInSlot(player_guid, target_guid, slot, equipment) =>
          val definition = equipment.Definition
          val pkt = ObjectCreateMessage(
            definition.ObjectId,
            equipment.GUID,
            ObjectCreateMessageParent(target_guid, slot),
            definition.Packet.ConstructorData(equipment).get
          )
          ObjectCreateMessageParent(target_guid, slot)
          VehicleEvents.publish(
            VehicleServiceResponse(s"/$forChannel/Vehicle", player_guid, VehicleResponse.EquipmentInSlot(pkt))
          )
        case VehicleAction.FrameVehicleState(
          player_guid,
          vehicle_guid,
          unk1,
          pos,
          orient,
          vel,
          unk2,
          unk3,
          unk4,
          is_crouched,
          unk6,
          unk7,
          unk8,
          unk9,
          unkA
        ) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.FrameVehicleState(
                vehicle_guid,
                unk1,
                pos,
                orient,
                vel,
                unk2,
                unk3,
                unk4,
                is_crouched,
                unk6,
                unk7,
                unk8,
                unk9,
                unkA
              )
            )
          )
        case VehicleAction.GenericObjectAction(player_guid, guid, code) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.GenericObjectAction(guid, code)
            )
          )
        case VehicleAction.InventoryState(player_guid, obj, parent_guid, start, con_data) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.InventoryState(obj, parent_guid, start, con_data)
            )
          )
        case VehicleAction.InventoryState2(player_guid, obj_guid, parent_guid, value) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.InventoryState2(obj_guid, parent_guid, value)
            )
          )
        case VehicleAction.KickPassenger(player_guid, seat_num, kickedByDriver, vehicle_guid) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.KickPassenger(seat_num, kickedByDriver, vehicle_guid)
            )
          )
        case VehicleAction.ObjectDelete(guid) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              Service.defaultPlayerGUID,
              VehicleResponse.ObjectDelete(guid)
            )
          )
        case VehicleAction.LoadVehicle(player_guid, vehicle, vtype, vguid, vdata) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.LoadVehicle(vehicle, vtype, vguid, vdata)
            )
          )
        case VehicleAction.MountVehicle(player_guid, vehicle_guid, seat) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.MountVehicle(vehicle_guid, seat)
            )
          )
        case VehicleAction.Ownership(player_guid, vehicle_guid) =>
          VehicleEvents.publish(
            VehicleServiceResponse(s"/$forChannel/Vehicle", player_guid, VehicleResponse.Ownership(vehicle_guid))
          )
        case VehicleAction.PlanetsideAttribute(exclude_guid, target_guid, attribute_type, attribute_value) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              exclude_guid,
              VehicleResponse.PlanetsideAttribute(target_guid, attribute_type, attribute_value)
            )
          )
        case VehicleAction.SeatPermissions(player_guid, vehicle_guid, seat_group, permission) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.SeatPermissions(vehicle_guid, seat_group, permission)
            )
          )
        case VehicleAction.StowEquipment(player_guid, vehicle_guid, slot, item) =>
          val definition = item.Definition
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.StowEquipment(
                vehicle_guid,
                slot,
                definition.ObjectId,
                item.GUID,
                definition.Packet.DetailedConstructorData(item).get
              )
            )
          )
        case VehicleAction.UnloadVehicle(player_guid, vehicle, vehicle_guid) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.UnloadVehicle(vehicle, vehicle_guid)
            )
          )
        case VehicleAction.UnstowEquipment(player_guid, item_guid) =>
          VehicleEvents.publish(
            VehicleServiceResponse(s"/$forChannel/Vehicle", player_guid, VehicleResponse.UnstowEquipment(item_guid))
          )
        case VehicleAction.VehicleState(
              player_guid,
              vehicle_guid,
              unk1,
              pos,
              ang,
              vel,
              unk2,
              unk3,
              unk4,
              wheel_direction,
              unk5,
              unk6
            ) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.VehicleState(
                vehicle_guid,
                unk1,
                pos,
                ang,
                vel,
                unk2,
                unk3,
                unk4,
                wheel_direction,
                unk5,
                unk6
              )
            )
          )
        case VehicleAction.SendResponse(player_guid, msg) =>
          VehicleEvents.publish(
            VehicleServiceResponse(s"/$forChannel/Vehicle", player_guid, VehicleResponse.SendResponse(msg))
          )

        //unlike other messages, just return to sender, don't publish
        case VehicleAction.UpdateAmsSpawnPoint(zone: Zone) =>
          sender() ! VehicleServiceResponse(
            s"/$forChannel/Vehicle",
            Service.defaultPlayerGUID,
            VehicleResponse.UpdateAmsSpawnPoint(AmsSpawnPoints(zone))
          )

        case VehicleAction.TransferPassengerChannel(
              player_guid,
              old_channel,
              temp_channel,
              vehicle,
              vehicle_to_delete
            ) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              player_guid,
              VehicleResponse.TransferPassengerChannel(old_channel, temp_channel, vehicle, vehicle_to_delete)
            )
          )
        case VehicleAction.KickCargo(player_guid, cargo, speed, delay) =>
          VehicleEvents.publish(
            VehicleServiceResponse(s"/$forChannel/Vehicle", player_guid, VehicleResponse.KickCargo(cargo, speed, delay))
          )

        case VehicleAction.ChangeLoadout(target_guid, removed_weapons, new_weapons, old_inventory, new_inventory) =>
          VehicleEvents.publish(
            VehicleServiceResponse(
              s"/$forChannel/Vehicle",
              Service.defaultPlayerGUID,
              VehicleResponse.ChangeLoadout(target_guid, removed_weapons, new_weapons, old_inventory, new_inventory)
            )
          )
        case _ => ;
      }

    //message to TurretUpgrader
    case VehicleServiceMessage.TurretUpgrade(msg) =>
      turretUpgrade forward msg

    //from VehicleSpawnControl, etc.
    case VehicleSpawnPad.ConcealPlayer(player_guid) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/${zone.id}/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.ConcealPlayer(player_guid)
        )
      )

    case VehicleSpawnPad.AttachToRails(vehicle, pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/${zone.id}/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.AttachToRails(vehicle.GUID, pad.GUID)
        )
      )

    case VehicleSpawnPad.StartPlayerSeatedInVehicle(driver_name, vehicle, pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/$driver_name/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.StartPlayerSeatedInVehicle(vehicle, pad)
        )
      )

    case VehicleSpawnPad.PlayerSeatedInVehicle(driver_name, vehicle, pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/$driver_name/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.PlayerSeatedInVehicle(vehicle, pad)
        )
      )

    case VehicleSpawnPad.ServerVehicleOverrideStart(driver_name, vehicle, pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/$driver_name/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.ServerVehicleOverrideStart(vehicle, pad)
        )
      )

    case VehicleSpawnPad.ServerVehicleOverrideEnd(driver_name, vehicle, pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/$driver_name/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.ServerVehicleOverrideEnd(vehicle, pad)
        )
      )

    case VehicleSpawnPad.DetachFromRails(vehicle, pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/${zone.id}/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.DetachFromRails(vehicle.GUID, pad.GUID, pad.Position, pad.Orientation.z)
        )
      )
    case VehicleSpawnPad.ResetSpawnPad(pad) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/${zone.id}/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.ResetSpawnPad(pad.GUID)
        )
      )

    case VehicleSpawnPad.RevealPlayer(player_guid) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/${zone.id}/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.RevealPlayer(player_guid)
        )
      )

    case VehicleSpawnPad.PeriodicReminder(to, reason, data) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/$to/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.PeriodicReminder(reason, data)
        )
      )

    //correspondence from WorldSessionActor
    case VehicleServiceMessage.AMSDeploymentChange(_) =>
      VehicleEvents.publish(
        VehicleServiceResponse(
          s"/${zone.id}/Vehicle",
          Service.defaultPlayerGUID,
          VehicleResponse.UpdateAmsSpawnPoint(AmsSpawnPoints(zone))
        )
      )

    case msg =>
      log.warn(s"Unhandled message $msg from ${sender()}")
  }

  import net.psforever.objects.serverobject.tube.SpawnTube
  def AmsSpawnPoints(zone: Zone): List[SpawnTube] = {
    import net.psforever.objects.vehicles.UtilityType
    import net.psforever.objects.GlobalDefinitions
    zone.Vehicles
      .filter(veh =>
        veh.Health > 0 && veh.Definition == GlobalDefinitions.ams && veh.DeploymentState == DriveState.Deployed
      )
      .flatMap(veh => veh.Utilities.values.filter(util => util.UtilType == UtilityType.ams_respawn_tube))
      .map(util => util().asInstanceOf[SpawnTube])
  }
}
