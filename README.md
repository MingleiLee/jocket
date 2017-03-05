# Jocket

A library integrates WebSocket and long-polling with Java web server as the backend.

Overview
--------

WebSocket is introduced in HTML5 and supported by modern browsers. However, there are some environments where WebSocket is not available. For example, WebSocket may be blocked by the firewall of banks. Jocket is created to solve this problem. When WebSocket is not available, Jocket use long-polling instead.

Jocket behaves like socket.io, but socket.io must use Node.js as the backend. If you are running a Java web server and want use something like socket.io, Jocket is your choice.

Example (server side)
---------------------

The code of server side is very like WebSocket endpoint:

    import io.jocket.connection.JocketConnection;
    import io.jocket.endpoint.JocketAbstractEndpoint;
    import io.jocket.endpoint.JocketCloseReason;
    import io.jocket.endpoint.JocketEndpoint;

    @JocketEndpoint("/my/chat")
    public class MyChat extends JocketAbstractEndpoint
    {
        @Override
        public void onOpen(JocketConnection cn)
        {
            // Your code here
        }

        @Override
        public void onClose(JocketConnection cn, JocketCloseReason reason)
        {
            // Your code here
        }

        @Override
        public void onMessage(JocketConnection cn, String code, String data)
        {
            // Your code here
        }
    }

With an extra line on server startup (for example, in WebListener):

    JocketDeployer.deploy(MyChat.class);

Example (client side)
---------------------

    var jocket = new Jocket({path: "/my/chat"});
    jocket.on("open", function() {
        // Your code here
    });
    jocket.on("close", function(code, reason) {
        // Your code here
    });
    jocket.on("message", function(code, data) {
        // Your code here
    });
    
Cluster
-------

In cluster environment, you should implement the JocketRedisDataSource interface:

    public class MyRedisDataSource implements JocketRedisDataSource
    {
        @Override
        public Jedis getJedis()
        {
            // Return a Jedis connection
        }
    }

And add the following on server startup:

    JocketRedisManager.initialize(new MyRedisDataSource());
    JocketService.setEventQueue(new JocketRedisQueue());
    JocketService.setSessionStore(new JocketRedisSessionStore());
