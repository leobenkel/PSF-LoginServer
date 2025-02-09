// Copyright (c) 2017 PSForever
package objects

import akka.actor.Props
import akka.testkit.TestProbe
import akka.actor.typed.scaladsl.adapter._
import base.ActorTest
import net.psforever.actors.zone.ZoneActor
import net.psforever.objects.avatar.{Avatar, Certification}
import net.psforever.objects.{Default, GlobalDefinitions, Player, Tool}
import net.psforever.objects.definition.ToolDefinition
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.serverobject.CommonMessages
import net.psforever.objects.serverobject.mount.{MountInfo, Mountable}
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.serverobject.turret._
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.packet.game.{InventoryStateMessage, RepairMessage}
import net.psforever.types._
import org.specs2.mutable.Specification
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.services.vehicle.{VehicleAction, VehicleServiceMessage}

import scala.collection.mutable
import scala.concurrent.duration._

class FacilityTurretTest extends Specification {
  "FacilityTurretTest" should {
    "define" in {
      val obj = new FacilityTurretDefinition(480)
      obj.Weapons mustEqual mutable.HashMap.empty[TurretUpgrade.Value, ToolDefinition]
      obj.ReserveAmmunition mustEqual false
      obj.FactionLocked mustEqual true
      obj.MaxHealth mustEqual 0
      obj.MountPoints.isEmpty mustEqual true
    }

    "construct" in {
      val obj = FacilityTurret(GlobalDefinitions.manned_turret)
      obj.Weapons.size mustEqual 1
      obj.Weapons(1).Equipment match {
        case Some(tool: Tool) =>
          tool.Definition mustEqual GlobalDefinitions.phalanx_sgl_hevgatcan
        case _ =>
          ko
      }
      obj.Seats.size mustEqual 1
      obj.MountPoints.size mustEqual 1
      obj.MountPoints.get(1).contains(MountInfo(0, Vector3.Zero)) mustEqual true
      obj.Health mustEqual 3600
      obj.Upgrade mustEqual TurretUpgrade.None
      obj.Health = 360
      obj.Health mustEqual 360
    }

    "upgrade to a different weapon" in {
      val obj = FacilityTurret(GlobalDefinitions.manned_turret)
      obj.Upgrade = TurretUpgrade.None
      obj.Weapons(1).Equipment match {
        case Some(tool: Tool) =>
          tool.Definition mustEqual GlobalDefinitions.phalanx_sgl_hevgatcan
          tool.FireModeIndex mustEqual 0
          tool.NextFireMode
          tool.FireModeIndex mustEqual 0 //one fire mode
        case _ =>
          ko
      }
      //upgrade
      obj.Upgrade = TurretUpgrade.AVCombo
      obj.Weapons(1).Equipment match {
        case Some(tool: Tool) =>
          tool.Definition mustEqual GlobalDefinitions.phalanx_avcombo
          tool.FireModeIndex mustEqual 0
          tool.ProjectileType mustEqual GlobalDefinitions.phalanx_projectile.ProjectileType
          tool.NextFireMode
          tool.FireModeIndex mustEqual 1
          tool.ProjectileType mustEqual GlobalDefinitions.phalanx_av_projectile.ProjectileType
        case _ =>
          ko
      }
      //revert
      obj.Upgrade = TurretUpgrade.None
      obj.Weapons(1).Equipment match {
        case Some(tool: Tool) =>
          tool.Definition mustEqual GlobalDefinitions.phalanx_sgl_hevgatcan
        case _ =>
          ko
      }
    }
  }
}

class FacilityTurretControl1Test extends ActorTest {
  "FacilityTurretControl" should {
    "construct" in {
      val obj = FacilityTurret(GlobalDefinitions.manned_turret)
      obj.Actor = system.actorOf(Props(classOf[FacilityTurretControl], obj), "turret-control")
      assert(obj.Actor != Default.Actor)
    }
  }
}

class FacilityTurretControl2Test extends ActorTest {
private val player = Player(Avatar(0, "", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute))
private val obj    = FacilityTurret(GlobalDefinitions.manned_turret)
  obj.GUID = PlanetSideGUID(1)
  obj.Zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    this.actor = new TestProbe(system).ref.toTyped[ZoneActor.Command]
  }
  obj.Actor = system.actorOf(Props(classOf[FacilityTurretControl], obj), "turret-control")
private val bldg = Building("Building", guid = 0, map_id = 0, Zone.Nowhere, StructureType.Building)
  bldg.Amenities = obj
  bldg.Faction = PlanetSideEmpire.TR

  "FacilityTurretControl" should {
    "mount on faction affiliation when FactionLock is true" in {
      assert(player.Faction == PlanetSideEmpire.TR)
      assert(obj.Faction == PlanetSideEmpire.TR)
      assert(obj.Definition.FactionLocked)

      obj.Actor ! Mountable.TryMount(player, 1)
      val reply = receiveOne(300 milliseconds)
      reply match {
        case msg: Mountable.MountMessages =>
          assert(msg.response.isInstanceOf[Mountable.CanMount])
        case _ =>
          assert(false)
      }
    }
  }
}

