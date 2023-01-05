// Copyright (c) 2022 PSForever
package net.psforever.objects.serverobject.terminals.tabs

/**
  * A basic tab outlining the specific type of stock available from this part of the terminal's interface.
  * Defines logic for enumerating items and entities that should be eliminated from being loaded.
  * @see `ItemTransactionMessage`
  */
trait ScrutinizedTab extends Tab {
  private var contraband: Seq[ExclusionRule] = Nil

private def Exclude: Seq[ExclusionRule] = contraband

private def Exclude_=(equipment: ExclusionRule): Seq[ExclusionRule] = {
    contraband = Seq(equipment)
    Exclude
  }

private def Exclude_=(equipmentList: Seq[ExclusionRule]): Seq[ExclusionRule] = {
    contraband = equipmentList
    Exclude
  }
}
