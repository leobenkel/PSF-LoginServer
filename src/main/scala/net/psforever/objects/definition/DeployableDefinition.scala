// Copyright (c) 2017 PSForever
package net.psforever.objects.definition

import akka.actor.ActorContext
import net.psforever.objects.Default
import net.psforever.objects.ce.{Deployable, DeployableCategory, DeployedItem}
import net.psforever.objects.definition.converter.SmallDeployableConverter
import net.psforever.objects.vital.damage.DamageCalculations
import net.psforever.objects.vital.resistance.ResistanceProfileMutators
import net.psforever.objects.vital.resolution.DamageResistanceModel
import net.psforever.objects.vital.{CollisionXYData, NoResistanceSelection, VitalityDefinition}

import scala.concurrent.duration._

object DeployAnimation extends Enumeration {
  type Type = Value

  val None, Standard, Fdu = Value
}

trait BaseDeployableDefinition {
  private var category: DeployableCategory.Value = DeployableCategory.Boomers
  private var deployTime: Long                   = (1 second).toMillis //ms
  var deployAnimation: DeployAnimation.Value     = DeployAnimation.None

private def Item: DeployedItem.Value

private def DeployCategory: DeployableCategory.Value = category

private def DeployCategory_=(cat: DeployableCategory.Value): DeployableCategory.Value = {
    category = cat
    DeployCategory
  }

private def DeployTime: Long = deployTime

private def DeployTime_=(time: FiniteDuration): Long = DeployTime_=(time.toMillis)

private def DeployTime_=(time: Long): Long = {
    deployTime = time
    DeployTime
  }

private def Initialize(obj: Deployable, context: ActorContext): Unit = {}

private def Uninitialize(obj: Deployable, context: ActorContext): Unit = {
    obj.Actor ! akka.actor.PoisonPill
    obj.Actor = Default.Actor
  }
}

abstract class DeployableDefinition(objectId: Int)
    extends ObjectDefinition(objectId)
    with DamageResistanceModel
    with ResistanceProfileMutators
    with VitalityDefinition
    with BaseDeployableDefinition {
  private val item = DeployedItem(objectId) //let throw NoSuchElementException
  DamageUsing = DamageCalculations.AgainstVehicle
  ResistUsing = NoResistanceSelection
  Packet = new SmallDeployableConverter
  registerAs = "deployables"
  collision.xy = new CollisionXYData(List((0f, 100)))

private def Item: DeployedItem.Value = item
}
