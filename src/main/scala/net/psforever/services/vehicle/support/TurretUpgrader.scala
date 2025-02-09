// Copyright (c) 2017 PSForever
package net.psforever.services.vehicle.support

import akka.actor.Cancellable
import net.psforever.objects.equipment.EquipmentSlot
import net.psforever.objects.{AmmoBox, Default, PlanetSideGameObject, Tool}
import net.psforever.objects.guid._
import net.psforever.objects.serverobject.PlanetSideServerObject
import net.psforever.objects.serverobject.turret.{FacilityTurret, TurretUpgrade, WeaponTurret}
import net.psforever.objects.vehicles.MountedWeapons
import net.psforever.objects.zones.Zone
import net.psforever.types.PlanetSideGUID
import net.psforever.services.support.{SimilarityComparator, SupportActor, SupportActorCaseConversions}
import net.psforever.services.vehicle.{VehicleAction, VehicleServiceMessage}

import scala.concurrent.Future
import scala.concurrent.duration._

class TurretUpgrader extends SupportActor[TurretUpgrader.Entry] {
  private var task: Cancellable = Default.Cancellable

  private var list: List[TurretUpgrader.Entry] = List()

  val sameEntryComparator: SimilarityComparator[TurretUpgrader.Entry] =
    new SimilarityComparator[TurretUpgrader.Entry]() {
      def Test(entry1: TurretUpgrader.Entry, entry2: TurretUpgrader.Entry): Boolean = {
        entry1.obj == entry2.obj && entry1.zone == entry2.zone && entry1.obj.GUID == entry2.obj.GUID
      }
    }

  /**
    * Sufficiently clean up the current contents of these waiting removal jobs.
    * Cancel all timers, rush all entries in the lists through their individual steps, then empty the lists.
    * This is an improved `HurryAll`.
    */
  override def postStop(): Unit = {
    super.postStop()
    task.cancel()
    list.foreach {
      UpgradeTurretAmmo
    }
    list = Nil
  }

  def CreateEntry(obj: PlanetSideGameObject, zone: Zone, upgrade: TurretUpgrade.Value, duration: Long) =
    TurretUpgrader.Entry(obj, zone, upgrade, duration)

  def InclusionTest(entry: TurretUpgrader.Entry): Boolean = entry.obj.isInstanceOf[FacilityTurret]

  def receive: Receive =
    entryManagementBehaviors
      .orElse {
        case TurretUpgrader.AddTask(turret, zone, upgrade, duration) =>
          val lengthOfTime = duration.getOrElse(TurretUpgrader.StandardUpgradeLifetime).toNanos
          if (lengthOfTime > (1 second).toNanos) { //don't even bother if it's too short; it'll revert near instantly
            val entry = CreateEntry(turret, zone, TurretUpgrade.None, lengthOfTime)
            UpgradeTurretAmmo(CreateEntry(turret, zone, upgrade, lengthOfTime))
            if (list.isEmpty) {
              //we were the only entry so the event must be started from scratch
              list = List(entry)
              trace(s"a task has been added: $entry")
              RetimeFirstTask()
            } else {
              val oldHead = list.head
              if (!list.exists(test => TurretUpgrader.Similarity(test, entry))) {
                list = (list :+ entry).sortBy(entry => entry.time + entry.duration)
                trace(s"a task has been added: $entry")
                if (oldHead != list.head) {
                  RetimeFirstTask()
                }
              }
            }
          }

        case TurretUpgrader.Downgrade() =>
          task.cancel()
          val now: Long = System.nanoTime
          val (in, out) =
            list.partition(entry => {
              now - entry.time >= entry.duration
            }) //&& entry.obj.Seats.values.count(_.isOccupied) == 0
          list = out
          in.foreach { UpgradeTurretAmmo }
          RetimeFirstTask()

        case _ => ;
      }

  def RetimeFirstTask(now: Long = System.nanoTime): Unit = {
    task.cancel()
    if (list.nonEmpty) {
      val short_timeout: FiniteDuration = math.max(1, list.head.duration - (now - list.head.time)) nanoseconds
      import scala.concurrent.ExecutionContext.Implicits.global
      task = context.system.scheduler.scheduleOnce(short_timeout, self, TurretUpgrader.Downgrade())
    }
  }

  def HurrySpecific(targets: List[PlanetSideGameObject], zone: Zone): Unit = {
    PartitionTargetsFromList(list, targets.map { TurretUpgrader.Entry(_, zone, TurretUpgrade.None, 0) }, zone) match {
      case (Nil, _) =>
        debug(s"no tasks matching the targets $targets have been hurried")
      case (in, out) =>
        debug(s"the following tasks have been hurried: $in")
        in.foreach { UpgradeTurretAmmo }
        list = out //.sortBy(entry => entry.time + entry.duration)
        if (out.nonEmpty) {
          RetimeFirstTask()
        }
    }
  }

  def HurryAll(): Unit = {
    trace("all tasks have been hurried")
    task.cancel()
    list.foreach {
      UpgradeTurretAmmo
    }
    list = Nil
  }

  def ClearSpecific(targets: List[PlanetSideGameObject], zone: Zone): Unit = {
    PartitionTargetsFromList(list, targets.map { TurretUpgrader.Entry(_, zone, TurretUpgrade.None, 0) }, zone) match {
      case (Nil, _) =>
        debug(s"no tasks matching the targets $targets have been cleared")
      case (in, out) =>
        debug(s"the following tasks have been cleared: $in")
        list = out //.sortBy(entry => entry.time + entry.duration)
        if (out.nonEmpty) {
          RetimeFirstTask()
        }
    }
  }

