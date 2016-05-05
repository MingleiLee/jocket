//-----------------------------------------------------------------------
// Jocket
//-----------------------------------------------------------------------

var Jocket = function(url, options)
{
	if (url == null || url == "") {
		throw "The URL should not be empty";
	}
	if (/^http(s?):/.test(url)) {
		this.url = url;
	}
	else {
		this.url = location.href.replace(/[\\?#].*/, "").replace(/[^\/]+$/, "") + url.replace(/^\/+/, "");
	}
	this.options = options || {};
	this._transports = [];
	for (var i = 0, list = this.options.transports || ["websocket", "polling"]; i < list.length; ++i) {
		var t = list[i];
		if (Jocket._transportClasses[t] == null) {
			throw "Invalid transport: " + t;
		}
		if (t != "websocket" || window.WebSocket != null) {
			this._transports.push(t);
		}
	}
	if (this._transports.length == 0) {
		throw "No available transports";
	}
	this._transport = null;
	this._listeners = {};
	if (this.options.autoOpen != false) {
		this.open();
	}
};

Jocket.prototype.emit = function(eventName, data)
{
	var jocket = this;
	if (jocket._transport == null) {
		return false;
	}
	jocket._transport.emit({name:eventName, data:JSON.stringify(data)});
	return true;
};

Jocket.prototype.send = function(data)
{
	return this.emit("message", data);
};

Jocket.prototype.open = function()
{
	var jocket = this;
	var ajax = new Jocket.Ajax();
	ajax.url = jocket.url.replace(/\?.*/, "") + ".jocket_prepare" + jocket.url.replace(/[^\?]+/, "");
	ajax.onsuccess = Jocket.doPrepareSuccess;
	ajax.onfailure = Jocket.doPrepareFailure;
	ajax.jocket = jocket;
	ajax.submit();
};

Jocket.prototype.close = function(eventName, data)
{
	var jocket = this;
	if (jocket._transport != null) {
		jocket._transport.close();
		jocket._transport = null;
	}
};

Jocket.prototype.isOpen = function()
{
	return this._transport != null;
};

Jocket.prototype.on = function(eventName, listener)
{
	var jocket = this;
	jocket._listeners[eventName] = jocket._listeners[eventName] || [];
	jocket._listeners[eventName].push(listener);
	return jocket;
};

Jocket.prototype._fire = function(eventName, eventData)
{
	var jocket = this;
	var listeners = jocket._listeners[eventName];
	if (eventName in jocket._listeners) {
		var listeners = jocket._listeners[eventName].slice(0);
		for (var i = 0; i < listeners.length; ++i) {
			listeners[i].call(jocket, eventData);
		}
	}
};

Jocket.doPrepareSuccess = function(response)
{
	var jocket = this.jocket;
	var transportClass = Jocket._transportClasses[jocket._transports[0]];
	jocket.sessionId = response.sessionId;
	jocket._transport = new transportClass(jocket); //TODO try next if failed
};

Jocket.doPrepareFailure = function(response)
{
	//TODO
	console.error("[Jocket] Jocket prepare failed");
};

Jocket._transportClasses = {};

Jocket.PACKET_TYPE_CLOSE	= 1,
Jocket.PACKET_TYPE_TIMEOUT	= 2,

Jocket.CLOSE_NORMAL			= 1000,
Jocket.CLOSE_AWAY			= 1001,	//close browser or reload page
Jocket.CLOSE_ABNORMAL		= 1006,	//network error

Jocket.EVENT_OPEN			= "open",
Jocket.EVENT_CLOSE			= "close",

//-----------------------------------------------------------------------
// Jocket.Ws
//-----------------------------------------------------------------------

Jocket.Ws = Jocket._transportClasses["websocket"] = function(jocket)
{
	this.jocket = jocket;
	this.url = jocket.url.replace(/^http/, "ws").replace(/\?.*/, "") + "?jocket_sid=" + jocket.sessionId;
	this.open();
};

Jocket.Ws.prototype.open = function()
{
	var ws = this;
	ws.webSocket = new WebSocket(ws.url);
	ws.webSocket.ws = ws;
	ws.webSocket.onopen	= Jocket.Ws.doWebSocketOpen;
	ws.webSocket.onclose = Jocket.Ws.doWebSocketClose;
	ws.webSocket.onerror = Jocket.Ws.doWebSocketError;
	ws.webSocket.onmessage = Jocket.Ws.doWebSocketMessage;
};

Jocket.Ws.prototype.close = function(eventName, data)
{
	this.webSocket.close();
};

Jocket.Ws.prototype.emit = function(message)
{
	this.webSocket.send(JSON.stringify(message));
};

Jocket.Ws.doWebSocketOpen = function()
{
	var ws = this.ws;
	ws.jocket._fire(Jocket.EVENT_OPEN);
};

Jocket.Ws.doWebSocketClose = function(event)
{
	var ws = this.ws;
	ws.jocket._transport = null;
	ws.jocket._fire(Jocket.EVENT_CLOSE, {code:event.code, description:event.reason});
	ws.jocket = null;
	ws.webSocket.ws = null;
	ws.webSocket = null;
};

Jocket.Ws.doWebSocketError = function(event)
{
	//TODO
	console.log("[Jocket] WebSocket error", event);
};

Jocket.Ws.doWebSocketMessage = function(event)
{
	var ws = this.ws;
	var packet = JSON.parse(event.data);
	console.log("[Jocket] Packet received: transport=websocket, packet=%o", packet);
	ws.jocket._fire(packet.name, JSON.parse(packet.data));
};

//-----------------------------------------------------------------------
// Jocket.Polling
//-----------------------------------------------------------------------

Jocket.Polling = Jocket._transportClasses["polling"] = function(jocket)
{
	this.jocket = jocket;
	this.url = jocket.url.replace(/\?.*/, "") + ".jocket_polling?jocket_sid=" + jocket.sessionId;
	this.poll();
	jocket._fire(Jocket.EVENT_OPEN);
};

Jocket.Polling.prototype.poll = function()
{
	var polling = this;
	var ajax = new Jocket.Ajax(polling.url);
	polling.ajax = ajax;
	ajax.timeout = 35000;
	ajax.onsuccess = Jocket.Polling.doSuccess;
	ajax.onfailure = Jocket.Polling.doFailure;
	ajax.polling = polling;
	ajax.submit();
};

Jocket.Polling.prototype.close = function()
{
	var polling = this;
	var jocket = polling.jocket;
	if (polling.ajax != null) {
		polling.ajax.abort();
		polling.ajax = null;
	}
	var ajax = new Jocket.Ajax(polling.url);
	ajax.submit({type:Jocket.PACKET_TYPE_CLOSE});
	jocket._fire(Jocket.EVENT_CLOSE, {code:Jocket.CLOSE_NORMAL});
};

Jocket.Polling.prototype.emit = function(message)
{
	var polling = this;
	var ajax = new Jocket.Ajax(polling.url);
	ajax.onsuccess = Jocket.Polling.doEmitSuccess;
	ajax.onfailure = Jocket.Polling.doEmitFailure;
	ajax.submit(message);
};

Jocket.Polling.doSuccess = function(packet)
{
	console.log("[Jocket] Packet received: transport=polling, data=%o", packet);
	var polling = this.polling;
	var jocket = polling.jocket;
	polling.ajax = null;
	if (packet.type == Jocket.PACKET_TYPE_CLOSE) {
		jocket._transport = null;
		jocket._fire(Jocket.EVENT_CLOSE, JSON.parse(packet.data));
		return;
	}
	polling.poll();
	if (packet.type != Jocket.PACKET_TYPE_TIMEOUT) {
		jocket._fire(packet.name, JSON.parse(packet.data));
	}
};

Jocket.Polling.doFailure = function(event, status)
{
	var jocket = this.polling.jocket;
	jocket._transport = null; //TODO clear other content
	jocket._fire(Jocket.EVENT_CLOSE, {code:Jocket.CLOSE_ABNORMAL});
};

Jocket.Polling.doEmitSuccess = function(response)
{
};

Jocket.Polling.doEmitFailure = function(response)
{
	//TODO
	console.error("[Jocket] Message emit failed");
};

//-----------------------------------------------------------------------
// Jocket.Ajax
//-----------------------------------------------------------------------

Jocket.Ajax = function(url)
{
	this.url		= url;
	this.onsuccess	= null;
	this.onfailure	= null;
	this._status	= Jocket.Ajax.STATUS_NEW;
};

Jocket.Ajax.STATUS_NEW		= 0;
Jocket.Ajax.STATUS_SENDING = 1;
Jocket.Ajax.STATUS_LOAD	= 2;
Jocket.Ajax.STATUS_ERROR 	= 3;
Jocket.Ajax.STATUS_ABORT 	= 4;
Jocket.Ajax.STATUS_TIMEOUT	= 5;

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
		var ajax 		= this;
		var xhr 		= new XMLHttpRequest();
		ajax._xhr		= xhr;
		xhr._ajax		= ajax;
		xhr.onload 		= Jocket.Ajax.doLoad;
		xhr.onerror 	= Jocket.Ajax.doError;
		xhr.onabort 	= Jocket.Ajax.doAbort;
		xhr.ontimeout	= Jocket.Ajax.doTimeout;
		var url 		= ajax.url;
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
		var ajax 		= this;
		var xhr 		= ajax._xhr;
		ajax._status	= status;
		ajax.result 	= Jocket.Ajax._parseXmlHttpRequestResponse(xhr, status);
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
		var ajax	= this;
		var xhr 	= ajax._xhr;
		xhr._ajax	= null;
		ajax._xhr	= null;
	}
};
