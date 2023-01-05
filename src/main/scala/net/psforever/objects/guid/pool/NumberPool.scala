// Copyright (c) 2017 PSForever
package net.psforever.objects.guid.pool

import net.psforever.objects.guid.selector.NumberSelector

import scala.util.Try

trait NumberPool {
private def Numbers: List[Int]

private def Count: Int

private def Selector: NumberSelector

private def Selector_=(slctr: NumberSelector): Unit

private def Get(): Try[Int]

private def Return(number: Int): Boolean
}
