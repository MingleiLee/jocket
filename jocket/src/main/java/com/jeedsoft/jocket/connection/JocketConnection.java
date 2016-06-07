package com.jeedsoft.jocket.connection;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.transport.polling.JocketPollingConnection;

public abstract class JocketConnection
{
	private static final Logger logger = LoggerFactory.getLogger(JocketConnection.class);

	private JocketSession session;

	private boolean active = true;

	public JocketConnection(JocketSession session)
	{
		this.session = session;
	}

	public JocketSession getSession()
	{
		return session;
	}

	public void setSession(JocketSession session)
	{
		this.session = session;
	}

	public String getSessionId()
	{
		return session.getId();
	}
	
	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public void onEvent(JocketPacket event)
	{
		String type = event.getType();
		if (JocketPacket.TYPE_PING.equals(type)) {
			try {
				session.setHeartbeating(false);
				downstream(new JocketPacket(JocketPacket.TYPE_PONG));
			}
			catch (IOException e) {
				logger.error("[Jocket] Failed to send PONG to client: sid=" + getSessionId(), e);
			}
		}
		else if (JocketPacket.TYPE_CLOSE.equals(type)) {
			try {
				String sessionId = getSessionId();
				JocketConnectionManager.remove(sessionId);
				JocketSessionManager.remove(sessionId);
				close(JocketCloseReason.parse(event.getData()));
			}
			catch (IOException e) {
				logger.error("[Jocket] Failed to close connection: sid=" + getSessionId(), e);
			}
		}
		else if (JocketPacket.TYPE_UPGRADE.equals(type)) {
			if (this instanceof JocketPollingConnection) {
				((JocketPollingConnection)this).closeOnUpgrade();
			}
		}
	}

	/**
	 * Send downstream message to client
	 * @param packet The message to be sent
	 * @throws IOException
	 */
	public abstract void downstream(JocketPacket packet) throws IOException;

	/**
	 * Close the alive connection when session is closed
	 * @param reason The close reason
	 * @throws IOException
	 */
	public abstract void close(JocketCloseReason reason) throws IOException;
}
