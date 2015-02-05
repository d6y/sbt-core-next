package sbt

import sbt.ScopeAxis.scopeAxisToScope
import std.TaskStreams
import sbt.serialization._

/**
 * This UI plugin provides the basic settings used by plugins that want to be able to communicate with a UI.
 *
 * Basically, we just stub out the setting you can use to look up the current UI context.
 */
object SbtUIPlugin extends AutoPlugin {

  override def trigger = AllRequirements
  override def requires = plugins.CorePlugin

  override val globalSettings: Seq[Setting[_]] = Seq(
    UIKeys.interactionService in Global <<= (UIKeys.interactionService in Global) ?? CommandLineUIServices,
    UIKeys.sendEventService in Global <<= (UIKeys.sendEventService in Global) ?? CommandLineUIServices,
    UIKeys.registeredProtocolConversions in Global <<= (UIKeys.registeredProtocolConversions in Global) ?? Nil,
    UIKeys.registeredSerializers in Global <<= (UIKeys.registeredSerializers in Global) ?? Nil)

  def registerTaskSerialization[T](key: TaskKey[T])(implicit pickler: Pickler[T], unpickler: Unpickler[T], mf: Manifest[T]): Setting[_] =
    UIKeys.registeredSerializers in Global += RegisteredSerializer(pickler, unpickler, mf)
  def registerSettingSerialization[T](key: SettingKey[T])(implicit pickler: Pickler[T], unpickler: Unpickler[T]): Setting[_] =
    UIKeys.registeredSerializers in Global += RegisteredSerializer(pickler, unpickler, key.key.manifest)
}

private[sbt] object CommandLineUIServices extends SbtPrivateInteractionService with SbtPrivateSendEventService {
  override def readLine(prompt: String, mask: Boolean): Option[String] = {
    val maskChar = if (mask) Some('*') else None
    SimpleReader.readLine(prompt, maskChar)
  }
  // TODO - Implement this better!
  def confirm(msg: String): Boolean = {
    object Assent {
      def unapply(in: String): Boolean = {
        (in == "y" || in == "yes")
      }
    }
    SimpleReader.readLine(msg + " (yes/no): ", None) match {
      case Some(Assent()) => true
      case _ => false
    }
  }
  override def sendEvent[T: Pickler](event: T): Unit = ()
}