  def ClearAll(): Unit = {
    task.cancel()
    list = Nil
  }

  /**
    * The process of upgrading a turret is nearly complete.
    * After the upgrade status is changed, the internal structure of the turret's weapons change to suit the configuration.
    * Of special importance are internal ammo supplies of the changing weapon,
    * the original ammunition that must be un-registered,
    * and the new boxes that must be registered so the weapon may be introduced into the game world properly.
    * @param entry na
    */
  def UpgradeTurretAmmo(entry: TurretUpgrader.Entry): Unit = {
    val target     = entry.obj.asInstanceOf[FacilityTurret]
    val zone       = entry.zone
    val zoneId     = zone.id
    val upgrade    = entry.upgrade
    val guid       = zone.GUID
    val turretGUID = target.GUID
    //kick all occupying players for duration of conversion
    target.Seats.values
      .filter { _.isOccupied }
      .foreach({ seat =>
        val tplayer = seat.occupant.get
        seat.unmount(tplayer)
        tplayer.VehicleSeated = None
        if (tplayer.HasGUID) {
          context.parent ! VehicleServiceMessage(
            zoneId,
            VehicleAction.KickPassenger(tplayer.GUID, 4, false, turretGUID)
          )
        }
      })
    debug(s"Converting manned wall turret weapon to $upgrade")

    val oldBoxes = AllMountedWeaponMagazines(target)
    target.Upgrade = upgrade //perform upgrade
    val newBoxes = AllMountedWeaponMagazines(target)

    val oldBoxesTask = oldBoxes
      .filterNot { box => newBoxes.exists(_ eq box) }
      .map(box => GUIDTask.unregisterEquipment(guid, box))
      .toList
    import scala.concurrent.ExecutionContext.Implicits.global
    val newBoxesTask = TaskBundle(
      new StraightforwardTask() {
        private val localFunc: () => Unit = FinishUpgradingTurret(entry)

        def action(): Future[Any] = {
          localFunc()
          Future(this)
        }
      },
      newBoxes
        .filterNot { box => oldBoxes.exists(_ eq box) }
        .map(box => GUIDTask.registerEquipment(guid, box))
        .toList
    )
    TaskWorkflow.execute(
      TaskBundle(
        new StraightforwardTask() {
          private val tasks = oldBoxesTask

          def action(): Future[Any] = {
            tasks.foreach { TaskWorkflow.execute }
            Future(this)
          }
        },
        newBoxesTask
      )
    )
  }

  /**
    * From an object that has mounted weapons, parse all of the internal ammunition loaded into all of the weapons.
    * @param target the object with mounted weaponry
    * @return all of the internal ammunition objects
    */
  def AllMountedWeaponMagazines(target: MountedWeapons): Iterable[AmmoBox] = {
    target.Weapons.values
      .map { _.Equipment }
      .collect { case Some(tool: Tool) => tool.AmmoSlots }
      .flatMap { _.map { _.Box } }
  }

  /**
    * Finish upgrading the turret by announcing to other players that the weapon type has changed.
    * By this point, a prior required action that required that new ammunition objects had to be registered.
    * It is now safe to announce that clients can update to the new weapon.
    * @param entry na
    */
  def FinishUpgradingTurret(entry: TurretUpgrader.Entry)(): Unit = {
    val target = entry.obj.asInstanceOf[FacilityTurret]
    val zone   = entry.zone
    trace(s"Wall turret finished ${target.Upgrade} upgrade")
    target.ConfirmUpgrade(entry.upgrade)
    val targetGUID = target.GUID
    if (target.Health > 0) {
      target.Weapons
        .map({ case (index: Int, slot: EquipmentSlot) => (index, slot.Equipment) })
        .collect {
          case (index, Some(tool: Tool)) =>
            context.parent ! VehicleServiceMessage(
              zone.id,
              VehicleAction.EquipmentInSlot(PlanetSideGUID(0), targetGUID, index, tool)
            )
        }
    }
  }
}

object TurretUpgrader extends SupportActorCaseConversions {
  private val StandardUpgradeLifetime: FiniteDuration = 30 minutes

  /**
    * All information necessary to apply to the removal process to produce an effect.
    * Internally, all entries have a "time created" field.
    * @param _obj the target
    * @param _zone the zone in which this target is registered
    * @param upgrade the next upgrade state for this turret
    * @param _duration how much longer the target will exist in its current state (in nanoseconds)
    */
  case class Entry(_obj: PlanetSideGameObject, _zone: Zone, upgrade: TurretUpgrade.Value, _duration: Long)
      extends SupportActor.Entry(_obj, _zone, _duration)

  final case class AddTask(
      turret: PlanetSideServerObject with WeaponTurret,
      zone: Zone,
      upgrade: TurretUpgrade.Value,
      duration: Option[FiniteDuration] = None
  )

  final case class Downgrade()

  private def Similarity(entry1: TurretUpgrader.Entry, entry2: TurretUpgrader.Entry): Boolean = {
    entry1.obj == entry2.obj && entry1.zone == entry2.zone && entry1.obj.GUID == entry2.obj.GUID
  }
}
