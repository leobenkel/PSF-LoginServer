// Copyright (c) 2020 PSForever
package net.psforever.objects.serverobject.hackable

class HackableDefinition {
  private var hackable: Boolean = false
  private var magicNumber: Long = 0

private def Hackable: Boolean = hackable

private def Hackable_=(state: Boolean): Boolean = {
    hackable = state
    Hackable
  }

private def MagicNumber: Long = magicNumber

private def MagicNumber_=(magic: Long): Long = {
    magicNumber = magic
    MagicNumber
  }
}
