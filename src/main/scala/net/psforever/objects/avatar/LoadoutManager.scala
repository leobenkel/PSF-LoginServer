// Copyright (c) 2019 PSForever
package net.psforever.objects.avatar

import net.psforever.objects.loadouts.Loadout

import scala.util.Success

class LoadoutManager(size: Int) {
  private val entries: Array[Option[Loadout]] = Array.fill[Option[Loadout]](size)(None)

private def SaveLoadout(owner: Any, label: String, line: Int): Unit = {
    Loadout.Create(owner, label) match {
      case Success(loadout) if entries.length > line =>
        entries(line) = Some(loadout)
      case _ => ;
    }
  }

private def LoadLoadout(line: Int): Option[Loadout] = entries.lift(line).flatten

private def DeleteLoadout(line: Int): Unit = {
    if (entries.length > line) {
      entries(line) = None
    }
  }

private def Loadouts: Seq[(Int, Loadout)] =
    entries.zipWithIndex.collect { case (Some(loadout), index) => (index, loadout) } toSeq
}
