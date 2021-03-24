/**
 * JavaScript library for Jocket
 *
 * Notice:
 *     1. The methods start with '_' is private, and should not be used by user.
 *     2. In the @params description, [TYPE, DEFAULT] means the parameter's type is TYPE, default value is DEFAULT.
 *        If DEFAULT is 'required', it means the parameter is required.
 */

//-----------------------------------------------------------------------
// Jocket
//-----------------------------------------------------------------------

/**
 * Create a Jocket object.
 *
 * Examples:
 *     var jocket = new Jocket({server:'http://def.com/service', path:'/my/chat'})
 *     var jocket = new Jocket({server:'http://abc.com', path:'/chat', params:{userId:12345}})
 *
 * @param options [object, required] A JSON object contains Jocket options. acceptable attributes:
 *     server	 	: [string, current directory] The server URL where Jocket is deployed. e.g. http://abc.com,
 *                 	https://abc.com/xyz. The default value is current web directory. e.g.
 *                 	-------------------------------------------------------------
 *                    URL of current page              |   server
 *                 	-------------------------------------------------------------
 *                    http://abc.com                   |   http://abc.com
 *                 	-------------------------------------------------------------
 *                    http://abc.com/                  |   http://abc.com
 *                 	-------------------------------------------------------------
 *                    http://abc.com/hello             |   http://abc.com
 *                 	-------------------------------------------------------------
 *                    http://abc.com/hello/            |   http://abc.com/hello
 *                 	-------------------------------------------------------------
 *                    http://abc.com/hello/world.jsp   |   http://abc.com/hello
 *                 	-------------------------------------------------------------
 *     path		    : [string, required] The Jocket path, which is defined by developer. e.g., /hello, /my/chat
 *     params	    : [object, null] The parameters pass to Jocket service. e.g., {user:'tom',age:25}, {chatId:123}
 *     upgrade      : [boolean, true] Upgrade to WebSocket if available. If not, use long-polling only.
 *     autoOpen     : [boolean, true] Auto connect to server when Jocket object is created.
 *     reconnect    : [boolean, true] Auto reconnect when connect is closed due to network or other errors.
 *     sendLog      : [boolean, true] Send client log to server side.
 *     urlVersion	: [integer, 2] The URL version, only for backward compatibility.
 *
 * You can also use the following parameters for backward compatibility. But you should upgrade to the prior way as
 * soon as possible.
 *
 * #param url Jocket URL. acceptable formats:
 *     1. URL starts with http/https. for example: http://www.aaa.com/abc/create.jocket
 *     2. URL starts with slash(/). for example: /abc, /abc/test
 *        In this case, Jocket and the current page should share a same back-end server.
 * #param options Jocket options. available properties:
 *     upgrade  : [boolean, true] Upgrade to WebSocket if available. If not, use long-polling only.
 *     autoOpen : [boolean, true] Auto connect to server when Jocket object is created.
 *     reconnect: [boolean, true] Auto reconnect when connect is closed due to network or other errors.
 */
