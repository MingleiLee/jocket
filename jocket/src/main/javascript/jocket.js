//-----------------------------------------------------------------------
// Jocket
//-----------------------------------------------------------------------

var Jocket = function(url, options)
{
	var jocket = this;
	if (url == null || url == "") {
		throw new Error("The URL should not be empty");
	}
	if (/^http(s?):/.test(url)) {
		jocket.url = url;
	}
	else {
		jocket.url = location.href.replace(/[\\?#].*/, "").replace(/[^\/]+$/, "") + url.replace(/^\/+/, "");
	}
	jocket.options = options || {};
	jocket._transport = null;
	jocket._listeners = {};
	jocket._isFirstConnection = true;
	if (jocket.options.autoOpen != false) {
		jocket.open();
	}
};

Jocket.prototype.send = function(name, data)
{
	var jocket = this;
	var packet = {type:Jocket.PACKET_TYPE_MESSAGE};
	if (name != null) {
		packet.name = name;
	}
	if (data != null) {
		packet.data = JSON.stringify(data);
	}
	return jocket.sendPacket(packet);
};

Jocket.prototype.sendPacket = function(packet)
{
	var jocket = this;
	if (jocket._transport == null) {
		logger.error("[Jocket] Packet ignored: sid=%s, packet=%o", jocket.sessionId, packet);
		return false;
	}
	jocket._transport.sendPacket(packet);
	Jocket._log("[Jocket] Packet sent: sid=%s, transport=%s, packet=%o", jocket.sessionId, jocket._transportName, packet);
	return true;
};

Jocket.prototype.open = function()
{
	var jocket = this;
	var ajax = new Jocket.Ajax();
	ajax.url = jocket.url.replace(/\?.*/, "") + ".jocket_prepare" + jocket.url.replace(/[^\?]+/, "");
	ajax.onsuccess = Jocket.doPrepareSuccess;
	ajax.onfailure = Jocket.doPrepareFailure;
	ajax.jocket = jocket;
	ajax.submit({transports:jocket.options.transports});
};

Jocket.prototype.close = function()
{
	var jocket = this;
	if (jocket._transport != null) {
		jocket._transport.close(Jocket.CLOSE_NORMAL);
		jocket._transport = null;
	}
	jocket._fire(Jocket.EVENT_CLOSE, {code:Jocket.CLOSE_NORMAL});
	jocket.sessionId = null;
};

Jocket.prototype.isOpen = function()
{
	return this._transport != null;
};

Jocket.prototype.on = Jocket.prototype.addEventListener = function(name, listener)
{
	var jocket = this;
	jocket._listeners[name] = jocket._listeners[name] || [];
	jocket._listeners[name].push(listener);
	return jocket;
};

Jocket.prototype._processDownstreamPacket = function(packet)
{
	var jocket = this;
	if (packet.type == Jocket.PACKET_TYPE_PONG) {
		if (jocket._transport != null) {
			var handshakeTimer = jocket._transport.timers.handshake;
			if (handshakeTimer != null) {
				Jocket._log("[Jocket] Session opened: sid=%s, transport=%s", jocket.sessionId, jocket._transportName);
				clearTimeout(handshakeTimer);
				delete jocket._transport.timers.handshake;
				jocket.sendPacket({type:Jocket.PACKET_TYPE_OPEN});
				jocket._fire(Jocket.EVENT_OPEN);
			}
			else {
				//TODO
			}
		}
	}
	else if (packet.type == Jocket.PACKET_TYPE_CLOSE) {
		if (jocket._transport != null) {
			jocket._transport.destroy();
		}
	}
	else if (packet.type == null || packet.type == Jocket.PACKET_TYPE_MESSAGE) {
		var data = packet.data == null ? null : JSON.parse(packet.data);
		jocket._fire(Jocket.PACKET_TYPE_MESSAGE, data, packet.name);
	}
};

Jocket.prototype._fire = function(type)
{
	var jocket = this;
	var listeners = jocket._listeners[type];
	if (type in jocket._listeners) {
		var listeners = jocket._listeners[type].slice(0);
		var args = Array.prototype.slice.call(arguments, 1);
		for (var i = 0; i < listeners.length; ++i) {
			listeners[i].apply(jocket, args);
		}
	}
};

Jocket.prototype._tryNextTransport = function()
{
	var jocket = this;
	++jocket._transportTryIndex;
	if (jocket._transportTryIndex >= jocket._transports.length) {
		console.error("[Jocket] All transports failed.");
		var ajax = new Jocket.Ajax(Jocket.Polling.getUrl(jocket));
		ajax.submit({type:Jocket.PACKET_TYPE_CLOSE});
		if (jocket._isFirstConnection || !jocket.options.autoReconnect) {
			var data = {code:Jocket.CLOSE_CONNECT_FAILED, message:"Failed to make Jocket connection."};
			jocket._fire(Jocket.EVENT_CLOSE, data);
		}
		else if (jocket.options.autoReconnect) {
			jocket._fire(Jocket.EVENT_PAUSE);
			//TODO setTimeout
		}
		jocket._isFirstConnection = false;
		return;
	}
	jocket._transportName = jocket._transports[jocket._transportTryIndex];
	var transportClass = Jocket._transportClasses[jocket._transportName];
	var transport = new transportClass(jocket);
	Jocket._log("[Jocket] Trying transport: sid=%s, transport=%s", jocket.sessionId, jocket._transportName);
	transport.timers.handshake = setTimeout(function() {
		transport.destroy();
		jocket._tryNextTransport();
	}, jocket._handshakeTimeout);
};

Jocket.prototype._sendPing = function()
{
	jocket.sendPacket({type:Jocket.PACKET_TYPE_PING});
};

Jocket.doPrepareSuccess = function(response)
{
	Jocket._log("[Jocket] Session prepared: sid=%s", response.sessionId);
	var jocket = this.jocket;
	jocket.sessionId = response.sessionId;
	jocket._transports = response.transports;
	jocket._handshakeTimeout = jocket.options.handshakeTimeout || response.handshakeTimeout || 3000;
	jocket._transportTryIndex = -1;
	jocket._tryNextTransport();
};

Jocket.doPrepareFailure = function(event, status)
{
	var ajax = this;
	var jocket = ajax.jocket;
	var data = {code:Jocket.CLOSE_INIT_FAILED, message:"Jocket initialization failed"};
	console.error("[Jocket] Jocket prepare failed. status=%d, result=%o", status, ajax.result);
	jocket._fire(Jocket.EVENT_CLOSE, data);
};

Jocket._log = function()
{
	if (Jocket.isDebug) {
		console.log.apply(console, arguments);
	}
};

Jocket._transportClasses = {};

Jocket.PACKET_TYPE_OPEN    = "open";
Jocket.PACKET_TYPE_CLOSE   = "close";
Jocket.PACKET_TYPE_PING    = "ping";
Jocket.PACKET_TYPE_PONG    = "pong";
Jocket.PACKET_TYPE_NOOP    = "noop";
Jocket.PACKET_TYPE_MESSAGE = "message";

Jocket.CLOSE_NORMAL         = 1000; //normal closure
Jocket.CLOSE_AWAY           = 1001; //close browser or reload page
Jocket.CLOSE_ABNORMAL       = 1006; //network error
Jocket.CLOSE_NEED_INIT      = 3600; //no jocket_sid passed to server
Jocket.CLOSE_NO_SESSION     = 3601; //the Jocket session specified by jocket_sid is not found
Jocket.CLOSE_INIT_FAILED    = 3602; //failed to open *.jocket_prepare
Jocket.CLOSE_CONNECT_FAILED = 3603; //all available transports failed

Jocket.EVENT_OPEN   = "open";
Jocket.EVENT_CLOSE  = "close";
Jocket.EVENT_PAUSE  = "pause";
Jocket.EVENT_RESUME = "resume";
Jocket.EVENT_ERROR  = "error";

//-----------------------------------------------------------------------
// Jocket.Ws
//-----------------------------------------------------------------------

Jocket.Ws = Jocket._transportClasses["websocket"] = function(jocket)
{
	var ws = this;
	ws.jocket = jocket;
	jocket._transport = ws;
	ws.url = jocket.url.replace(/^http/, "ws").replace(/\?.*/, "") + "?jocket_sid=" + jocket.sessionId;
	ws.timers = {};
	ws.open();
};

Jocket.Ws.prototype.open = function()
{
	var ws = this;
	var jocket = ws.jocket;
	ws.webSocket = new WebSocket(ws.url);
	ws.webSocket.ws = ws;
	ws.webSocket.onopen = Jocket.Ws.doWebSocketOpen;
	ws.webSocket.onclose = Jocket.Ws.doWebSocketClose;
	ws.webSocket.onerror = Jocket.Ws.doWebSocketError;
	ws.webSocket.onmessage = Jocket.Ws.doWebSocketMessage;
};

Jocket.Ws.prototype.close = function(code, message)
{
	this.destroy(code, message);
};

Jocket.Ws.prototype.destroy = function(code, message)
{
	var ws = this;
	for (var key in ws.timers) {
		clearTimeout(ws.timers[key]);
	}
	ws.timers = {};
	var webSocket = ws.webSocket;
	if (webSocket != null) {
		ws.webSocket = null;
		webSocket.ws = null;
		webSocket.onopen = null;
		webSocket.onclose = null;
		webSocket.onerror = null;
		webSocket.onmessage = null;
		webSocket.close(code, message);
	}
};

Jocket.Ws.prototype.sendPacket = function(packet)
{
	this.webSocket.send(JSON.stringify(packet));
};

Jocket.Ws.doWebSocketOpen = function()
{
	this.ws.jocket._sendPing();
};

Jocket.Ws.doWebSocketClose = function(event)
{
	Jocket._log("[Jocket] WebSocket closed: event=%o", event);
	var ws = this.ws;
	var jocket = ws.jocket;
	ws.destroy();
	if (ws.timers.handshake != null) {
		jocket._tryNextTransport();
		return;
	}
	var packet = {type:Jocket.EVENT_CLOSE, data:JSON.stringify({code:event.code, message:event.reason})};
	jocket._processDownstreamPacket(packet);
};

Jocket.Ws.doWebSocketError = function(event)
{
	var jocket = this.ws.jocket;
	console.error("[Jocket] WebSocket error", event);
	jocket._fire(Jocket.EVENT_ERROR, event);
};

Jocket.Ws.doWebSocketMessage = function(event)
{
	var ws = this.ws;
	var packet = JSON.parse(event.data);
	var sessionId = ws.jocket && ws.jocket.sessionId;
	Jocket._log("[Jocket] Packet received: sid=%s, transport=websocket, packet=%o", sessionId, packet);
	ws.jocket._processDownstreamPacket(packet);
};

//-----------------------------------------------------------------------
// Jocket.Polling
//-----------------------------------------------------------------------

Jocket.Polling = Jocket._transportClasses["polling"] = function(jocket)
{
	var polling = this;
	polling.jocket = jocket;
	jocket._transport = polling;
	polling.url = Jocket.Polling.getUrl(jocket);
	polling.timers = {};
	polling.poll();
	jocket._sendPing();
};

Jocket.Polling.prototype.close = function(code, message)
{
	var polling = this;
	polling.destroy();
	var reason = {code:code, message:message};
	var packet = {type:Jocket.PACKET_TYPE_CLOSE, data:JSON.stringify(reason)};
	new Jocket.Ajax(polling.url).submit(packet);
};

Jocket.Polling.prototype.destroy = function()
{
	var polling = this;
	var jocket = polling.jocket;
	for (var key in polling.timers) {
		clearTimeout(polling.timers[key]);
	}
	polling.timers = {};
	if (polling.ajax != null) {
		polling.ajax.polling = null;
		polling.ajax.abort();
		polling.ajax = null;
	}
};

Jocket.Polling.prototype.poll = function()
{
	var polling = this;
	var ajax = new Jocket.Ajax(polling.url);
	polling.ajax = ajax;
	ajax.timeout = 35000; //TODO
	ajax.onsuccess = Jocket.Polling.doSuccess;
	ajax.onfailure = Jocket.Polling.doFailure;
	ajax.polling = polling;
	ajax.submit();
};

Jocket.Polling.prototype.sendPacket = function(packet)
{
	var polling = this;
	var ajax = new Jocket.Ajax(polling.url);
	ajax.polling = polling;
	ajax.onfailure = Jocket.Polling.doSendPacketFailure;
	ajax.submit(packet);
};

Jocket.Polling.getUrl = function(jocket)
{
	return jocket.url.replace(/\?.*/, "") + ".jocket_polling?jocket_sid=" + jocket.sessionId;	
};

Jocket.Polling.doSuccess = function(packet)
{
	Jocket._log("[Jocket] Packet received: transport=polling, data=%o", packet);
	var polling = this.polling;
	var jocket = polling.jocket;
	polling.ajax = null;
	if (packet.type == Jocket.PACKET_TYPE_CLOSE) {
		jocket._processDownstreamPacket(packet);
		return;
	}
	polling.poll();
	jocket._processDownstreamPacket(packet);
};

Jocket.Polling.doFailure = function(event, status)
{
	var polling = this.polling;
	var jocket = polling.jocket;
	polling.destroy();
	jocket._fire(Jocket.EVENT_CLOSE, {code:Jocket.CLOSE_ABNORMAL}); //TODO retry
};

Jocket.Polling.doSendPacketFailure = function(event, status)
{
	var polling = this.polling;
	var jocket = polling.jocket;
	console.error("[Jocket] Packet send failed: sid=%s, event=%o, status=%d", jocket.sessionId, event, status);
	jocket._fire(Jocket.EVENT_ERROR, event);
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

Jocket.Ajax._parseXmlHttpRequestResponse = function(xhr, status)
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
			console.error("[Jocket] Invalid JSON format. url=%s, response=%s", ajax.url, text);
		}
	}
	else if (status != Jocket.Ajax.STATUS_ABORT) {
		console.error("[Jocket] AJAX failed. url=%s, status=%d, http status=%d", ajax.url, status, xhr.status);
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
		if (ajax.cache != true) {
			url += (url.indexOf("?") == -1 ? "?" : "&") + "jocket_rnd=" + new Date().getTime() + Math.random();
		}
		xhr.open(data == null ? "GET" : "POST", url, true);
		xhr.setRequestHeader("Cache-Control", "no-store, no-cache");
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
		ajax.result = Jocket.Ajax._parseXmlHttpRequestResponse(xhr, status);
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
// Initialization
//-----------------------------------------------------------------------

(function(){
	var scripts = document.getElementsByTagName("script");
	var script = scripts[scripts.length - 1];
	Jocket.isDebug = /debug=true/.test(script.src);
	Jocket._log("[Jocket] Jocket library loaded: debug=%s", Jocket.isDebug);
})();

