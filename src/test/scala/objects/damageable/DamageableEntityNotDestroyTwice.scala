package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.serverobject.generator.{Generator, GeneratorControl}
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.vital.Vitality
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.types._

import scala.concurrent.duration._

class DamageableEntityNotDestroyTwice extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(10))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}

    GUID(guid)
  }
private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
private val gen      = Generator(GlobalDefinitions.generator)                        //guid=2
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

  "DamageableEntity" should {
    "not be destroyed twice (skirting around the 'damage-destroys-at' value)" in {
      val originalHealth = gen.Health = gen.Definition.DamageDestroysAt + 1 //damaged, not yet restored
      gen.Condition = PlanetSideGeneratorState.Destroyed //initial state manip
      gen.Destroyed = true
      assert(gen.Destroyed)
      assert(originalHealth < gen.Definition.DefaultHealth)
      assert(originalHealth < gen.Definition.RepairRestoresAt)
      assert(originalHealth > gen.Definition.DamageDestroysAt)

      gen.Actor ! Vitality.Damage(applyDamageTo)
      avatarProbe.receiveOne(500 milliseconds)      //only one message
      avatarProbe.expectNoMessage(500 milliseconds) //only one message
      activityProbe.receiveOne(500 milliseconds)    //triggers activity hotspot, like it's not a killing blow
      assert(gen.Health < originalHealth)
      assert(gen.Destroyed)
      assert(originalHealth < gen.Definition.DefaultHealth)
      assert(originalHealth < gen.Definition.RepairRestoresAt)
      assert(gen.Health <= gen.Definition.DamageDestroysAt)
    }
  }
}
