package com.jeedsoft.jocket.connection;

public class JocketCloseCode
{
	//standard codes (see javax.websocket.CloseReason)
    public static final int NORMAL				= 1000; //normal closure
    public static final int GOING_AWAY			= 1001;	//close browser or reload page
    public static final int CLOSED_ABNORMALLY	= 1006; //network error
    
    //custom codes (the library level close codes should between 3000-3999, see RFC 6455)
    public static final int NO_SESSION_PARAM  	= 3600; //the Jocket session ID parameter is missing
    public static final int SESSION_NOT_FOUND	= 3601; //the Jocket session not found
    public static final int CREATE_FAILED		= 3602; //failed to create Jocket session
    public static final int CONNECT_FAILED		= 3603; //all available transports failed
}