var Jocket = function(options)
{
    this._logger = new Jocket.Logger();
    this._logger.jocket = this;
    if (typeof options === "object") {
        this.options = options;
        this._urlVersion = options.urlVersion || 2;
        var server = (options.server || Jocket._getServerUrl()).replace(/\/+$/, '');
        if (this._urlVersion === 1) {
            this._createUrl = server + options.path + ".jocket";
        }
        else {
            this._createUrl = server + "/jocket/create"
            				+ "?jocket_version=" + Jocket._version
            				+ "&jocket_path=" + encodeURIComponent(options.path);
        }
        if (options.params != null) {
            var separator = this._urlVersion === 1 ? "?" : "&";
            for (var key in options.params) {
                this._createUrl += separator + key + "=" + encodeURIComponent(options.params[key]);
                separator = "&";
            }
        }
    }
    else { // For backward compatibility. the old way is: new Jocket(url, options)
        this._logger.warn("Please use Jocket by new way: new Jocket({server:'http://host/a', path:'/b/c', params:{d:'e'}})");
        this._urlVersion = 2;
        var url = options;
        this.options = arguments[1] || {};
        var index = url.indexOf("?");
        var path = index === -1 ? url : url.substring(0, index);
        var args = index === -1 ? null : url.substring(index + 1);
        if (/^http(s?):/.test(url)) {
            if (url.indexOf("create.jocket") === -1) {
                this._urlVersion = 1;
                this._createUrl = path + ".jocket" + (args == null ? "" : "?" + args);
            }
            else {
                this._createUrl = url;
            }
        }
        else {
            var pageDir = location.href.replace(/[\?#].*/, "").replace(/[^\/]+$/, "");
            var createUrl = pageDir + "create.jocket?jocket_path=" + encodeURIComponent(path);
            this._createUrl = createUrl + (args == null ? "" : "&" + args);
        }
    }
    this.status = Jocket.STATUS_NEW;
    this.timers = {};
    this._reconnectDelay = Jocket.util.getProperty("reconnectDelay", this.options, 1000);
    this._reconnectDelayMax = Jocket.util.getProperty("reconnectDelayMax", this.options, 20000);
    this._reconnectCount = 0;
    this._listeners = {};
    this._indices = {instance:++Jocket._instanceCount, connection:0, request:0};
    Jocket._lastInstance = this;
    if (this.options.autoOpen !== false) {
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
    if (jocket.status !== Jocket.STATUS_NEW && jocket.status !== Jocket.STATUS_CLOSED) {
        throw new Error("Jocket is already opening/open.");
    }
    jocket.status = Jocket.STATUS_OPENING;
    jocket._indices.connection++;
    jocket._probing = null;
    jocket._transport = null;

    var ajax = jocket._createAjax(jocket._createUrl, "jocket_client");
    ajax.timeout = 20000;
    ajax.timeParamName = "jocket_time";
    ajax.onSuccess = function(response) {
        jocket._logger.debug("Session created: sid=%s", response.sessionId);
        jocket._open(response);
    };
    ajax.onFailure = function(event, status) {
        var message = "Failed to create session: status=%d, result=%o, error=%s";
        jocket._logger.error(message, status, ajax.result, ajax.errorMessage);
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
Jocket.prototype.close = function(code)
{
    this._close(code || Jocket.CLOSE_NORMAL, "close by user");
};

/**
 * Send message to server.
 *
 * @param name [string, required] The message name(type). If there are multiple message types, you can either
 *     enclose the type in data, or use the name parameter directly. This parameter can be null.
 * @param data [any, required] The message body. The data type should be supported by JSON.stringify()
 */
Jocket.prototype.send = function(name, data)
{
    var packet = {type: Jocket.PACKET_TYPE_MESSAGE};
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
 * @param name [string, required] The event name
 * @param listener [function, required] The event listener (a JavaScript function)
 */
Jocket.prototype.on = Jocket.prototype.addEventListener = function(name, listener)
{
    this._listeners[name] = this._listeners[name] || [];
    this._listeners[name].push(listener);
    return this;
};

/**
 * Get server path from current page's URL
 *
 * For example:
 *
 * URL: http://www.abc.com/test?key=value
 * Server: http://www.abc.com
 *
 * URL: http://www.abc.com/user/list?key=value
 * Server: http://www.abc.com/user
 */
Jocket._getServerUrl = function()
{
    // Fortify will complain the following line:
    // var url = window.location.href.replace(/[\?#].*$/, "");
    /^([^\?#]+)/.test(window.location.href);
    var url = RegExp.$1;

    // Remove the last part of URL
    index = url.lastIndexOf("/");
    if (index > 0 && url.charAt(index - 1) !== "/") {
        url = url.substring(0, index);
    }
    return url;
};

Jocket.prototype._open = function(response)
{
    var jocket = this;

    // Basic properties
    jocket.sessionId = response.sessionId;
    jocket.status = Jocket.STATUS_OPEN;
    jocket._reconnectCount = 0;
    jocket._needUpgrade = Jocket.util.getProperty("upgrade", jocket.options, response, true);
    jocket._pingInterval = Jocket.util.getProperty("pingInterval", response, 25000);
    jocket._pingTimeout = Jocket.util.getProperty("pingTimeout", response, 20000);

    // Polling/WebSocket URL. Old path is 'create.jocket', new path is 'jocket/create'
    var url = jocket._createUrl.replace(/\.jocket.*/, "").replace(/jocket\/create.*/, "jocket/");
    for (var i = 0; i < response.pathDepth; ++i) {
        url = url.replace(/\/[^\/]*$/, "");
    }
    if (jocket._urlVersion === 1) {
        jocket._pollMethod = "GET";
        jocket._pollUrl = url + "/jocket?s=" + jocket.sessionId;
        jocket._sendUrl = url + "/jocket?s=" + jocket.sessionId;
    }
    else {
        jocket._pollMethod = "POST";
        jocket._pollUrl = url + "/jocket/poll?s=" + jocket.sessionId;
        jocket._sendUrl = url + "/jocket/send?s=" + jocket.sessionId;
    }
    jocket._webSocketUrl = url.replace(/^http/, "ws") + "/jocket/ws?s=" + jocket.sessionId;

    // Start polling transport and fire open event
    jocket._transport = new Jocket.Polling(jocket);
    jocket._transport.start();
    jocket._fire(Jocket.EVENT_OPEN);

    // Check WebSocket connection
    if (jocket._needUpgrade) {
        jocket._probing = new Jocket.Ws(jocket);
        jocket._probing.start();
    }
};

Jocket.prototype._close = function(code, message)
{
    if (this.status === Jocket.STATUS_CLOSED) {
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
    this._logger.debug("Session closed: sid=%s, code=%d, message=%s", this.sessionId, code, message);
    this._logger.upload();
    this.sessionId = null;
    if (this.options.reconnect !== false) {
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
    if (transport === this._transport || (this._transport == null && transport === this._probing)) {
        this._close(code, message);
        return;
    }
    if (transport === this._probing) {
        delete this._probing;
    }
    transport.destroy(code, message);
};

Jocket.prototype._sendPacket = function(packet)
{
    if (this._transport == null) {
        this._logger.error("Packet ignored: sid=%s, packet=%o", this.sessionId, packet);
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
        if (delay === jocket._reconnectDelayMax) {
            break;
        }
    }
    jocket.timers.reconnect = setTimeout(function() {
        jocket._logger.debug("Reconnect: count=%d", jocket._reconnectCount);
        delete jocket.timers.reconnect;
        jocket.open();
    }, delay);
};

Jocket.prototype._upgrade = function()
{
    this._logger.debug("Upgrade transport to WebSocket: sid=%s", this.sessionId);
    if (this._transport != null && this._probing != null) {
        var polling = this._transport;
        this._transport = this._probing;
        delete this._probing;
        polling.stop();
    }
};

Jocket.prototype._fire = function(name)
{
    if (name in this._listeners) {
        var listeners = this._listeners[name].slice(0);
        var args = Array.prototype.slice.call(arguments, 1);
        for (var i = 0; i < listeners.length; ++i) {
            listeners[i].apply(this, args);
        }
    }
};

Jocket.prototype._createAjax = function(url, clientParamName)
{
    var indices = this._indices;
    indices.request++;
    var key = clientParamName || "c";
    var value = Jocket._pageId + ":" + indices.instance + "," + indices.connection + "," + indices.request;
    var ajaxUrl = url + (url.indexOf("?") === -1 ? "?" : "&") + key + "=" + value;
    return new Jocket.Ajax(ajaxUrl);
};

Jocket.STATUS_NEW     = "new";
Jocket.STATUS_OPENING = "opening";
Jocket.STATUS_OPEN    = "open";
Jocket.STATUS_CLOSED  = "closed";

Jocket.PACKET_TYPE_OPEN    = "open";
Jocket.PACKET_TYPE_CLOSE   = "close";
Jocket.PACKET_TYPE_PING    = "ping";
Jocket.PACKET_TYPE_PONG    = "pong";
Jocket.PACKET_TYPE_UPGRADE = "upgrade";
Jocket.PACKET_TYPE_MESSAGE = "message";
Jocket.PACKET_TYPE_CONFIRM = "confirm";
Jocket.PACKET_TYPE_LOG     = "log";

Jocket.CLOSE_NORMAL            = 1000; //normal closure
Jocket.CLOSE_AWAY              = 1001; //close browser or reload page
Jocket.CLOSE_ABNORMAL          = 1006; //network error
Jocket.CLOSE_NO_SESSION_PARAM  = 3600; //the Jocket session ID parameter is missing
Jocket.CLOSE_SESSION_NOT_FOUND = 3601; //the Jocket session is not found
Jocket.CLOSE_CREATE_FAILED     = 3602; //failed to create Jocket session
Jocket.CLOSE_CONNECT_FAILED    = 3603; //failed to connect to server
Jocket.CLOSE_PING_TIMEOUT      = 3604; //ping timeout
Jocket.CLOSE_POLLING_FAILED    = 3605; //polling failed
Jocket.CLOSE_PING_FAILED       = 3606; //ping failed

Jocket.EVENT_OPEN			= "open";
Jocket.EVENT_CLOSE			= "close";
Jocket.EVENT_UPGRADE		= "upgrade";
Jocket.EVENT_ERROR   		= "error";
Jocket.EVENT_MESSAGE		= "message";
Jocket.EVENT_BROWSER_CLOSE	= "browserclose";

//-----------------------------------------------------------------------
// Jocket.Ws
//-----------------------------------------------------------------------

Jocket.Ws = function(jocket)
{
    this.jocket = jocket;
    this.isFirstPong = true;
    this.timers = {};
};

Jocket.Ws.prototype.start = function()
{
    var ws = this;
    var jocket = ws.jocket;
    ws.socket = new WebSocket(jocket._webSocketUrl);
    ws.socket.onopen = function() {
        jocket._logger.debug("WebSocket opened.");
        ws.ping();
    };
    ws.socket.onclose = function(event) {
        jocket._logger.debug("WebSocket closed: event=%o", event);
        jocket._closeTransport(ws, event.code, event.reason);
    };
    ws.socket.onerror = Jocket.Ws.doSocketError = function(event) {
        jocket._logger.error("WebSocket error: event=%o", event);
        jocket._fire(Jocket.EVENT_ERROR, event);
    };
    ws.socket.onmessage = function(event) {
        var packet = JSON.parse(event.data);
        jocket._logger.debug("Packet received: sid=%s, transport=websocket, packet=%o", jocket.sessionId, packet);
        ws.handlePacket(packet);
    };
};

Jocket.Ws.prototype.destroy = function(code, message)
{
    Jocket.util.clearTimers(this);
    var socket = this.socket;
    if (socket != null) {
        this.socket = null;
        socket.onopen = socket.onclose = socket.onerror = socket.onmessage = null;
        if (socket.readyState === 0 /*CONNECTING*/ || socket.readyState === 1 /*OPEN*/) {
            socket.close(code, message);
        }
        delete this.jocket._transport;
        this.jocket._logger.debug("WebSocket closed: code=%d, message=%s", code, message);
    }
};

Jocket.Ws.prototype.ping = function()
{
    var ws = this;
    var jocket = ws.jocket;
    delete ws.timers.interval;
    ws.sendPacket({type: Jocket.PACKET_TYPE_PING});
    ws.timers.timeout = setTimeout(function() {
        jocket._closeTransport(ws, Jocket.CLOSE_PING_TIMEOUT, "ping timeout");
    }, jocket._pingTimeout);
};

Jocket.Ws.prototype.sendPacket = function(packet, noLog)
{
    var jocket = this.jocket;
    if (!noLog) {
        jocket._logger.debug("Packet send: sid=%s, transport=websocket, packet=%o", jocket.sessionId, packet);
    }
    this.socket.send(JSON.stringify(packet));
};

Jocket.Ws.prototype.handlePacket = function(packet)
{
    var ws = this;
    var jocket = ws.jocket;
    if (packet.type === Jocket.PACKET_TYPE_PONG) {
        Jocket.util.clearPingTimeout(ws);
        ws.timers.interval = setTimeout(function() {
            ws.ping();
        }, jocket._pingInterval);
        if (ws.isFirstPong) {
            ws.isFirstPong = false;
            ws.sendPacket({type: Jocket.PACKET_TYPE_UPGRADE});
        }
    }
    else if (packet.type === Jocket.PACKET_TYPE_MESSAGE) {
        ws.sendPacket({type: Jocket.PACKET_TYPE_CONFIRM, data: packet.id});
        try {
            jocket._fire(Jocket.EVENT_MESSAGE, packet.name, JSON.parse(packet.data || "null"));
        }
        catch (e) {
            jocket._logger.error("Failed to invoke message handler.");
            throw e;
        }
    }
    else {
        jocket._logger.warn("Unsupported message type for WebSocket: %s", packet.type);
    }
};

//-----------------------------------------------------------------------
// Jocket.Polling
//-----------------------------------------------------------------------

Jocket.Polling = function(jocket)
{
    this.jocket = jocket;
    this.pollMethod = jocket._pollMethod;
    this.pollUrl = jocket._pollUrl;
    this.sendUrl = jocket._sendUrl;
    this.timers = {};
};

Jocket.Polling.prototype.start = function()
{
    this.active = true;
    this.poll();
    this.schedulePing();
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
        var reason = {code: code, message: message};
        var packet = {type: Jocket.PACKET_TYPE_CLOSE, data: JSON.stringify(reason)};
        jocket._createAjax(this.sendUrl).submit("POST", packet);
    }
};

Jocket.Polling.prototype.stop = function()
{
    var polling = this;
    polling.active = false;
    Jocket.util.clearTimers(polling);
    polling.timers.destroy = setTimeout(function() {
        polling.destroy();
    }, polling.jocket._pingTimeout);
};

/**
 * URL parameters:
 *      s: sessionId
 *      c: clientId, a unique ID to distinguish client
 *      p: confirmPacketId, the previous received packet ID (If packet is message and ID is not null)
 *      t: client time
 */
Jocket.Polling.prototype.poll = function(confirmPacketId)
{
    var polling = this;
    var jocket = polling.jocket;
    var url = confirmPacketId == null ? polling.pollUrl : polling.pollUrl + "&p=" + confirmPacketId;
    var ajax = jocket._createAjax(url);
    ajax.onSuccess = function(packet) {
        jocket._logger.debug("Packet received: sid=%s, transport=polling, packet=%o", jocket.sessionId, packet);
        polling.handlePacket(packet);
    };
    ajax.onFailure = function() {
        var message = "Polling failed";
        if (ajax.errorMessage != null) {
            message += ": " + ajax.errorMessage;
        }
        jocket._close(Jocket.CLOSE_POLLING_FAILED, message);
    };
    ajax.submit(polling.pollMethod);
};

Jocket.Polling.prototype.sendPacket = function(packet, noLog)
{
    var polling = this;
    var jocket = this.jocket;
    if (!noLog) {
        jocket._logger.debug("Packet send: sid=%s, transport=polling, packet=%o", jocket.sessionId, packet);
    }
    var ajax = jocket._createAjax(this.sendUrl, null, noLog);
    ajax.onFailure = function(event, status) {
        if (!noLog) {
            var message = "Packet send failed: sid=%s, event=%o, status=%d, error=%s";
            jocket._logger.error(message, jocket.sessionId, event, status, ajax.errorMessage);
        }
        if (packet.type === Jocket.PACKET_TYPE_PING) {
            jocket._closeTransport(polling, Jocket.CLOSE_PING_FAILED, "ping failed");
        }
        else {
            jocket._fire(Jocket.EVENT_ERROR, event);
        }
    };
    ajax.submit("POST", packet);
};

Jocket.Polling.prototype.ping = function()
{
    var polling = this;
    var jocket = polling.jocket;
    delete polling.timers.interval;
    polling.sendPacket({type: Jocket.PACKET_TYPE_PING});
    polling.timers.timeout = setTimeout(function() {
        jocket._closeTransport(polling, Jocket.CLOSE_PING_TIMEOUT, "ping timeout");
    }, jocket._pingTimeout);
};

/**
 * Schedule next ping operation
 */
Jocket.Polling.prototype.schedulePing = function()
{
    var polling = this;
    var jocket = polling.jocket;
    polling.timers.interval = setTimeout(function() {
        polling.ping();
    }, jocket._pingInterval);
};

Jocket.Polling.prototype.handlePacket = function(packet)
{
    var polling = this;
    var jocket = polling.jocket;
    if (polling.active) {
        if (packet.type === Jocket.PACKET_TYPE_CLOSE) {
            polling.active = false;
            var data = JSON.parse(packet.data);
            jocket._close(data.code, data.message);
            return;
        }
        if (packet.type === Jocket.PACKET_TYPE_PONG) {
            Jocket.util.clearPingTimeout(polling);
            polling.schedulePing();
        }
        if (packet.type === Jocket.PACKET_TYPE_UPGRADE) {
            jocket._upgrade();
        }
        else {
            var packetId = packet.type === Jocket.PACKET_TYPE_MESSAGE ? packet.id : null;
            polling.poll(packetId);
        }
    }
    else if (packet.type === Jocket.PACKET_TYPE_MESSAGE && packet.id != null) {
        polling.sendPacket({type: Jocket.PACKET_TYPE_CONFIRM, data: packet.id});
    }

    if (packet.type === Jocket.PACKET_TYPE_MESSAGE) {
        try {
            jocket._fire(Jocket.EVENT_MESSAGE, packet.name, JSON.parse(packet.data || "null"));
        }
        catch (e) {
            jocket._logger.error("Failed to invoke message handler.");
            throw e;
        }
    }
};

//-----------------------------------------------------------------------
// Jocket.Ajax
//-----------------------------------------------------------------------

Jocket.Ajax = function(url)
{
    this.url = url;
    this.onSuccess = null;
    this.onFailure = null;
    this.async = true;
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

Jocket.Ajax.prototype.submit = function(method, data)
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
    url += (url.indexOf("?") === -1 ? "?" : "&") + timeParamName + "=" + Jocket._digit64.now();
    xhr.open(method || "GET", url, ajax.async);
    xhr.setRequestHeader("Cache-Control", "no-store, no-cache");
    xhr.setRequestHeader("Jocket-Client", "Web");
    if (ajax.timeout != null) {
        xhr.timeout = ajax.timeout;
    }
    if (data == null) {
        xhr.send();
    }
    else {
        xhr.send(ajax.dataToString(data));
    }
    ajax._status = Jocket.Ajax.STATUS_SENDING;
};

/**
 * Some vulnerability scanning tools report '..\' in data as a problem. (Although it's NOT for Jocket)
 * To avoid this, we convert '.' to other characters, and convert it back on server side.
 *
 * We use a rarely used character (\uF000) as escape character. because the escape character will be
 * converted firstly, use a rarely used character can minimize changes in the original text, and more
 * friendly for human reading.
 *
 * Escape mapping:
 *
 *     \uF000 => \uF000 + '0'
 *     .      => \uF000 + '1'
 */
Jocket.Ajax.prototype.dataToString = function(data)
{
	var str = JSON.stringify(data);
	var esc = '\uF000';
	return str.replace(/\uF000/g, esc + '0').replace(/\./g, esc + '1');
};

Jocket.Ajax.prototype.abort = function()
{
    this.onFailure = null;
    this._xhr.abort();
};

Jocket.Ajax.prototype.isLoad = function()
{
    return this._status === Jocket.Ajax.STATUS_LOAD;
};

Jocket.Ajax.prototype.isError = function()
{
    return this._status === Jocket.Ajax.STATUS_ERROR;
};

Jocket.Ajax.prototype.isAbort = function()
{
    return this._status === Jocket.Ajax.STATUS_ABORT;
};

Jocket.Ajax.prototype.isTimeout = function()
{
    return this._status === Jocket.Ajax.STATUS_TIMEOUT;
};

Jocket.Ajax.prototype._finish = function(event, status)
{
    var ajax = this;
    var xhr = ajax._xhr;
    ajax._status = status;
    ajax.result = ajax._parseResponse(status);
    if (ajax.result != null && ajax.result.success) {
        if (ajax.onSuccess != null) {
            ajax.onSuccess(ajax.result.json);
        }
    }
    else {
        if (ajax.onFailure != null) {
            ajax.onFailure(event, status);
        }
    }
};

Jocket.Ajax.prototype._cleanup = function()
{
    var ajax = this;
    var xhr = ajax._xhr;
    xhr._ajax = null;
    ajax._xhr = null;
};

Jocket.Ajax.prototype._parseResponse = function(status)
{
    var ajax = this;
    var xhr = ajax._xhr;
    var result = {success: false, httpStatus: xhr.status};
    if (xhr.status === 200) {
        var text = xhr.responseText;
        var taint = "for(;;)";
        if (text != null && text.length >= taint.length && text.substring(0, taint.length) === taint) {
            text = text.substring(taint.length);
        }
        try {
            result.json = JSON.parse(text);
            result.success = true;
        }
        catch (e) {
            ajax.errorMessage = "Invalid JSON format. url=" + ajax.url + ", response=" + text;
            console.error(ajax.errorMessage);
        }
    }
    else if (status !== Jocket.Ajax.STATUS_ABORT) {
        ajax.errorMessage = "AJAX failed. url=" + ajax.url + ", status=" + status + ", httpstatus=" + xhr.status;
        console.error(ajax.errorMessage);
    }
    return result;
};

//-----------------------------------------------------------------------
// Logger
//-----------------------------------------------------------------------

Jocket.Logger = function()
{
    this.buffer = [];
};

Jocket._globalLogger = new Jocket.Logger();

Jocket.Logger.prototype.debug = function()
{
    arguments[0] = Jocket.Logger._getPrefix() + arguments[0];
    if (Jocket.isDebug) {
        console.log.apply(console, arguments);
    }
    this._addToBuffer(arguments);
};

Jocket.Logger.prototype.warn = function()
{
    arguments[0] = Jocket.Logger._getPrefix() + arguments[0];
    console.warn.apply(console, arguments);
    this._addToBuffer(arguments);
};

Jocket.Logger.prototype.error = function()
{
    arguments[0] = Jocket.Logger._getPrefix() + arguments[0];
    console.error.apply(console, arguments);
    this._addToBuffer(arguments);
};

Jocket.Logger.prototype.upload = function()
{
	var jocket = this.jocket;
    if (jocket.options.sendLog !== false && this.buffer.length > 0) {
        console.log("Send logs to server: rowCount=%d", this.buffer.length); // Don't use Jocket.Logger here
        var transport = jocket._transport || new Jocket.Polling(jocket);
        transport.sendPacket({type: Jocket.PACKET_TYPE_LOG, data: JSON.stringify(this.buffer)}, true);
    }
    this.buffer = [];
};

Jocket.Logger.prototype._addToBuffer = function(args)
{
    if (this.jocket == null) {
        return;
    }

    var message = args[0];
    var regex = /%([dos])/g;
    var strIndex = 0;
    var argIndex = 1;
    var buf = [];
    var result;
    while ((result = regex.exec(message)) != null) {
        buf.push(message.substring(strIndex, result.index));
        buf.push(result[1] === 'o' ? JSON.stringify(args[argIndex]) : args[argIndex]);
        strIndex = result.index + result[0].length;
        ++argIndex;
    }
    buf.push(message.substring(strIndex));
    var log = buf.join("");
    if (log.length > 1000) {
        log = log.substring(0, 1000);
    }
    this.buffer.push(log);
    if (this.buffer.length >= 20) {
        this.upload();
    }
};

Jocket.Logger._getPrefix = function()
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
        for (var key in transport.timers) {
            clearTimeout(transport.timers[key]);
        }
        transport.timers = {};
    },

    clearPingTimeout: function(transport)
    {
        if (transport.timers.timeout != null) {
            clearTimeout(transport.timers.timeout);
            delete transport.timers.timeout;
        }
    }
};

Jocket._digit64 =
{
    digits: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".split(""),

    serial: 0,

    previousTime: 0,

    now: function()
    {
        var time = Jocket._digit64.zip(new Date().getTime());
        if (time === Jocket._digit64.previousTime) {
            return time + "." + Jocket._digit64.serial++;
        }
        Jocket._digit64.previousTime = time;
        Jocket._digit64.serial = 0;
        return time;
    },

    zip: function(value)
    {
        var text = "";
        while (value !== 0) {
            text = Jocket._digit64.digits[value & 0x3F] + text;
            value >>>= 6;
        }
        return text;
    }
};

Jocket._doBeforeUnload = function()
{
    Jocket._pageCleanup();
};

Jocket._doUnload = function()
{
    Jocket._pageCleanup();
};

Jocket._pageCleanup = function()
{
    if (Jocket._pageCleaned) {
        return;
    }
    var jocket = Jocket._lastInstance;
    if (jocket != null) {
        var ajax = jocket._createAjax(jocket._sendUrl);
        ajax.async = false;
        ajax.onSuccess = function() {
            Jocket._pageCleaned = true;
        };
        ajax.submit("POST", {type: Jocket.EVENT_BROWSER_CLOSE});
        jocket._logger.upload();
    }
};

//-----------------------------------------------------------------------
// Initialization
//-----------------------------------------------------------------------

(function(){
    window.addEventListener("beforeunload", Jocket._doBeforeUnload);
    window.addEventListener("unload", Jocket._doUnload);

    var scripts = document.getElementsByTagName("script");
    var script = scripts[scripts.length - 1];
    Jocket._pageId = Jocket._digit64.now();
    Jocket._instanceCount = 0;
    Jocket._lastInstance = null;
    Jocket._pageCleaned = false;
    Jocket._version = "2.1.0";

    var isDebug = null;
    try {
        for (var w = window; ; w = w.parent) {
            if (/jocket-debug=(true|false)/.test(w.location.href)) {
                isDebug = RegExp.$1 === "true";
            }
            if (w.parent === w) {
                break;
            }
        }
    }
    catch (e) {
        // Cross-origin exception can be ignored here
    }
    Jocket.isDebug = isDebug === null ? /debug=true/.test(script.src) : isDebug;
    Jocket._globalLogger.debug("Jocket library loaded: version=%s, debug=%s", Jocket._version, Jocket.isDebug);
})();
