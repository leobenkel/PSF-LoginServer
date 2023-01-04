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

class DamageableMountableDestroyTest extends ActorTest {
  //TODO this test with not send HitHint packets because LivePlayers is not being allocated for the players in the zone
private val guid = new NumberPoolHub(new MaxNumberSource(10))
private val zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools() = {}

    GUID(guid)
  }
private val building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
private val mech     = ImplantTerminalMech(GlobalDefinitions.implant_terminal_mech)  //guid=2
private val player1 =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Spawn()
  player1.Position = Vector3(2, 2, 2)
private val player1Probe = TestProbe()
  player1.Actor = player1Probe.ref
private val player2 =
    Player(Avatar(0, "TestCharacter2", PlanetSideEmpire.NC, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=4
  player2.Spawn()
private val player2Probe = TestProbe()
  player2.Actor = player2Probe.ref
  guid.register(building, 1)
  guid.register(mech, 2)
  guid.register(player1, 3)
  guid.register(player2, 4)
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = mech
  mech.Position = Vector3(1, 0, 0)
  mech.Actor = system.actorOf(Props(classOf[ImplantTerminalMechControl], mech), "mech-control")
private val activityProbe = TestProbe()
private val avatarProbe   = TestProbe()
private val buildingProbe = TestProbe()
  zone.Activity = activityProbe.ref
  zone.AvatarEvents = avatarProbe.ref
  building.Actor = buildingProbe.ref
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
  mech.Seats(0).mount(player2)            //mount the player
  player2.VehicleSeated = Some(mech.GUID) //mount the player
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "DamageableMountable" should {
    "alert seated occupants that the mountable object has been destroyed and that they should have died" in {
      val originalHealth = mech.Health = mech.Definition.DamageDestroysAt + 1 //initial state manip
      assert(originalHealth > mech.Definition.DamageDestroysAt)
      assert(!mech.Destroyed)

      mech.Actor ! Vitality.Damage(applyDamageTo)
      val msg12 = avatarProbe.receiveN(2, 500 milliseconds)
      player1Probe.expectNoMessage(500 milliseconds)
      val msg3 = player2Probe.receiveOne(200 milliseconds)
      assert(
        msg12.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg12(1) match {
          case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(2), _, _, Vector3(1, 0, 0))) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg3 match {
          case Player.Die(_) => true
          case _             => false
        }
      )
      assert(mech.Health <= mech.Definition.DamageDestroysAt)
      assert(mech.Destroyed)
    }
  }
}
