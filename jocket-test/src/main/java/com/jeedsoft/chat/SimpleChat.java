package com.jeedsoft.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.endpoint.JocketEndpoint;
import com.jeedsoft.jocket.endpoint.JocketServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JocketServerEndpoint("/chat/simple")
public class SimpleChat implements JocketEndpoint
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleChat.class);

    private static final AtomicInteger serial = new AtomicInteger();
	
	private static final Map<String, JocketSession> sessions = new HashMap<>();

	private static final String KEY_USER_NAME = "user";
	
	@Override
	public void onOpen(JocketSession session, HttpSession httpSession)
	{
		synchronized (sessions) {
			sessions.put(session.getId(), session);
			String userName = "User" + serial.incrementAndGet();
            logger.info("Jocket opened: userName={}, session class={}", userName, session.getClass());
			session.setAttribute(KEY_USER_NAME, userName);
			JSONObject message = new JSONObject();
			message.put("senderType", "system");
			message.put("content", userName + " has connected. (total online: " + sessions.size() + ")");
			for (JocketSession item: sessions.values()) {
				item.send(null, message);
			}
		}
	}

	@Override
	public void onClose(JocketSession session, JocketCloseReason closeReason)
	{
		synchronized (sessions) {
			sessions.remove(session.getId());
			String userName = session.getAttribute(KEY_USER_NAME);
			JSONObject message = new JSONObject();
			message.put("senderType", "system");
			message.put("content", userName + " has disconnected. (total online: " + sessions.size() + ")");
			for (JocketSession item: sessions.values()) {
				item.send(null, message);
			}
		}
	}

	@Override
	public void onMessage(JocketSession session, String name, String data)
	{
		String content = new JSONObject(data).getString("content");
		String userName = session.getAttribute(KEY_USER_NAME);
		synchronized (sessions) {
			for (JocketSession item: sessions.values()) {
				boolean isSelf = item.getId().equals(session.getId());
				JSONObject message = new JSONObject();
				message.put("senderType", isSelf ? "self" : "user");
				message.put("content", isSelf ? content : userName + ": " + content);
				item.send(null, message);
			}
		}
	}
}
