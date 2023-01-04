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
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.types._

import scala.concurrent.duration._

/*
the damage targets, Generator, Terminal, etc., are used to test basic destruction
essentially, treat them more as generic entities whose object types are damageable (because they are)
see specific object type tests in relation to what those object types does above and beyond that during damage
 */
class DamageableEntityDamageTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}

    GUID(guid)
  }
private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
private val gen      = Generator(GlobalDefinitions.generator)                        //guid=2
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
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

  "DamageableEntity" should {
    "handle taking damage" in {
      gen.Actor ! Vitality.Damage(applyDamageTo)
      val msg1 = avatarProbe.receiveOne(500 milliseconds)
      val msg2 = activityProbe.receiveOne(500 milliseconds)
      assert(
        msg1 match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg2 match {
          case activity: Zone.HotSpot.Activity =>
            activity.attacker == PlayerSource(player1) &&
              activity.defender == SourceEntry(gen) &&
              activity.location == Vector3(1, 0, 0)
          case _ => false
        }
      )
    }
  }
}
