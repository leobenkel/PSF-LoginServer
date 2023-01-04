package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.serverobject.terminals.implant.{ImplantTerminalMech, ImplantTerminalMechControl}
import net.psforever.objects.vital.Vitality
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.types._

import scala.concurrent.duration._

class DamageableEntityDestroyedTest extends ActorTest {
private val guid = new NumberPoolHub(new MaxNumberSource(5))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}

    GUID(guid)
  }
private val avatarProbe = TestProbe()
  zone.AvatarEvents = avatarProbe.ref
private val activityProbe = TestProbe()
  zone.Activity = activityProbe.ref
private val mech = ImplantTerminalMech(GlobalDefinitions.implant_terminal_mech) //guid=2
  mech.Position = Vector3(1, 0, 0)
  mech.Actor = system.actorOf(Props(classOf[ImplantTerminalMechControl], mech), "mech-control")
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Position = Vector3(14, 0, 0)                                                                  //<14m from generator; dies
  player1.Spawn()
private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = mech
private val buildingProbe = TestProbe()
  building.Actor = buildingProbe.ref
  guid.register(building, 1)
  guid.register(mech, 2)
  guid.register(player1, 3)
private val weapon     = Tool(GlobalDefinitions.phoenix) //decimator
private val projectile = weapon.Projectile
private val resolved = DamageInteraction(
    SourceEntry(mech),
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
      mech.DamageModel
    ),
    Vector3(1, 0, 0)
  )
private val applyDamageTo = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "DamageableEntity" should {
    "manage taking damage until being destroyed" in {
      mech.Health = 1 //no matter what, the next shot destoys it
      assert(mech.Health == 1)
      assert(!mech.Destroyed)

      mech.Actor ! Vitality.Damage(applyDamageTo)
      val msg1_2 = avatarProbe.receiveN(2, 500 milliseconds)
      assert(
        msg1_2.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg1_2(1) match {
          case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(2), _, _, Vector3(1, 0, 0))) => true
          case _                                                                                             => false
        }
      )
      assert(mech.Health == 0)
      assert(mech.Destroyed)
    }
  }
}
