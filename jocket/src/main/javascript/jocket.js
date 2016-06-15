/**
 * JavaScript library for Jocket
 * 
 * Notice: The methods start with '_' is private, and should not be used by user.
 */

//-----------------------------------------------------------------------
// Jocket
//-----------------------------------------------------------------------

/**
 * Create a Jocket object.
 * 
 * @param url Jocket URL. acceptable formats:
 *     1. URL starts with http/https. for example: http://www.aaa.com/bbb/chat
 *     2. URL starts with slash(/). for example: /abc, /abc/test
 *        In this case, Jocket and the current page should share a same back-end server.
 * @param options Jocket options. available properties:
 *     autoOpen : Auto connect to server when Jocket object is created.
 *                default: true
 *     reconnect: Auto reconnect when connect is closed due to network or other errors.
 *                default: true
 */
var Jocket = function(url, options)
{
	if (url == null || url == "") {
		throw new Error("The URL should not be empty");
	}
	var index = url.indexOf("?");
	var path = index == -1 ? url : url.substring(0, index);
	var args = index == -1 ? null : url.substring(index + 1);
	if (/^http(s?):/.test(url)) {
		this._createUrl = path + ".jocket" + (args == null ? "" : "?" + args);
	}
	else {
		var pageDir = location.href.replace(/[\?#].*/, "").replace(/[^\/]+$/, "");
		var createUrl = pageDir + "create.jocket?jocket_path=" + encodeURIComponent(path);
		this._createUrl = createUrl + (args == null ? "" : "&" + args);
	}
	this.status = Jocket.STATUS_NEW;
	this.options = options || {};
	this._timers = {};
	this._reconnectDelay = Jocket.util.getProperty("reconnectDelay", this.options, 1000);
	this._reconnectDelayMax = Jocket.util.getProperty("reconnectDelayMax", this.options, 20000);
	this._reconnectCount = 0;
	this._listeners = {};
	if (this.options.autoOpen != false) {
		this.open();
	}
};

/**
 * Open the Jocket connection.
 * 
 * Steps:
 * 1. Apply a Jocket session (*NOT* HTTP session) ID though HTTP
 * 2. Handshake through HTTP.
 *     2.1. If failed, close the Jocket object (close event will be fired)
 *     2.2. If succeed, open the Jocket session (open event will be fired, you can send/receive message now)
 *          And try to open WebSocket connection and handshake
 *         2.2.1. If failed, keep to use HTTP to send/receive message
 *         2.2.2. If succeed, use WebSocket instead of HTTP (upgrade the transport to WebSocket)
 */
Jocket.prototype.open = function()
{
	var jocket = this;
	if (jocket.status != Jocket.STATUS_NEW && jocket.status != Jocket.STATUS_CLOSED) {
		throw new Error("Jocket is already opening/open.");
	}
	jocket.status = Jocket.STATUS_OPENING;
	var ajax = new Jocket.Ajax(jocket._createUrl);
	ajax.timeout = 20000;
	ajax.timeParamName = "jocket_time";
	ajax.onsuccess = function(response) {
		Jocket.logger.debug("Session created: sid=%s", response.sessionId);
		jocket.sessionId = response.sessionId;
		jocket._needUpgrade = Jocket.util.getProperty("upgrade", jocket.options, response, true);
		jocket._pingInterval = Jocket.util.getProperty("pingInterval", response, 25000);
		jocket._pingTimeout = Jocket.util.getProperty("pingTimeout", response, 20000);
		var url = jocket._createUrl.replace(/\.jocket.*/, "");
		for (var i = 0; i < response.pathDepth; ++i) {
			url = url.replace(/\/[^\/]*$/, "");
		}
		jocket._rootUrl = url + "/";
		jocket._probing = new Jocket.Polling(jocket);
		jocket._probing.start();
	};
	ajax.onfailure = function(event, status) {
		Jocket.logger.error("Failed to create session: status=%d, result=%o", status, ajax.result);
		jocket._close(Jocket.CLOSE_CREATE_FAILED, "Failed to create session");
	};
	ajax.submit();
};

/**
 * Close the Jocket connection.
 * 
 * If the connection is already closed, the invocation is ignored. Otherwise:
 * 1. A packet with type='close' will be sent to server.
 * 2. The 'close' event listeners bounded to this Jocket object will be invoked. 
 */
Jocket.prototype.close = function()
{
	this._close(Jocket.CLOSE_NORMAL, "close by user");
};

/**
 * Send message to server.
 * 
 * @param data The message body. The data type should be supported by JSON.stringify()
 * @param name [optional] The message name(type). If there are multiple message types, you can either
 *     enclose the type in data, or use the name parameter directly.
 */
Jocket.prototype.send = function(name, data)
{
	var packet = {type:Jocket.PACKET_TYPE_MESSAGE};
	if (data != null) {
		packet.data = JSON.stringify(data);
	}
	if (name != null) {
		packet.name = name;
	}
	return this._sendPacket(packet);
};

/**
 * Test whether the Jocket connection is open (ready to send/receive messages)
 */
Jocket.prototype.isOpen = function()
{
	return this._transport != null;
};

/**
 * Add event listener
 * 
 * @param name The event name
 * @param listener The event listener (a JavaScript function)
 */
Jocket.prototype.on = Jocket.prototype.addEventListener = function(name, listener)
{
	this._listeners[name] = this._listeners[name] || [];
	this._listeners[name].push(listener);
	return this;
};

Jocket.prototype._close = function(code, message)
{
	if (this.status == Jocket.STATUS_CLOSED) {
		return;
	}
	this.status = Jocket.STATUS_CLOSED;
	Jocket.util.clearTimers(this);
	if (this._transport != null) {
		this._transport.destroy(code, message);
		delete this._transport;
	}
	if (this._probing != null) {
		this._probing.destroy(code, message);
		delete this._probing;
	}
	this._fire(Jocket.EVENT_CLOSE, {code:code, message:message});
	Jocket.logger.debug("Session closed: sid=%s, code=%d, message=%s", this.sessionId, code, message);
	this.sessionId = null;
	if (this.options.reconnect != false) {
		var codes = {};
		codes[Jocket.CLOSE_NORMAL] = 1;
		codes[Jocket.CLOSE_AWAY] = 1;
		codes[Jocket.CLOSE_NO_SESSION_PARAM] = 1;
		if (!(code in codes)) {
			this._reconnect();
		}
	}
};

Jocket.prototype._closeTransport = function(transport, code, message)
{
	if (transport == this._transport) {
		this._close(code, message);
		return;
	}
	if (transport == this._probing) {
		delete this._probing;
	}
	transport.destroy(code, message);
};

Jocket.prototype._sendPacket = function(packet)
{
	if (this._transport == null) {
		Jocket.logger.error("Packet ignored: sid=%s, packet=%o", this.sessionId, packet);
		return false;
	}
	this._transport.sendPacket(packet);
	return true;
};

Jocket.prototype._reconnect = function()
{
	var jocket = this;
	++jocket._reconnectCount;
	var delay = jocket._reconnectDelay;
	for (var i = 1; i < jocket._reconnectCount; ++i) {
		delay = Math.min(delay * 2, jocket._reconnectDelayMax);
		if (delay == jocket._reconnectDelayMax) {
			break;
		}
	}
	jocket._timers.reconnect = setTimeout(function() {
		Jocket.logger.debug("Reconnect: count=%d", jocket._reconnectCount);
		delete jocket._timers.reconnect;
		jocket.open();
	}, delay);
};

Jocket.prototype._upgrade = function()
{
	Jocket.logger.debug("Upgrade transport to WebSocket: sid=%s", this.sessionId);
	var polling = this._transport;
	this._transport = this._probing; 
	delete this._probing;
	polling.stop();
	this._sendPacket({type:Jocket.EVENT_UPGRADE});
};

Jocket.prototype._fire = function(name)
{
	var listeners = this._listeners[name];
	if (name in this._listeners) {
		var listeners = this._listeners[name].slice(0);
		var args = Array.prototype.slice.call(arguments, 1);
		for (var i = 0; i < listeners.length; ++i) {
			listeners[i].apply(this, args);
		}
	}
};

Jocket.prototype._getPollingUrl = function()
{
	return this._rootUrl + "jocket?s=" + this.sessionId;
};

Jocket.prototype._getWebSocketUrl = function()
{
	return this._rootUrl.replace(/^http/, "ws") + "jocket-ws?s=" + this.sessionId;
};

Jocket.STATUS_NEW     = "new";
Jocket.STATUS_OPENING = "opening";
Jocket.STATUS_OPEN    = "open";
Jocket.STATUS_CLOSED  = "closed";

Jocket.PACKET_TYPE_OPEN    = "open";
Jocket.PACKET_TYPE_CLOSE   = "close";
Jocket.PACKET_TYPE_PING    = "ping";
Jocket.PACKET_TYPE_PONG    = "pong";
Jocket.PACKET_TYPE_NOOP    = "noop";
Jocket.PACKET_TYPE_MESSAGE = "message";

Jocket.CLOSE_NORMAL            = 1000; //normal closure
Jocket.CLOSE_AWAY              = 1001; //close browser or reload page
Jocket.CLOSE_ABNORMAL          = 1006; //network error
Jocket.CLOSE_NO_SESSION_PARAM  = 3600; //the Jocket session ID parameter is missing
Jocket.CLOSE_SESSION_NOT_FOUND = 3601; //the Jocket session is not found
Jocket.CLOSE_CREATE_FAILED     = 3602; //failed to create Jocket session
Jocket.CLOSE_CONNECT_FAILED    = 3603; //failed to connect to server
Jocket.CLOSE_PING_TIMEOUT      = 3604; //ping timeout
Jocket.CLOSE_POLLING_FAILED    = 3605; //polling failed

Jocket.EVENT_OPEN    = "open";
Jocket.EVENT_CLOSE   = "close";
Jocket.EVENT_UPGRADE = "upgrade";
Jocket.EVENT_ERROR   = "error";
Jocket.EVENT_MESSAGE = "message";

//-----------------------------------------------------------------------
// Jocket.Ws
//-----------------------------------------------------------------------

Jocket.Ws = function(jocket)
{
	this.jocket = jocket;
	this._timers = {};
};

Jocket.Ws.prototype.start = function()
{
	var ws = this;
	var jocket = ws.jocket;
	ws.socket = new WebSocket(jocket._getWebSocketUrl());
	ws.socket.onopen = function() {
		Jocket.logger.debug("WebSocket opened.");
		ws.ping();
	};
	ws.socket.onclose = function(event) {
		Jocket.logger.debug("WebSocket closed: event=%o", event);
		jocket._closeTransport(ws, event.code, event.reason);
	};
	ws.socket.onerror = Jocket.Ws.doSocketError = function(event) {
		Jocket.logger.error("WebSocket error: event=%o", event);
		jocket._fire(Jocket.EVENT_ERROR, event);
	};
	ws.socket.onmessage = function(event) {
		var packet = JSON.parse(event.data);
		Jocket.logger.debug("Packet received: sid=%s, transport=websocket, packet=%o", jocket.sessionId, packet);
		if (packet.type == Jocket.PACKET_TYPE_PONG) {
			Jocket.util.clearPingTimeout(ws);		
			if (ws == jocket._probing) {
				jocket._upgrade();
			}
			ws._timers.interval = setTimeout(function() {
				ws.ping();
			}, jocket._pingInterval);
		}
		else if (packet.type == Jocket.PACKET_TYPE_MESSAGE) {
			jocket._fire(Jocket.EVENT_MESSAGE, packet.name, JSON.parse(packet.data || "null"));
		}
		else {
			Jocket.logger.warn("Unsupport message type for WebSocket: %s", packet.type);
		}
	};
};

Jocket.Ws.prototype.destroy = function(code, message)
{
	Jocket.util.clearTimers(this);
	var socket = this.socket;
	if (socket != null) {
		this.socket = null;
		socket.onopen = socket.onclose = socket.onerror = socket.onmessage = null;
		if (socket.readyState == 0 /*CONNECTING*/ || socket.readyState == 1 /*OPEN*/) {
			socket.close(code, message);
		}
		Jocket.logger.debug("WebSocket closed: code=%d, message=%s", code, message);
	}
};

Jocket.Ws.prototype.ping = function(packet)
{
	var jocket = this.jocket;
	delete this._timers.interval;
	this.sendPacket({type:Jocket.PACKET_TYPE_PING});
	this._timers.timeout = setTimeout(function() {
		jocket._closeTransport(ws, Jocket.CLOSE_PING_TIMEOUT, "ping timeout");
	}, jocket._pingTimeout);
};

Jocket.Ws.prototype.sendPacket = function(packet)
{
	var jocket = this.jocket;
	Jocket.logger.debug("Packet send: sid=%s, transport=websocket, packet=%o", jocket.sessionId, packet);
	this.socket.send(JSON.stringify(packet));
};

//-----------------------------------------------------------------------
// Jocket.Polling
//-----------------------------------------------------------------------

Jocket.Polling = function(jocket)
{
	this.jocket = jocket;
	this.url = jocket._getPollingUrl();
	this._timers = {};
};

Jocket.Polling.prototype.start = function()
{
	this.active = true;
	this.poll();
	this.ping();
};

Jocket.Polling.prototype.destroy = function(code, message)
{
	var jocket = this.jocket;
	Jocket.util.clearTimers(this);
	if (this.ajax != null) {
		this.ajax.polling = null;
		this.ajax.abort();
		this.ajax = null;
	}
	if (this.active) {
		this.active = false;
		var reason = {code:code, message:message};
		var packet = {type:Jocket.PACKET_TYPE_CLOSE, data:JSON.stringify(reason)};
		new Jocket.Ajax(this.url).submit(packet);
	}
};

Jocket.Polling.prototype.stop = function()
{
	var polling = this;
	polling.active = false;
	Jocket.util.clearTimers(polling);
	polling._timers.destroy = setTimeout(function() {
		polling.destroy();
	}, polling.jocket._pingTimeout);
};

Jocket.Polling.prototype.poll = function()
{
	var polling = this;
	var jocket = polling.jocket;
	var ajax = new Jocket.Ajax(polling.url);
	ajax.onsuccess = function(packet) {
		Jocket.logger.debug("Packet received: sid=%s, transport=polling, packet=%o", jocket.sessionId, packet);
		if (polling.active) {
			if (packet.type == Jocket.PACKET_TYPE_CLOSE) {
				polling.active = false;
				jocket._close(Jocket.EVENT_CLOSE, JSON.parse(packet.data));
				return;
			}
			if (packet.type == Jocket.PACKET_TYPE_PONG) {
				Jocket.util.clearPingTimeout(polling);		
				if (polling == jocket._probing) {
					Jocket.logger.debug("Session opened: sid=%s", jocket.sessionId);
					jocket._probing = null;
					jocket._reconnectCount = 0;
					jocket._transport = polling;
					jocket.status = Jocket.STATUS_OPEN;
					jocket._fire(Jocket.EVENT_OPEN);
				}
			}
			polling.poll();
			if (packet.type == Jocket.PACKET_TYPE_PONG) {
				if (jocket._needUpgrade) {
					jocket._needUpgrade = false;
					jocket._probing = new Jocket.Ws(jocket);
					jocket._probing.start();
				}
				polling._timers.interval = setTimeout(function() {
					polling.ping();
				}, jocket._pingInterval);
			}
		}
		if (packet.type == Jocket.PACKET_TYPE_MESSAGE) {
			jocket._fire(Jocket.EVENT_MESSAGE, packet.name, JSON.parse(packet.data || "null"));
		}
	};
	ajax.onfailure = function(event, status) {
		jocket._close(Jocket.CLOSE_POLLING_FAILED, "polling failed");
	};
	ajax.submit();
};

Jocket.Polling.prototype.sendPacket = function(packet)
{
	var jocket = this.jocket;
	Jocket.logger.debug("Packet send: sid=%s, transport=polling, packet=%o", jocket.sessionId, packet);
	var ajax = new Jocket.Ajax(this.url);
	ajax.onfailure = function(event, status) {
		Jocket.logger.error("Packet send failed: sid=%s, event=%o, status=%d", jocket.sessionId, event, status);
		jocket._fire(Jocket.EVENT_ERROR, event);
	};
	ajax.submit(packet);
};

Jocket.Polling.prototype.ping = function()
{
	var polling = this;
	var jocket = polling.jocket;
	delete polling._timers.interval;
	polling.sendPacket({type:Jocket.PACKET_TYPE_PING});
	polling._timers.timeout = setTimeout(function() {
		jocket._closeTransport(polling, Jocket.CLOSE_PING_TIMEOUT, "ping timeout");
	}, jocket._pingTimeout);
};

//-----------------------------------------------------------------------
// Jocket.Ajax
//-----------------------------------------------------------------------

Jocket.Ajax = function(url)
{
	this.url = url;
	this.onsuccess = null;
	this.onfailure = null;
	this._status = Jocket.Ajax.STATUS_NEW;
};

Jocket.Ajax.STATUS_NEW     = 0;
Jocket.Ajax.STATUS_SENDING = 1;
Jocket.Ajax.STATUS_LOAD    = 2;
Jocket.Ajax.STATUS_ERROR   = 3;
Jocket.Ajax.STATUS_ABORT   = 4;
Jocket.Ajax.STATUS_TIMEOUT = 5;

Jocket.Ajax.doLoad = function(event)
{
	event.target._ajax._finish(event, Jocket.Ajax.STATUS_LOAD);
};

Jocket.Ajax.doError = function(event)
{
	event.target._ajax._finish(event, Jocket.Ajax.STATUS_ERROR);
};

Jocket.Ajax.doAbort = function(event)
{
	event.target._ajax._finish(event, Jocket.Ajax.STATUS_ABORT);
};

Jocket.Ajax.doTimeout = function(event)
{
	event.target._ajax._finish(event, Jocket.Ajax.STATUS_TIMEOUT);
};

Jocket.Ajax.doLoadEnd = function(event)
{
	event.target._ajax._cleanup();
};

Jocket.Ajax._parseResponse = function(xhr, status)
{
	var ajax = xhr._ajax; 
	var result = {success:false, httpStatus:xhr.status};
	if (xhr.status == 200) {
		var text = xhr.responseText;
		try {
			result.json = JSON.parse(text);
			result.success = true;
		}
		catch (e) {
			Jocket.logger.error("Invalid JSON format. url=%s, response=%s", ajax.url, text);
		}
	}
	else if (status != Jocket.Ajax.STATUS_ABORT) {
		Jocket.logger.error("AJAX failed. url=%s, status=%d, httpstatus=%d", ajax.url, status, xhr.status);
	}
	return result;
};

Jocket.Ajax.prototype = 
{
	submit: function(data)
	{
		var ajax = this;
		var xhr = new XMLHttpRequest();
		ajax._xhr = xhr;
		xhr._ajax = ajax;
		xhr.onload = Jocket.Ajax.doLoad;
		xhr.onerror = Jocket.Ajax.doError;
		xhr.onabort = Jocket.Ajax.doAbort;
		xhr.ontimeout = Jocket.Ajax.doTimeout;
		var url = ajax.url;
		var timeParamName = ajax.timeParamName || "t";
		url += (url.indexOf("?") == -1 ? "?" : "&") + timeParamName + "=" + Jocket.zipTime.now();
		xhr.open(data == null ? "GET" : "POST", url, true);
		xhr.setRequestHeader("Cache-Control", "no-store, no-cache");
		xhr.setRequestHeader("Jocket-Client", "Web");
		if (ajax.timeout != null) {
			xhr.timeout = ajax.timeout;
		}
		if (data == null) {
			xhr.send();
		}
		else {
			xhr.send(JSON.stringify(data));
		}
		ajax._status = Jocket.Ajax.STATUS_SENDING;
	},

	abort: function()
	{
		this.onfailure = null;
		this._xhr.abort();
	},
	
	isLoad: function()
	{
		return this._status == Jocket.Ajax.STATUS_LOAD;
	},

	isError: function()
	{
		return this._status == Jocket.Ajax.STATUS_ERROR;
	},

	isAbort: function()
	{
		return this._status == Jocket.Ajax.STATUS_ABORT;
	},

	isTimeout: function()
	{
		return this._status == Jocket.Ajax.STATUS_TIMEOUT;
	},
	
	_finish: function(event, status)
	{
		var ajax = this;
		var xhr = ajax._xhr;
		ajax._status = status;
		ajax.result = Jocket.Ajax._parseResponse(xhr, status);
		if (ajax.result != null && ajax.result.success) {
			if (ajax.onsuccess != null) {
				ajax.onsuccess(ajax.result.json);
			}
		}
		else {
			if (ajax.onfailure != null) {
				ajax.onfailure(event, status);
			}
		}
	},
	
	_cleanup: function()
	{
		var ajax = this;
		var xhr = ajax._xhr;
		xhr._ajax = null;
		ajax._xhr = null;
	}
};

//-----------------------------------------------------------------------
// Utility
//-----------------------------------------------------------------------

Jocket.util = 
{
	getProperty: function(key)
	{
		for (var i = 1; i < arguments.length - 1; ++i) {
			var map = arguments[i];
			if (key in map) {
				return map[key];
			}
		}
		return arguments[arguments.length - 1];
	},

	clearTimers: function(transport)
	{
		for (var key in transport._timers) {
			clearTimeout(transport._timers[key]);
		}
		transport._timers = {};
	},
	
	clearPingTimeout: function(transport)
	{
		if (transport._timers.timeout != null) {
			clearTimeout(transport._timers.timeout);
			delete transport._timers.timeout;
		}
	}
};

Jocket.logger = 
{
	debug: function()
	{
		if (Jocket.isDebug) {
			arguments[0] = Jocket.logger._getPrefix() + arguments[0];
			console.log.apply(console, arguments);
		}
	},
	
	warn: function()
	{
		arguments[0] = Jocket.logger._getPrefix() + arguments[0];
		console.warn.apply(console, arguments);
	},

	error: function()
	{
		arguments[0] = Jocket.logger._getPrefix() + arguments[0];
		console.error.apply(console, arguments);
	},
	
	_getPrefix: function()
	{
		var now = new Date();
		var h = now.getHours();
		var m = now.getMinutes();
		var s = now.getSeconds();
		var ms = now.getMilliseconds();
		return "[" + (h < 10 ? "0" : "") + h
			+ ":" + (m < 10 ? "0" : "") + m
			+ ":" + (s < 10 ? "0" : "") + s
			+ "." + (ms < 10 ? "00" : ms < 100 ? "0" : "") + ms
			+ " Jocket] ";
	}
};

Jocket.zipTime =
{
	digits: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".split(""),

	serial: 0,
	
	previous: 0,
	
	now: function()
	{
		var time = "";
		var millis = new Date().getTime();
		while (millis != 0) {
			time = Jocket.zipTime.digits[millis & 0x3F] + time;
			millis >>>= 6;
		}
		if (time == Jocket.zipTime.previous) {
			return time + "." + Jocket.zipTime.serial++;
		}
		Jocket.zipTime.previous = time;
		Jocket.zipTime.serial = 0;
		return time;
	}
};

//-----------------------------------------------------------------------
// Initialization
//-----------------------------------------------------------------------

(function(){
	var scripts = document.getElementsByTagName("script");
	var script = scripts[scripts.length - 1];
	Jocket.isDebug = /debug=true/.test(script.src);
	Jocket.logger.debug("Jocket library loaded: debug=%s", Jocket.isDebug);
})();
