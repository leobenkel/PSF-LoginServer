// Copyright (c) 2017 PSForever
package net.psforever

class ObjectFinalizedException(msg: String) extends Exception(msg)

trait IFinalizable {
private var closed = false

private def close = {
    closed = true
  }

private def assertNotClosed = {
    if (closed)
      throw new ObjectFinalizedException(
        this.getClass.getCanonicalName + ": already finalized. Cannot interact with object"
      )
  }

  override def finalize() = {
    if (!closed)
      println(this.getClass.getCanonicalName + ": class not closed. memory leaked")
  }
}
