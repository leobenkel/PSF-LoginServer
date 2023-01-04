package objects.damageable

import akka.actor.Props
import akka.testkit.TestProbe
import base.ActorTest
import net.psforever.objects._
import net.psforever.objects.avatar.Avatar
import net.psforever.objects.ballistics._
import net.psforever.objects.definition.ProjectileDefinition
import net.psforever.objects.guid.NumberPoolHub
import net.psforever.objects.guid.source.MaxNumberSource
import net.psforever.objects.serverobject.structures.{Building, StructureType}
import net.psforever.objects.serverobject.terminals.{Terminal, TerminalControl}
import net.psforever.objects.vital.Vitality
import net.psforever.objects.vital.base.DamageResolution
import net.psforever.objects.vital.interaction.DamageInteraction
import net.psforever.objects.vital.projectile.ProjectileReason
import net.psforever.objects.vital.resolution.ResolutionCalculations.Output
import net.psforever.objects.zones.{Zone, ZoneMap}
import net.psforever.services.avatar.{AvatarAction, AvatarServiceMessage}
import net.psforever.types._

import scala.concurrent.duration._

class DamageableAmenityTest extends ActorTest {
  private val guid: NumberPoolHub = new NumberPoolHub(new MaxNumberSource(10))
  private val zone: Zone = new Zone("test", new ZoneMap("test"), 0) {
    override def SetupNumberPools(): Unit = {}

    GUID(guid)
  }
  private val building: Building = Building("test-building", 1, 1, zone, StructureType.Facility) //guid=1
  private val term: Terminal     = Terminal(GlobalDefinitions.order_terminal)                    //guid=2
  private val player1: Player =
    Player(Avatar(0, "TestCharacter1", PlanetSideEmpire.TR, CharacterSex.Male, 0, CharacterVoice.Mute)) //guid=3
  player1.Spawn()
  guid.register(building, 1)
  guid.register(term, 2)
  guid.register(player1, 3)
  building.Position = Vector3(1, 0, 0)
  building.Zone = zone
  building.Amenities = term
  term.Position = Vector3(1, 0, 0)
  term.Actor = system.actorOf(Props(classOf[TerminalControl], term), "terminal-control")
  private val activityProbe: TestProbe = TestProbe()
  private val avatarProbe: TestProbe   = TestProbe()
  private val buildingProbe: TestProbe = TestProbe()
  zone.Activity = activityProbe.ref
  zone.AvatarEvents = avatarProbe.ref
  building.Actor = buildingProbe.ref

  private val weapon: Tool                     = Tool(GlobalDefinitions.phoenix) //decimator
  private val projectile: ProjectileDefinition = weapon.Projectile
  private val resolved: DamageInteraction = DamageInteraction(
    target = SourceEntry(term),
    cause = ProjectileReason(
      resolution = DamageResolution.Hit,
      projectile = Projectile(
        profile = projectile,
        tool_def = weapon.Definition,
        fire_mode = weapon.FireMode,
        owner = PlayerSource(player1),
        attribute_to = 0,
        shot_origin = Vector3(2, 0, 0),
        shot_angle = Vector3(-1, 0, 0)
      ),
      damageModel = term.DamageModel
    ),
    hitPos = Vector3(1, 0, 0)
  )
  private val applyDamageTo: Output = resolved.calculate()
  expectNoMessage(200 milliseconds)
  //we're not testing that the math is correct

  "DamageableAmenity" should {
    "send de-initialization messages upon destruction" in {
      //the decimator does enough damage to one-shot this terminal from any initial health
      term.Health = term.Definition.DamageDestroysAt + 1
      assert(term.Health > term.Definition.DamageDestroysAt)
      assert(!term.Destroyed)

      term.Actor ! Vitality.Damage(applyDamageTo)
      val msg1234 = avatarProbe.receiveN(4, 500 milliseconds)
      assert(
        msg1234.head match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 0, _)) => true
          case _                                                                                            => false
        }
      )
      assert(
        msg1234(1) match {
          case AvatarServiceMessage("test", AvatarAction.Destroy(PlanetSideGUID(2), _, _, Vector3(1, 0, 0))) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg1234(2) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 50, 1)) => true
          case _                                                                                             => false
        }
      )
      assert(
        msg1234(3) match {
          case AvatarServiceMessage("test", AvatarAction.PlanetsideAttributeToAll(PlanetSideGUID(2), 51, 1)) => true
          case _                                                                                             => false
        }
      )
      assert(term.Health <= term.Definition.DamageDestroysAt)
      assert(term.Destroyed)
    }
  }
}
