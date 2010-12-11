/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package funicular

class ClockUseException(message: String) extends RuntimeException(message) {
	def this() = this("clock use exception")
}
