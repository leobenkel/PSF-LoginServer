// Copyright (c) 2020 PSForever
package objects

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.actors.zone.BuildingActor
import net.psforever.objects.avatar.{Avatar, Certification}
import net.psforever.objects.ballistics._
import net.psforever.objects.{GlobalDefinitions, Player, Tool}
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.serverobject.CommonMessages
import net.psforever.objects.serverobject.generator.{Generator, GeneratorControl, GeneratorDefinition}
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.vital.Vitality
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.vital.prop.DamageWithPosition
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.packet.game.{InventoryStateMessage, RepairMessage, TriggerEffectMessage}
import net.psforever.types._
import org.specs2.mutable.Specification
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}

import scala.concurrent.duration._

class GeneratorTest extends Specification {
  "Generator" should {
    "construct" in {
      Generator(GeneratorTest.generator_definition)
      ok
    }

    "start in 'Normal' condition" in {
      val obj = Generator(GeneratorTest.generator_definition)
      obj.Condition mustEqual PlanetSideGeneratorState.Normal
    }
  }
}

class GeneratorControlConstructTest extends ActorTest {
  "GeneratorControl" should {
    "construct" in {
      val gen = Generator(GeneratorTest.generator_definition)
      gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "gen-control")
      assert(gen.Actor != ActorRef.noSender)
    }
  }
}

class GeneratorControlDamageTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref

private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()

private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  building.PlayersInSOI = List(player1)
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "handle damage" in {
      assert(gen.Health == gen.Definition.MaxHealth)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal)

      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg_avatar = avatarProbe.receiveOne(500 milliseconds)
      val msg_building = buildingProbe.receiveOne(500 milliseconds)
      assert(
        msg_avatar match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_building match {
          case BuildingActor.AmenityStateChange(_, Some(GeneratorControl.Event.UnderAttack)) => true
          case _                                                                             => false
        }
      )
      assert(gen.Health < gen.Definition.MaxHealth)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal)
    }
  }
}

class GeneratorControlCriticalTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref

private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()

private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  building.PlayersInSOI = List(player1)
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
private val halfHealth    = gen.Definition.MaxHealth / 2
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "handle damage through the generator's critical state" in {
      gen.Health = halfHealth + 1 //no matter what, the next shot pushes it to critical status
      assert(gen.Health > halfHealth)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal)

      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg_avatar   = avatarProbe.receiveOne(500 milliseconds)
      val msg_building = buildingProbe.receiveOne(500 milliseconds)
      assert(
        msg_avatar match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_building match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Critical)) => o eq gen
          case _                                                                          => false
        }
      )
      assert(gen.Health < halfHealth)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Critical)
    }
  }
}

class GeneratorControlDestroyedTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref

private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()
  player1.Actor = TestProbe().ref

private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  building.PlayersInSOI = List(player1)
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "handle damage until destroyed" in {
      gen.Health = 1 //no matter what, the next shot destroys the generator
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal) //skipped critical state because didn't transition ~50%

      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg_building12 = buildingProbe.receiveN(2,500 milliseconds)
      assert(
        msg_building12.head match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Offline)) => o eq gen
          case _ => false
        }
      )
      assert(
        msg_building12(1) match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Destabilized)) => o eq gen
          case _ => false
        }
      )
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Destroyed)

      avatarProbe.expectNoMessage(9500 milliseconds)
      val msg_avatar2  = avatarProbe.receiveN(3, 1000 milliseconds) //see DamageableEntity test file
      val msg_building = buildingProbe.receiveOne(200 milliseconds)
      assert(
        msg_building match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Destroyed)) => o eq gen
          case _                                                                           => false
        }
      )
      assert(
        msg_avatar2.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_avatar2(1) match {
          case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(2), _, _, Vector3(1, 0, 0))) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg_avatar2(2) match {
          case AvatarServiceMessage(
                "test",
                AvatarAction.SendResponse(_, TriggerEffectMessage(PlanetSideGUID(2), "explosion_generator", None, None))
              ) =>
            true
          case _ => false
        }
      )
      assert(gen.Health == 0)
      assert(gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Destroyed)
    }
  }
}

class GeneratorControlKillsTest extends ActorTest {
private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref
private val player2 =
    Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.TR, CharacterSex.Female, 1, CharacterVoice.Mute)) //guid=4
  player2.Position = Vector3(25, 0, 0)                                                                       //>14m from generator; lives
  player2.Spawn()
private val player2Probe = TestProbe()
  player2.Actor = player2Probe.ref

private val avatarProbe = TestProbe()
private val activityProbe = TestProbe()
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
    override def LivePlayers = List(player1, player2)
    override def AvatarEvents = avatarProbe.ref
    override def Activity = activityProbe.ref
  }
