// Copyright (c) 2020 PSForever
package net.psforever.objects.serverobject.transfer

import akka.actor.Actor

trait TransferBehavior {
  _: Actor =>
  protected var transferEvent: TransferBehavior.Event.Value = TransferBehavior.Event.None
  protected var transferTarget: Option[TransferContainer]   = None
  protected var findChargeTargetFunc: (TransferContainer, Option[TransferContainer]) => Option[TransferContainer] =
    TransferBehavior.FindNoTargets
  protected var findDischargeTargetFunc: (TransferContainer, Option[TransferContainer]) => Option[TransferContainer] =
    TransferBehavior.FindNoTargets

private def TransferMaterial: TransferContainer.TransferMaterial
private def ChargeTransferObject: TransferContainer

  val transferBehavior: Receive = {
    case TransferBehavior.Charging(mat) if mat != TransferMaterial =>
      TryStopChargingEvent(ChargeTransferObject)

    case TransferBehavior.Charging(_)
        if transferEvent == TransferBehavior.Event.None || transferEvent == TransferBehavior.Event.Charging =>
      TryChargingActivity()

    case TransferBehavior.Discharging(mat) if mat != TransferMaterial =>
      TryStopChargingEvent(ChargeTransferObject)

    case TransferBehavior.Discharging(_)
        if transferEvent == TransferBehavior.Event.None || transferEvent == TransferBehavior.Event.Discharging =>
      TryDischargingActivity()

    case TransferBehavior.Stopping() =>
      TryStopChargingEvent(ChargeTransferObject)
  }

  /* Charging */
private def TryChargingActivity(): Unit = {
    if (transferEvent != TransferBehavior.Event.Discharging) {
      val chargeable = ChargeTransferObject
      findChargeTargetFunc(chargeable, transferTarget) match {
        case Some(obj) =>
          HandleChargingEvent(obj)
        case None if transferEvent == TransferBehavior.Event.Charging =>
          TryStopChargingEvent(chargeable)
        case _ => ;
      }
    }
  }

private def HandleChargingEvent(target: TransferContainer): Boolean

  /* Discharging */
private def TryDischargingActivity(): Unit = {
    if (transferEvent != TransferBehavior.Event.Charging) {
      val chargeable = ChargeTransferObject
      //determine how close we are to something that we can discharge into
      findDischargeTargetFunc(chargeable, transferTarget) match {
        case Some(obj) =>
          HandleDischargingEvent(obj)
        case None if transferEvent == TransferBehavior.Event.Discharging =>
          TryStopChargingEvent(chargeable)
        case _ => ;
      }
    }
  }

private def HandleDischargingEvent(target: TransferContainer): Boolean

  /* Stopping */
private def TryStopChargingEvent(container: TransferContainer): Unit = {
    transferEvent = TransferBehavior.Event.None
    transferTarget match {
      case Some(_: net.psforever.objects.serverobject.structures.WarpGate) => ;
      case Some(obj) =>
        obj.Actor ! TransferBehavior.Stopping()
      case _ => ;
    }
    transferTarget = None
  }
}

object TransferBehavior {
  object Event extends Enumeration {
    val None, Charging, Discharging = Value
  }

  sealed trait Command

  /**
    * Message to cue a process of transferring into oneself.
    */
  final case class Charging(transferMaterial: Any) extends Command

  /**
    * Message to cue a process of transferring from oneself.
    */
  final case class Discharging(transferMaterial: Any) extends Command

  /**
    * Message to cue a stopping the transfer process.
    */
  final case class Stopping() extends Command

  /**
    * A default search function that does not actually search for anything or ever find anything.
    * @param obj an entity designated as the "origin"
    * @param optionalTarget an optional entity that can be one of the discovered targets
    * @return always returns `None`
    */
private def FindNoTargets(obj: TransferContainer, optionalTarget: Option[TransferContainer]): Option[TransferContainer] = None
}
