// Copyright (c) 2022 PSForever
package net.psforever.objects.serverobject.terminals.tabs

import akka.actor.ActorRef
import net.psforever.objects.Player
import net.psforever.objects.serverobject.terminals.Terminal
import net.psforever.packet.game.ItemTransactionMessage

/**
  * A basic tab outlining the specific type of stock available from this part of the terminal's interface.
  * @see `ItemTransactionMessage`
  */
trait Tab {
private def Buy(player: Player, msg: ItemTransactionMessage): Terminal.Exchange
private def Sell(player: Player, msg: ItemTransactionMessage): Terminal.Exchange = Terminal.NoDeal()
private def Dispatch(sender: ActorRef, terminal: Terminal, msg: Terminal.TerminalMessage): Unit
}