private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Amenities = gen
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)
  guid.register(player2, 4)
  zone.blockMap.addTo(player1)
  zone.blockMap.addTo(player2)

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(25, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(2, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "damages (kills) players when the generator is destroyed" in {
      gen.Health = 1 //no matter what, the next shot destroys the generator
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal) //skipped critical state because didn't transition ~50%

      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg_building12 = buildingProbe.receiveN(2,500 milliseconds)
      assert(
        msg_building12.head match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Offline)) => o eq gen
          case _ => false
        }
      )
      assert(
        msg_building12(1) match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Destabilized)) => o eq gen
          case _ => false
        }
      )
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Destroyed)

      avatarProbe.expectNoMessage(9500 milliseconds)
      val msg_avatar2  = avatarProbe.receiveN(3, 1000 milliseconds) //see DamageableEntity test file
      val msg_building = buildingProbe.receiveOne(200 milliseconds)
      assert(
        msg_building match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Destroyed)) => o eq gen
          case _                                                                           => false
        }
      )
      assert(
        msg_avatar2.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg_avatar2(1) match {
          case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(2), _, _, Vector3(1, 0, 0))) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg_avatar2(2) match {
          case AvatarServiceMessage(
          "test",
          AvatarAction.SendResponse(_, TriggerEffectMessage(PlanetSideGUID(2), "explosion_generator", None, None))
          ) =>
            true
          case _ => false
        }
      )
      assert(gen.Health == 0)
      assert(gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Destroyed)

      val msg_player1  = player1Probe.receiveOne(200 milliseconds)
      player2Probe.expectNoMessage(200 milliseconds)
      assert(
        msg_player1 match {
          case _ @ Vitality.Damage(_) => true
          case _               => false
        }
      )
    }
  }
}

class GeneratorControlNotDestroyTwice extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(10))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}

    GUID(guid)
  }
private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
private val gen      = Generator(GeneratorTest.generator_definition)                        //guid=2
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Spawn()
  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")
private val activityProbe = TestProbe()
private val avatarProbe   = TestProbe()
private val buildingProbe = TestProbe()
  zone.Activity = activityProbe.ref
  zone.AvatarEvents = avatarProbe.ref
  building.Actor = buildingProbe.ref

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "not send a status update if destroyed and partially repaired, but destroyed again" in {
      //damaged, not yet restored, but will not be destroyed again within one shot
      val originalHealth = gen.Health = gen.Definition.DamageDestroysAt + 1
      gen.Condition = PlanetSideGeneratorState.Destroyed //initial state manip
      gen.Destroyed = true
      assert(gen.Destroyed)
      assert(originalHealth < gen.Definition.DefaultHealth)
      assert(originalHealth < gen.Definition.RepairRestoresAt)
      assert(originalHealth > gen.Definition.DamageDestroysAt)

      gen.Actor ! Vitality.Damage(applyDamageTo)
      avatarProbe.expectNoMessage(500 milliseconds)
      activityProbe.receiveOne(500 milliseconds)
      buildingProbe.expectNoMessage(1000 milliseconds)
      assert(gen.Health < originalHealth)
      assert(gen.Destroyed)
      assert(originalHealth < gen.Definition.DefaultHealth)
      assert(originalHealth < gen.Definition.RepairRestoresAt)
      assert(gen.Health <= gen.Definition.DamageDestroysAt)

      //damaged, not yet restored, and would have been destroyed with next shot
      gen.Health = 1
      assert(gen.Health == 1)
      assert(gen.Destroyed)
      gen.Actor ! Vitality.Damage(applyDamageTo)
      avatarProbe.expectNoMessage(500 milliseconds)
      activityProbe.receiveOne(500 milliseconds) //activity alert occurs because this was not a kill shot
      buildingProbe.expectNoMessage(1000 milliseconds)
      assert(gen.Health == 0)
      assert(gen.Destroyed)
    }
  }
}

class GeneratorControlNotDamageIfExplodingTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref

private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref

private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  building.PlayersInSOI = List(player1)
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "not damage if the generator is going to explode" in {
      gen.Health = 1 //no matter what, the next shot destroys the generator
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal) //skipped critical state because didn't transition ~50%

      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg_building12 = buildingProbe.receiveN(2,500 milliseconds)
      assert(
        msg_building12.head match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Offline)) => o eq gen
          case _ => false
        }
      )
      assert(
        msg_building12(1) match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Destabilized)) => o eq gen
          case _ => false
        }
      )
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Destroyed)
      //going to explode state

      //once
      gen.Actor ! Vitality.Damage(applyDamageTo)
      avatarProbe.expectNoMessage(500 milliseconds)
      buildingProbe.expectNoMessage(200 milliseconds)
      player1Probe.expectNoMessage(200 milliseconds)
      assert(gen.Health == 1)
      //twice
      gen.Actor ! Vitality.Damage(applyDamageTo)
      avatarProbe.expectNoMessage(500 milliseconds)
      buildingProbe.expectNoMessage(200 milliseconds)
      player1Probe.expectNoMessage(200 milliseconds)
      assert(gen.Health == 1)
    }
  }
}

class GeneratorControlNotRepairIfExplodingTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref

private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref

private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  building.PlayersInSOI = List(player1)
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)

private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(gen),
    ProjectileReason(
      DamageResolution.Hit,
      Projectile(
        projectile,
        weapon.Definition,
        weapon.FireMode,
        PlayerSource(player1),
        0,
        Vector3(2, 0, 0),
        Vector3(-1, 0, 0)
      ),
      gen.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()

private val tool = Tool(GlobalDefinitions.nano_dispenser) //4 & 5
  guid.register(tool, 4)
  guid.register(tool.AmmoSlot.Box, 5)
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "not repair if the generator is going to explode" in {
      gen.Health = 1 //no matter what, the next shot destroys the generator
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Normal) //skipped critical state because didn't transition ~50%

      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg_building12 = buildingProbe.receiveN(2,500 milliseconds)
      assert(
        msg_building12.head match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Offline)) => o eq gen
          case _ => false
        }
      )
      assert(
        msg_building12(1) match {
          case BuildingActor.AmenityStateChange(o, Some(GeneratorControl.Event.Destabilized)) => o eq gen
          case _ => false
        }
      )
      assert(gen.Health == 1)
      assert(!gen.Destroyed)
      assert(gen.Condition == PlanetSideGeneratorState.Destroyed)
      //going to explode state

      //once
      gen.Actor ! CommonMessages.Use(player1, Some(tool)) //repair?
      avatarProbe.expectNoMessage(1000 milliseconds)      //no messages
      buildingProbe.expectNoMessage(200 milliseconds)
      player1Probe.expectNoMessage(200 milliseconds)
      assert(gen.Health == 1)
      //twice
      gen.Actor ! CommonMessages.Use(player1, Some(tool)) //repair?
      avatarProbe.expectNoMessage(1000 milliseconds)      //no messages
      buildingProbe.expectNoMessage(200 milliseconds)
      player1Probe.expectNoMessage(200 milliseconds)
      assert(gen.Health == 1)
    }
  }
}

class GeneratorControlRepairPastRestorePoint extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}
    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref

private val gen = Generator(GeneratorTest.generator_definition) //guid=2
  gen.Position = Vector3(1, 0, 0)
  gen.Actor = system.actorOf(Props(classOf[GeneratorControl], gen), "generator-control")

private val avatar = Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)
    .copy(certifications = Set(Certification.Engineering))
private val player1 = Player(avatar) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                     //<14m from generator; dies
  player1.Spawn()
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref

private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = gen
  building.PlayersInSOI = List(player1)
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref

private val tool = Tool(GlobalDefinitions.nano_dispenser) //4 & 5

  guid.register(building, 1)
  guid.register(gen, 2)
  guid.register(player1, 3)
  guid.register(tool, 4)
  guid.register(tool.AmmoSlot.Box, 5)
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "GeneratorControl" should {
    "send a status update if destroyed and repairing past the restoration point" in {
      val originalHealth = gen.Health = gen.Definition.RepairRestoresAt - 1 //damage
      gen.Condition = PlanetSideGeneratorState.Destroyed //initial state manip
      gen.Destroyed = true
      assert(originalHealth < gen.Definition.DefaultHealth)
      assert(originalHealth < gen.Definition.RepairRestoresAt)
      assert(gen.Destroyed)

      gen.Actor ! CommonMessages.Use(player1, Some(tool)) //repair
      val msg_avatar   = avatarProbe.receiveN(3, 500 milliseconds) //expected
      val msg_building = buildingProbe.receiveOne(200 milliseconds)
      assert(
        msg_avatar.head match {
          case AvatarServiceMessage(
                "TestCharacter1",
                AvatarAction
                  .SendResponse(_, InventoryStateMessage(ValidPlanetSideGUID(5), _, ValidPlanetSideGUID(4), _))
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
                "TestCharacter1",
                AvatarAction.SendResponse(_, RepairMessage(ValidPlanetSideGUID(2), _))
              ) =>
            true
          case _ => false
        }
      )
      assert(
        msg_building match {
          case BuildingActor.AmenityStateChange(o, _) => o eq gen
          case _                              => false
        }
      )
      assert(gen.Condition == PlanetSideGeneratorState.Normal)
      assert(gen.Health > gen.Definition.RepairRestoresAt)
      assert(!gen.Destroyed)
    }
  }
}

object GeneratorTest {
  final val generator_definition = new GeneratorDefinition(352) {
    MaxHealth = 4000
    Damageable = true
    DamageableByFriendlyFire = false
    Repairable = true
    RepairDistance = 13.5f
    RepairIfDestroyed = true
    innateDamage = new DamageWithPosition {
      DamageRadius = 14
    }
    //note: no auto-repair
  }
}
