//-----------------------------------------------------------------------
// jio
//-----------------------------------------------------------------------

var jio = 
{
	//packet types
	PACKET_TYPE_CLOSE	: 1,
	PACKET_TYPE_TIMEOUT	: 2,
	
	//close reason codes
	CLOSE_NORMAL	: 1000,
	CLOSE_AWAY		: 1001,	//close browser or reload page
	CLOSE_ABNORMAL	: 1006,	//network error
	
	//pre-defined events
	EVENT_OPEN	: "open",
	EVENT_CLOSE	: "close",
	
	connect: function(url, options)
	{
		return new jio.Socket(url, options);
	}
};

//-----------------------------------------------------------------------
// jio.Socket
//-----------------------------------------------------------------------

jio.Socket = function(url, options)
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
		if (jio.Socket.impls[t] == null) {
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

jio.Socket.prototype.emit = function(eventName, data)
{
	var socket = this;
	if (socket._transport == null) {
		return false;
	}
	socket._transport.emit({name:eventName, data:JSON.stringify(data)});
	return true;
};

jio.Socket.prototype.open = function()
{
	var socket = this;
	var ajax = new jio.Ajax();
	ajax.url = socket.url.replace(/\?.*/, "") + ".jocket_prepare" + socket.url.replace(/[^\?]+/, "");
	ajax.onsuccess = jio.Socket.doPrepareSuccess;
	ajax.onfailure = jio.Socket.doPrepareFailure;
	ajax.socket = socket;
	ajax.submit();
};

jio.Socket.prototype.close = function(eventName, data)
{
	var socket = this;
	if (socket._transport != null) {
		socket._transport.close();
		socket._transport = null;
	}
};

jio.Socket.prototype.isOpen = function()
{
	return this._transport != null;
};

jio.Socket.prototype.on = function(eventName, listener)
{
	var socket = this;
	socket._listeners[eventName] = socket._listeners[eventName] || [];
	socket._listeners[eventName].push(listener);
	return socket;
};

jio.Socket.prototype._fire = function(eventName, eventData)
{
	var socket = this;
	var listeners = socket._listeners[eventName];
	if (eventName in socket._listeners) {
		var listeners = socket._listeners[eventName].slice(0);
		for (var i = 0; i < listeners.length; ++i) {
			listeners[i].call(socket, eventData);
		}
	}
};

jio.Socket.doPrepareSuccess = function(response)
{
	var socket = this.socket;
	socket.connectionId = response.connectionId;
	socket._transport = new jio.Socket.impls[socket._transports[0]](socket); //TODO try next if failed
};

jio.Socket.doPrepareFailure = function(response)
{
	//TODO
	console.error("[Jocket] Jocket prepare failed");
};

jio.Socket.impls = {};

//-----------------------------------------------------------------------
// jio.WebSocket
//-----------------------------------------------------------------------

jio.Ws = jio.Socket.impls["websocket"] = function(socket)
{
	this.socket = socket;
	this.url = socket.url.replace(/^http/, "ws").replace(/\?.*/, "") + "?jocket_cid=" + socket.connectionId;
	this.open();
};

jio.Ws.prototype.open = function()
{
	var ws = this;
	ws.webSocket = new WebSocket(ws.url);
	ws.webSocket.ws = ws;
	ws.webSocket.onopen	= jio.Ws.doWebSocketOpen;
	ws.webSocket.onclose = jio.Ws.doWebSocketClose;
	ws.webSocket.onerror = jio.Ws.doWebSocketError;
	ws.webSocket.onmessage = jio.Ws.doWebSocketMessage;
};

jio.Ws.prototype.close = function(eventName, data)
{
	this.webSocket.close();
};

jio.Ws.prototype.emit = function(message)
{
	this.webSocket.send(JSON.stringify(message));
};

jio.Ws.doWebSocketOpen = function()
{
	var ws = this.ws;
	ws.socket._fire(jio.EVENT_OPEN);
};

jio.Ws.doWebSocketClose = function(event)
{
	var ws = this.ws;
	ws.socket._transport = null;
	ws.socket._fire(jio.EVENT_CLOSE, {code:event.code, description:event.reason});
	ws.socket = null;
	ws.webSocket.ws = null;
	ws.webSocket = null;
};

jio.Ws.doWebSocketError = function(event)
{
	//TODO
	console.log("[Jocket] WebSocket error", event);
};

jio.Ws.doWebSocketMessage = function(event)
{
	var ws = this.ws;
	var packet = JSON.parse(event.data);
	console.log("[Jocket] Packet received: transport=websocket, packet=%o", packet);
	ws.socket._fire(packet.name, JSON.parse(packet.data));
};

//-----------------------------------------------------------------------
// jio.Polling
//-----------------------------------------------------------------------

jio.Polling = jio.Socket.impls["polling"] = function(socket)
{
	this.socket = socket;
	this.url = socket.url.replace(/\?.*/, "") + ".jocket_polling?jocket_cid=" + socket.connectionId;
	this.poll();
	socket._fire(jio.EVENT_OPEN);
};

jio.Polling.prototype.poll = function()
{
	var polling = this;
	var ajax = new jio.Ajax(polling.url);
	polling.ajax = ajax;
	ajax.timeout = 35000;
	ajax.onsuccess = jio.Polling.doSuccess;
	ajax.onfailure = jio.Polling.doFailure;
	ajax.polling = polling;
	ajax.submit();
};

jio.Polling.prototype.close = function()
{
	var polling = this;
	var socket = polling.socket;
	if (polling.ajax != null) {
		polling.ajax.abort();
		polling.ajax = null;
	}
	var ajax = new jio.Ajax(polling.url);
	ajax.submit({type:jio.PACKET_TYPE_CLOSE});
	socket._fire(jio.EVENT_CLOSE, {code:jio.CLOSE_NORMAL});
};

jio.Polling.prototype.emit = function(message)
{
	var polling = this;
	var ajax = new jio.Ajax(polling.url);
	ajax.onsuccess = jio.Polling.doEmitSuccess;
	ajax.onfailure = jio.Polling.doEmitFailure;
	ajax.submit(message);
};

jio.Polling.doSuccess = function(packet)
{
	console.log("[Jocket] Packet received: transport=polling, data=%o", packet);
	var polling = this.polling;
	var socket = polling.socket;
	polling.ajax = null;
	if (packet.type == jio.PACKET_TYPE_CLOSE) {
		socket._transport = null;
		socket._fire(jio.EVENT_CLOSE, JSON.parse(packet.data));
		return;
	}
	polling.poll();
	if (packet.type != jio.PACKET_TYPE_TIMEOUT) {
		socket._fire(packet.name, JSON.parse(packet.data));
	}
};

jio.Polling.doFailure = function(event, status)
{
	var socket = this.polling.socket;
	socket._transport = null; //TODO clear other content
	socket._fire(jio.EVENT_CLOSE, {code:jio.CLOSE_ABNORMAL});
};

jio.Polling.doEmitSuccess = function(response)
{
};

jio.Polling.doEmitFailure = function(response)
{
	//TODO
	console.error("[Jocket] Message emit failed");
};

//-----------------------------------------------------------------------
// jio.Ajax
//-----------------------------------------------------------------------

jio.Ajax = function(url)
{
	this.url		= url;
	this.onsuccess	= null;
	this.onfailure	= null;
	this._status	= jio.Ajax.STATUS_NEW;
};

jio.Ajax.STATUS_NEW		= 0;
jio.Ajax.STATUS_SENDING = 1;
jio.Ajax.STATUS_LOAD	= 2;
jio.Ajax.STATUS_ERROR 	= 3;
jio.Ajax.STATUS_ABORT 	= 4;
jio.Ajax.STATUS_TIMEOUT	= 5;

jio.Ajax.doLoad = function(event)
{
	event.target._ajax._finish(event, jio.Ajax.STATUS_LOAD);
};

jio.Ajax.doError = function(event)
{
	event.target._ajax._finish(event, jio.Ajax.STATUS_ERROR);
};

jio.Ajax.doAbort = function(event)
{
	event.target._ajax._finish(event, jio.Ajax.STATUS_ABORT);
};

jio.Ajax.doTimeout = function(event)
{
	event.target._ajax._finish(event, jio.Ajax.STATUS_TIMEOUT);
};

jio.Ajax.doLoadEnd = function(event)
{
	event.target._ajax._cleanup();
};

jio.Ajax._parseXmlHttpRequestResponse = function(xhr, status)
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
	else if (status != jio.Ajax.STATUS_ABORT) {
		console.error("[Jocket] AJAX failed. url=%s, status=%d, http status=%d", ajax.url, status, xhr.status);
	}
	return result;
};

