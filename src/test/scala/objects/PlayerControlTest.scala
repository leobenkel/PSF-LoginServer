// Copyright (c) 2020 PSForever
package objects

import akka.actor.typed.ActorRef
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.actors.session.AvatarActor
import net.psforever.objects.avatar.{Avatar, Certification, PlayerControl}
import net.psforever.objects.ballistics._
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.vital.Vitality
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.objects._
import net.psforever.objects.serverobject.CommonMessages
import net.psforever.objects.serverobject.environment.{DeepSquare, EnvironmentAttribute, OxygenStateTarget, Pool}
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.packet.game._
import net.psforever.types._
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}

import scala.concurrent.duration._

class PlayerControlHealTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val player2 =
    Player(Avatar(1, "TestCharacter2", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=2
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1, player2)
    override def AvatarEvents = avatarProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), "player1-control")
  player2.Zone = zone
  player2.Spawn()
  guid.register(player2.avatar.locker, 6)
  player2.Actor = system.actorOf(Props(classOf[PlayerControl], player2, null), "player2-control")

private val tool = Tool(GlobalDefinitions.medicalapplicator) //guid=3 & 4
  guid.register(player1, 1)
  guid.register(player2, 2)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)

  "PlayerControl" should {
    "handle being healed by another player" in {
      val originalHealth   = player2.Health = 0 //initial state manip
      val originalMagazine = tool.Magazine
      assert(originalHealth < player2.MaxHealth)

      player2.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg_avatar = avatarProbe.receiveN(4, 500 milliseconds)
      assert(
        msg_avatar.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, InventoryStateMessage(PlanetSideGUID(4), _, PlanetSideGUID(3), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_avatar(2) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 55, 1)
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(3) match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, RepairMessage(PlanetSideGUID(2), _))
              ) =>
            true
          case _ => false
        }
      )
      val raisedHealth = player2.Health
      assert(raisedHealth > originalHealth)
      assert(tool.Magazine < originalMagazine)

      player1.Position = Vector3(10, 0, 0) //moved more than 5m away
      player2.Actor ! CommonMessages.Use(player1, Some(tool))
      avatarProbe.expectNoMessage(500 milliseconds)
      assert(raisedHealth == player2.Health)
    }
  }
}
class PlayerControlHealSelfTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1)
    override def AvatarEvents = avatarProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), "player1-control")

private val tool = Tool(GlobalDefinitions.medicalapplicator) //guid=3 & 4
  guid.register(player1, 1)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)

  "PlayerControl" should {
    "handle healing own self" in {
      val originalHealth   = player1.Health = 1 //initial state manip
      val originalMagazine = tool.Magazine
      assert(originalHealth < player1.MaxHealth)

      player1.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg_avatar1 = avatarProbe.receiveN(2, 500 milliseconds)
      assert(
        msg_avatar1.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, InventoryStateMessage(PlanetSideGUID(4), _, PlanetSideGUID(3), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar1(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 0, _)) => true
          case _                                                                                            => false
        }
      )
      val raisedHealth = player1.Health
      assert(raisedHealth > originalHealth)
      assert(tool.Magazine < originalMagazine)

      player1.Position = Vector3(10, 0, 0) //trying to move away from oneself doesn't work
      player1.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg_avatar2 = avatarProbe.receiveN(2, 500 milliseconds)
      assert(
        msg_avatar2.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, InventoryStateMessage(PlanetSideGUID(4), _, PlanetSideGUID(3), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar2(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(player1.Health > raisedHealth)
    }
  }
}

class PlayerControlRepairTest extends ActorTest {
private val avatar = Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)
    .copy(certifications = Set(Certification.Engineering))
private val player1 = Player(avatar) //guid=1
private val player2 =
    Player(Avatar(1, "TestCharacter2", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=2
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1, player2)
    override def AvatarEvents = avatarProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), "player1-control")
  player2.Zone = zone
  player2.Spawn()
  guid.register(player2.avatar.locker, 6)
  player2.Actor = system.actorOf(Props(classOf[PlayerControl], player2, null), "player2-control")

