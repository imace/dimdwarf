// Copyright © 2008-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://dimdwarf.sourceforge.net/LICENSE

package net.orfjackal.dimdwarf.net

import org.hamcrest.Matchers._
import org.hamcrest.MatcherAssert.assertThat
import net.orfjackal.dimdwarf.mq.MessageQueue
import net.orfjackal.dimdwarf.auth._
import net.orfjackal.dimdwarf.actors._
import net.orfjackal.dimdwarf.net.sgs._
import net.orfjackal.dimdwarf.domain._
import org.specsy.scala.ScalaSpecsy

class LoginLogoutSpec extends ScalaSpecsy {
  val queues = new DeterministicMessageQueues
  val authenticator = new FakeAuthenticator
  val clock = new Clock(SimpleTimestamp(0L))
  val networkActor = new DummyNetworkActor()

  val toNetwork = new MessageQueue[NetworkMessage]("toNetwork")
  queues.addActor(networkActor, toNetwork)
  val networkCtrl = new NetworkController(toNetwork, authenticator, null, clock)
  queues.addController(networkCtrl)

  val USERNAME = "John Doe"
  val PASSWORD = "secret"
  val WRONG_PASSWORD = "wrong-password"
  val SESSION = DummySessionHandle(1)


  "When client send a login request with right credentials" >> {
    clientSends(LoginRequest(USERNAME, PASSWORD))

    "a login success message is sent to the client" >> {
      assertMessageSent(toNetwork, SendToClient(LoginSuccess(), SESSION))
    }
  }

  "When client send a login request with wrong credentials" >> {
    clientSends(LoginRequest(USERNAME, WRONG_PASSWORD))

    "a login failure message is sent to the client" >> {
      assertMessageSent(toNetwork, SendToClient(LoginFailure(), SESSION))
    }
  }

  "When client sends a logout request" >> {
    clientSends(LoginRequest(USERNAME, PASSWORD))
    clientSends(LogoutRequest())

    // TODO: keep track of which clients are connected (cam be test-driven with JMX monitoring or session messages)
    "NetworkController logs out the client"

    "a logout success message is sent to the client" >> {
      assertMessageSent(toNetwork, SendToClient(LogoutSuccess(), SESSION))
    }

    // TODO: the client is disconnected after a timeout, if it doesn't disconnect by itself as it should
  }

  // TODO: when a client is not logged in, do not allow a logout request (or any other messages)

  private def assertMessageSent(queue: MessageQueue[NetworkMessage], expected: Any) {
    assertThat(queues.seenIn(queue).last, is(expected))
  }

  private def clientSends(message: ClientMessage) {
    queues.toHub.send(ReceivedFromClient(message, SESSION))
    queues.processMessagesUntilIdle()
  }

  class FakeAuthenticator extends Authenticator {
    def isUserAuthenticated(credentials: Credentials, onYes: => Unit, onNo: => Unit) {
      if (credentials == new PasswordCredentials(USERNAME, PASSWORD)) {
        onYes
      } else {
        onNo
      }
    }
  }

  class DummyNetworkActor extends Actor[NetworkMessage] {
    def start() {}

    def process(message: NetworkMessage) {}
  }

  case class DummySessionHandle(id: Int) extends SessionHandle
}