jio.Ajax.prototype = 
{
	submit: function(data)
	{
		var ajax 		= this;
		var xhr 		= new XMLHttpRequest();
		ajax._xhr		= xhr;
		xhr._ajax		= ajax;
		xhr.onload 		= jio.Ajax.doLoad;
		xhr.onerror 	= jio.Ajax.doError;
		xhr.onabort 	= jio.Ajax.doAbort;
		xhr.ontimeout	= jio.Ajax.doTimeout;
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
		ajax._status = jio.Ajax.STATUS_SENDING;
	},

	abort: function()
	{
		this.onfailure = null;
		this._xhr.abort();
	},
	
	isLoad: function()
	{
		return this._status == jio.Ajax.STATUS_LOAD;
	},

	isError: function()
	{
		return this._status == jio.Ajax.STATUS_ERROR;
	},

	isAbort: function()
	{
		return this._status == jio.Ajax.STATUS_ABORT;
	},

	isTimeout: function()
	{
		return this._status == jio.Ajax.STATUS_TIMEOUT;
	},
	
	_finish: function(event, status)
	{
		var ajax 		= this;
		var xhr 		= ajax._xhr;
		ajax._status	= status;
		ajax.result 	= jio.Ajax._parseXmlHttpRequestResponse(xhr, status);
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

//-----------------------------------------------------------------------
// initialization
//-----------------------------------------------------------------------

if (typeof io == "undefined") {
	io = jio;
}