class FacilityTurretControl3Test extends ActorTest {
private val player = Player(Avatar(0, "", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute))
private val obj    = FacilityTurret(GlobalDefinitions.manned_turret)
  obj.GUID = PlanetSideGUID(1)
  obj.Actor = system.actorOf(Props(classOf[FacilityTurretControl], obj), "turret-control")
private val bldg = Building("Building", guid = 0, map_id = 0, Zone.Nowhere, StructureType.Building)
  bldg.Amenities = obj

  "FacilityTurretControl" should {
    "block seating on mismatched faction affiliation when FactionLock is true" in {
      assert(player.Faction == PlanetSideEmpire.TR)
      assert(obj.Faction == PlanetSideEmpire.NEUTRAL)
      assert(obj.Definition.FactionLocked)

      obj.Actor ! Mountable.TryMount(player, 1)
      val reply = receiveOne(300 milliseconds)
      reply match {
        case msg: Mountable.MountMessages =>
          assert(msg.response.isInstanceOf[Mountable.CanNotMount])
        case _ =>
          assert(false)
      }
    }
  }
}

class FacilityTurretControl4Test extends ActorTest {
private val player = Player(Avatar(0, "", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute))
private val obj = FacilityTurret(GlobalDefinitions.vanu_sentry_turret)
  obj.GUID = PlanetSideGUID(1)
  obj.Zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    this.actor = new TestProbe(system).ref.toTyped[ZoneActor.Command]
  }
  obj.Actor = system.actorOf(Props(classOf[FacilityTurretControl], obj), "turret-control")
private val bldg = Building("Building", guid = 0, map_id = 0, Zone.Nowhere, StructureType.Building)
  bldg.Amenities = obj

  "FacilityTurretControl" should {
    "seating even with mismatched faction affiliation when FactionLock is false" in {
      assert(player.Faction == PlanetSideEmpire.TR)
      assert(obj.Faction == PlanetSideEmpire.NEUTRAL)
      assert(!obj.Definition.FactionLocked)

      obj.Actor ! Mountable.TryMount(player, 1)
      val reply = receiveOne(300 milliseconds)
      reply match {
        case msg: Mountable.MountMessages =>
          assert(msg.response.isInstanceOf[Mountable.CanMount])
        case _ =>
          assert(false)
      }
    }
  }
}

class FacilityTurretControlRestorationTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(10))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val building      = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
private val activityProbe = TestProbe()
private val avatarProbe   = TestProbe()
private val vehicleProbe  = TestProbe()
private val buildingProbe = TestProbe()
  zone.Activity = activityProbe.ref
  zone.AvatarEvents = avatarProbe.ref
  zone.VehicleEvents = vehicleProbe.ref
  building.Actor = buildingProbe.ref

private val turret = new FacilityTurret(GlobalDefinitions.manned_turret) //2, 5, 6
  turret.Actor = system.actorOf(Props(classOf[FacilityTurretControl], turret), "turret-control")
  turret.Zone = zone
  turret.Position = Vector3(1, 0, 0)
private val turretWeapon = turret.Weapons.values.head.Equipment.get.asInstanceOf[Tool]

private val avatar = Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)
    .copy(certifications = Set(Certification.Engineering))
private val player1 = Player(avatar) //guid=3
  player1.Spawn()
  player1.Position = Vector3(2, 2, 2)
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref

  guid.register(building, 1)
  guid.register(turret, 2)
  guid.register(player1, 3)
  guid.register(turretWeapon, 5)
  guid.register(turretWeapon.AmmoSlot.Box, 6)
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = turret

private val tool = Tool(GlobalDefinitions.nano_dispenser) //7 & 8
  guid.register(tool, 7)
  guid.register(tool.AmmoSlot.Box, 8)

  "RepairableTurretWeapon" should {
    "handle repairs and restoration" in {
      turret.Health = turret.Definition.RepairRestoresAt - 1 //initial state manip
      turret.Destroyed = true                                //initial state manip
      assert(turret.Health < turret.Definition.RepairRestoresAt)
      assert(turret.Destroyed)

      turret.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg12345 = avatarProbe.receiveN(5, 500 milliseconds)
      val msg4     = vehicleProbe.receiveOne(500 milliseconds)
      assert(
        msg12345.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction
                  .SendResponse(PlanetSideGUID(0), InventoryStateMessage(PlanetSideGUID(8), _, PlanetSideGUID(7), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg12345(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg12345(2) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 50, 0)) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg12345(3) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 51, 0)) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg12345(4) match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(PlanetSideGUID(0), RepairMessage(PlanetSideGUID(2), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg4 match {
          case VehicleServiceMessage("test", VehicleAction.EquipmentInSlot(_, PlanetSideGUID(2), 1, t))
              if t eq turretWeapon =>
            true
          case _ => false
        }
      )
      assert(turret.Health > turret.Definition.RepairRestoresAt)
      assert(!turret.Destroyed)
    }
  }
}