private val tool = Tool(GlobalDefinitions.bank) //guid=3 & 4
  guid.register(player1, 1)
  guid.register(player2, 2)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)

  "PlayerControl" should {
    "handle being repaired by another player" in {
      val originalArmor    = player2.Armor = 0 //initial state manip
      val originalMagazine = tool.Magazine
      assert(originalArmor < player2.MaxArmor)

      player2.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg_avatar = avatarProbe.receiveN(5, 1000 milliseconds)
      assert(
        msg_avatar.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, InventoryStateMessage(PlanetSideGUID(4), _, PlanetSideGUID(3), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 4, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_avatar(2) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 56, 1)
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(3) match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, RepairMessage(PlanetSideGUID(2), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(4) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 56, 1)
              ) =>
            true
          case _ => false
        }
      )
      assert(player2.Armor > originalArmor)
      assert(tool.Magazine < originalMagazine)

      val fixedArmor = player2.Armor
      player1.Position = Vector3(10, 0, 0) //moved more than 5m away
      player2.Actor ! CommonMessages.Use(player1, Some(tool))
      avatarProbe.expectNoMessage(500 milliseconds)
      assert(fixedArmor == player2.Armor)
    }
  }
}

class PlayerControlRepairSelfTest extends ActorTest {
private val avatar = Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)
    .copy(certifications = Set(Certification.Engineering))
private val player1 = Player(avatar) //guid=1
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1)
    override def AvatarEvents = avatarProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), "player1-control")

private val tool = Tool(GlobalDefinitions.bank) //guid=3 & 4
  guid.register(player1, 1)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)

  "PlayerControl" should {
    "handle repairing own self" in {
      val originalArmor    = player1.Armor = 0 //initial state manip
      val originalMagazine = tool.Magazine
      assert(originalArmor < player1.MaxArmor)

      player1.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg_avatar1 = avatarProbe.receiveN(2, 500 milliseconds)
      assert(
        msg_avatar1.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, InventoryStateMessage(PlanetSideGUID(4), _, PlanetSideGUID(3), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar1(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 4, _)) => true
          case _                                                                                            => false
        }
      )
      val fixedArmor = player1.Armor
      assert(fixedArmor > originalArmor)
      assert(tool.Magazine < originalMagazine)

      player1.Position = Vector3(10, 0, 0) //trying to move away from oneself doesn't work
      player1.Actor ! CommonMessages.Use(player1, Some(tool))
      val msg_avatar2 = avatarProbe.receiveN(2, 500 milliseconds)
      assert(
        msg_avatar2.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction.SendResponse(_, InventoryStateMessage(PlanetSideGUID(4), _, PlanetSideGUID(3), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar2(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 4, _)) => true
          case _                                                                                            => false
        }
      )
      assert(player1.Armor > fixedArmor)
    }
  }
}

class PlayerControlDamageTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val player2 =
    Player(Avatar(1, "TestCharacter2", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=2
private val avatarProbe = TestProbe()
private val activityProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1, player2)
    override def AvatarEvents = avatarProbe.ref
    override def Activity = activityProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), name = "player1-control")
  player2.Zone = zone
  player2.Spawn()
  guid.register(player2.avatar.locker, 6)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player2.Actor = system.actorOf(Props(classOf[PlayerControl], player2, avatarActor), name = "player2-control")

private val tool         = Tool(GlobalDefinitions.suppressor) //guid 3 & 4
private val projectile   = tool.Projectile
private val player1Source = PlayerSource(player1)
private val resolved = DamageInteraction(
    SourceEntry(player2),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        tool.Definition,
        tool.FireMode,
        player1Source,
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      player1.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  guid.register(player1, 1)
  guid.register(player2, 2)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)
  expectNoMessage(200 milliseconds)

  "PlayerControl" should {
    "handle damage" in {
      assert(player2.Health == player2.Definition.DefaultHealth)
      assert(player2.Armor == player2.MaxArmor)
      player2.Actor ! Vitality.Damage(applyDamageTo)
      val msg_avatar   = avatarProbe.receiveN(3, 500 milliseconds)
      val msg_stamina  = probe.receiveOne(500 milliseconds)
      val msg_activity = activityProbe.receiveOne(200 milliseconds)
      assert(
        msg_avatar.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 4, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_stamina match {
          case AvatarActor.ConsumeStamina(_) => true
          case _                             => false
        }
      )
      assert(
        msg_avatar(1) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _ => false
        }
      )
      assert(
        msg_activity match {
          case activity: Zone.HotSpot.Activity =>
            activity.attacker == player1Source &&
              activity.defender == PlayerSource(player2) &&
              activity.location == Vector3(1, 0, 0)
          case _ => false
        }
      )
      assert(
        msg_avatar(2) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.HitHint(PlanetSideGUID(1), PlanetSideGUID(2))
              ) =>
            true
          case _ => false
        }
      )
      assert(player2.Health < player2.Definition.DefaultHealth)
      assert(player2.Armor < player2.MaxArmor)
    }
  }
}

class PlayerControlDeathStandingTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val player2 =
    Player(Avatar(1, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=2
private val avatarProbe = TestProbe()
private val activityProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1, player2)
    override def AvatarEvents = avatarProbe.ref
    override def Activity = activityProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), name = "player1-control")
  player2.Zone = zone
  player2.Spawn()
  guid.register(player2.avatar.locker, 6)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player2.Actor = system.actorOf(Props(classOf[PlayerControl], player2, avatarActor), name = "player2-control")

private val tool         = Tool(GlobalDefinitions.suppressor) //guid 3 & 4
private val projectile   = tool.Projectile
private val player1Source = PlayerSource(player1)
private val resolved = DamageInteraction(
    SourceEntry(player2),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        tool.Definition,
        tool.FireMode,
        player1Source,
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      player1.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  guid.register(player1, 1)
  guid.register(player2, 2)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)
  expectNoMessage(200 milliseconds)

  "PlayerControl" should {
    "handle death" in {
      player2.Health = player2.Definition.DamageDestroysAt + 1 //initial state manip
      player2.ExoSuit = ExoSuitType.MAX
      player2.Armor = 1     //initial state manip
      player2.Capacitor = 1 //initial state manip
      assert(player2.Health > player2.Definition.DamageDestroysAt)
      assert(player2.Armor == 1)
      assert(player2.Capacitor == 1)
      assert(player2.isAlive)

      player2.Actor ! Vitality.Damage(applyDamageTo)
      val msg_avatar = avatarProbe.receiveN(8, 500 milliseconds)
      val msg_stamina = probe.receiveOne(500 milliseconds)
      activityProbe.expectNoMessage(200 milliseconds)
      assert(
        msg_avatar.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 4, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_stamina match {
          case AvatarActor.DeinitializeImplants() => true
          case _                                  => false
        }
      )
      assert(
        msg_avatar(1) match {
          case AvatarServiceMessage("TestCharacter2", AvatarAction.DropSpecialItem()) => true
          case _                                                                      => false
        }
      )
      assert(
        msg_avatar(2) match {
          case AvatarServiceMessage("TestCharacter2", AvatarAction.Killed(PlanetSideGUID(2), None)) => true
          case _                                                                                    => false
        }
      )
      assert(
        msg_avatar(3) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_avatar(4) match {
          case AvatarServiceMessage("TestCharacter2", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 7, _)) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(5) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.SendResponse(_, DestroyMessage(PlanetSideGUID(2), PlanetSideGUID(1), _, _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(6) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.SendResponse(
                  _,
                  AvatarDeadStateMessage(DeadState.Dead, 300000, 300000, Vector3.Zero, PlanetSideEmpire.NC, true)
                )
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(7) match {
          case AvatarServiceMessage("test", AvatarAction.DestroyDisplay(killer, victim, _, _))
              if killer.Name.equals(player1.Name) && victim.Name.equals(player2.Name) =>
            true
          case _ => false
        }
      )
      assert(player2.Health <= player2.Definition.DamageDestroysAt)
      assert(player2.Armor == 0)
      assert(!player2.isAlive)
    }
  }
}

class PlayerControlDeathSeatedTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val player2 =
    Player(Avatar(1, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=2
private val avatarProbe = TestProbe()
private val activityProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1, player2)
    override def AvatarEvents = avatarProbe.ref
    override def Activity = activityProbe.ref
  }

  player1.Zone = zone
  player1.Spawn()
  player1.Position = Vector3(2, 0, 0)
  guid.register(player1.avatar.locker, 5)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, null), name = "player1-control")
  player2.Zone = zone
  player2.Spawn()
  guid.register(player2.avatar.locker, 6)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player2.Actor = system.actorOf(Props(classOf[PlayerControl], player2, avatarActor), name = "player2-control")

private val tool         = Tool(GlobalDefinitions.suppressor) //guid 3 & 4
private val vehicle = Vehicle(GlobalDefinitions.quadstealth) //guid=5
  vehicle.Faction = player2.Faction

  guid.register(player1, 1)
  guid.register(player2, 2)
  guid.register(tool, 3)
  guid.register(tool.AmmoSlot.Box, 4)
  guid.register(vehicle, 7)
private val projectile   = tool.Projectile
private val player1Source = PlayerSource(player1)
private val resolved = DamageInteraction(
    SourceEntry(player2),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        tool.Definition,
        tool.FireMode,
        player1Source,
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      player1.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)

  "PlayerControl" should {
    "handle death when seated (in something)" in {
      player2.Health = player2.Definition.DamageDestroysAt + 1 //initial state manip
      player2.VehicleSeated = vehicle.GUID                     //initial state manip, anything
      vehicle.Seats(0).mount(player2)
      player2.Armor = 0 //initial state manip
      assert(player2.Health > player2.Definition.DamageDestroysAt)
      assert(player2.isAlive)

      player2.Actor ! Vitality.Damage(applyDamageTo)
      val msg_avatar = avatarProbe.receiveN(9, 500 milliseconds)
      val msg_stamina = probe.receiveOne(500 milliseconds)
      activityProbe.expectNoMessage(200 milliseconds)
      assert(
        msg_stamina match {
          case AvatarActor.DeinitializeImplants() => true
          case _                                  => false
        }
      )
      assert(
        msg_avatar.head match {
          case AvatarServiceMessage("TestCharacter2", AvatarAction.DropSpecialItem()) => true
          case _                                                                      => false
        }
      )
      assert(
        msg_avatar(1) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.Killed(PlanetSideGUID(2), Some(PlanetSideGUID(7)))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(2) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.SendResponse(_, ObjectDetachMessage(PlanetSideGUID(7), PlanetSideGUID(2), _, _, _, _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(3) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 29, 1)
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(4) match {
          case AvatarServiceMessage("test", AvatarAction.ObjectDelete(PlanetSideGUID(2), PlanetSideGUID(2), _)) => true
          case _                                                                                                => false
        }
      )
      assert(
        msg_avatar(5) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_avatar(6) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.SendResponse(_, DestroyMessage(PlanetSideGUID(2), PlanetSideGUID(1), _, _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(7) match {
          case AvatarServiceMessage(
                "TestCharacter2",
                AvatarAction.SendResponse(
                  _,
                  AvatarDeadStateMessage(DeadState.Dead, 300000, 300000, Vector3.Zero, PlanetSideEmpire.NC, true)
                )
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_avatar(8) match {
          case AvatarServiceMessage("test", AvatarAction.DestroyDisplay(killer, victim, _, _))
              if killer.Name.equals(player1.Name) && victim.Name.equals(player2.Name) =>
            true
          case _ => false
        }
      )
      assert(player2.Health <= player2.Definition.DamageDestroysAt)
      assert(!player2.isAlive)
    }
  }
}

class PlayerControlInteractWithWaterTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val pool = Pool(EnvironmentAttribute.Water, DeepSquare(-1, 10, 10, 0, 0))
private val zone = new Zone(
    id = "test",
    new ZoneMap(name = "test-map") {
      environment = List(pool)
    },
    zoneNumber = 0
  ) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1)
    override def AvatarEvents = avatarProbe.ref
  }
  zone.blockMap.addTo(player1)
  zone.blockMap.addTo(pool)

  player1.Zone = zone
  player1.Spawn()
  guid.register(player1.avatar.locker, 5)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, avatarActor), "player1-control")

  guid.register(player1, 1)

  "PlayerControl" should {
    "cause drowning when player steps too deep in water" in {
      assert(player1.Health == 100)
      player1.Position = Vector3(5,5,-3) //right in the pool
      player1.zoneInteractions() //trigger

      val msg_drown = avatarProbe.receiveOne(250 milliseconds)
      assert(
        msg_drown match {
          case AvatarServiceMessage(
            "TestCharacter1",
            AvatarAction.OxygenState(OxygenStateTarget(PlanetSideGUID(1), OxygenState.Suffocation, 100f), _)
          )      => true
          case _ => false
        }
      )
      //player will die in 60s
      //detailing these death messages is not necessary
      assert(player1.Health == 100)
      probe.receiveOne(65 seconds) //wait until our implants deinitialize
      assert(player1.Health == 0) //ded
    }
  }
}

class PlayerControlStopInteractWithWaterTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val pool = Pool(EnvironmentAttribute.Water, DeepSquare(-1, 10, 10, 0, 0))
private val zone = new Zone(
    id = "test",
    new ZoneMap(name = "test-map") {
      environment = List(pool)
    },
    zoneNumber = 0
  ) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1)
    override def AvatarEvents = avatarProbe.ref
  }
  zone.blockMap.addTo(player1)
  zone.blockMap.addTo(pool)

  player1.Zone = zone
  player1.Spawn()
  guid.register(player1.avatar.locker, 5)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, avatarActor), "player1-control")

  guid.register(player1, 1)

  "PlayerControl" should {
    "stop drowning if player steps out of deep water" in {
      assert(player1.Health == 100)
      player1.Position = Vector3(5,5,-3) //right in the pool
      player1.zoneInteractions() //trigger

      val msg_drown = avatarProbe.receiveOne(250 milliseconds)
      assert(
        msg_drown match {
          case AvatarServiceMessage(
            "TestCharacter1",
            AvatarAction.OxygenState(OxygenStateTarget(PlanetSideGUID(1), OxygenState.Suffocation, 100f), _)
          )      => true
          case _ => false
        }
      )
      //player would normally die in 60s
      player1.Position = Vector3.Zero //pool's closed
      player1.zoneInteractions() //trigger
      val msg_recover = avatarProbe.receiveOne(250 milliseconds)
      assert(
        msg_recover match {
          case AvatarServiceMessage(
            "TestCharacter1",
            AvatarAction.OxygenState(OxygenStateTarget(PlanetSideGUID(1), OxygenState.Recovery, _), _)
          )      => true
          case _ => false
        }
      )
      assert(player1.Health == 100) //still alive?
      probe.expectNoMessage(65 seconds)
      assert(player1.Health == 100) //yep, still alive
    }
  }
}

class PlayerControlInteractWithLavaTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val pool = Pool(EnvironmentAttribute.Lava, DeepSquare(-1, 10, 10, 0, 0))
private val zone = new Zone(
    id = "test-map",
    new ZoneMap(name = "test-map") {
      environment = List(pool)
    },
    zoneNumber = 0
  ) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1)
    override def AvatarEvents = avatarProbe.ref
    override def Activity = TestProbe().ref
  }
  zone.blockMap.addTo(player1)
  zone.blockMap.addTo(pool)

  player1.Zone = zone
  player1.Spawn()
  guid.register(player1.avatar.locker, 5)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, avatarActor), "player1-control")

  guid.register(player1, 1)

  "PlayerControl" should {
    "take continuous damage if player steps into lava" in {
      assert(player1.Health == 100) //alive
      player1.Position = Vector3(5,5,-3) //right in the pool
      player1.zoneInteractions() //trigger

      val msg_burn = avatarProbe.receiveN(3, 1 seconds)
      assert(
        msg_burn.head match {
          case AvatarServiceMessage("test-map", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 0, _)) => true
          case _                                                                                                => false
        }
      )
      assert(
        msg_burn(1) match {
          case AvatarServiceMessage("TestCharacter1", AvatarAction.EnvironmentalDamage(PlanetSideGUID(1), _, _)) => true
          case _                                                                                                 => false
        }
      )
      assert(
        msg_burn(2) match {
          case AvatarServiceMessage("test-map", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(1), 54, _)) => true
          case _                                                                                                 => false
        }
      )
      assert(player1.Health > 0) //still alive?
      probe.receiveOne(65 seconds) //wait until player1's implants deinitialize
      assert(player1.Health == 0) //ded
    }
  }
}

class PlayerControlInteractWithDeathTest extends ActorTest {
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=1
private val avatarProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(15))
private val pool = Pool(EnvironmentAttribute.Death, DeepSquare(-1, 10, 10, 0, 0))
private val zone = new Zone(
    id = "test-map",
    new ZoneMap(name = "test-map") {
      environment = List(pool)
    },
    zoneNumber = 0
  ) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1)
    override def AvatarEvents = avatarProbe.ref
    override def Activity = TestProbe().ref
  }
  zone.blockMap.addTo(player1)
  zone.blockMap.addTo(pool)

  player1.Zone = zone
  player1.Spawn()
  guid.register(player1.avatar.locker, 5)
private val (probe, avatarActor) = PlayerControlTest.DummyAvatar(system)
  player1.Actor = system.actorOf(Props(classOf[PlayerControl], player1, avatarActor), "player1-control")

  guid.register(player1, 1)

  "PlayerControl" should {
    "take continuous damage if player steps into a pool of death" in {
      assert(player1.Health == 100) //alive
      player1.Position = Vector3(5,5,-3) //right in the pool
      player1.zoneInteractions() //trigger

      probe.receiveOne(250 milliseconds) //wait until oplayer1's implants deinitialize
      assert(player1.Health == 0) //ded
    }
  }
}

object PlayerControlTest {
  /**
    * A `TestProbe` whose `ActorRef` is packaged as a return type with it
    * and is passable as a typed `AvatarActor.Command` `Behavior` object.
    * Used for spawning `PlayControl` `Actor` objects with a refence to the `AvatarActor`,
    * when messaging callback renders it necessary during tests
    * but when accurate responses are unnecessary to emulate.
    * @param system what we use to spawn the `Actor`
    * @return the resulting probe, and it's modified `ActorRef`
    */
  def DummyAvatar(system: ActorSystem): (TestProbe, ActorRef[AvatarActor.Command]) = {
    import akka.actor.typed.scaladsl.adapter.ClassicActorRefOps
    val probe = new TestProbe(system)
    val actor = ClassicActorRefOps(probe.ref).toTyped[AvatarActor.Command]
    (probe, actor)
  }
}
