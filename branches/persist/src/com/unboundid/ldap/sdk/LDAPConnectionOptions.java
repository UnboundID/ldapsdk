/*
 * Copyright 2007-2009 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2009 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk;



import com.unboundid.util.Mutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.util.Validator.*;



/**
 * This class provides a data structure that may be used to configure a number
 * of connection-related properties.  Elements included in the set of connection
 * options include:
 * <UL>
 *   <LI>A flag that indicates whether the SDK should attempt to automatically
 *       re-establish a connection if it is unexpectedly closed.  By default,
 *       it will not attempt to do so.</LI>
 *   <LI>A flag that indicates whether simple bind attempts that contain a
 *       non-empty DN will be required to have a non-empty password.  By
 *       default, a password will be required in such cases.</LI>
 *   <LI>A flag that indicates whether to automatically attempt to follow any
 *       referrals that may be returned by the server.  By default, it will not
 *       automatically attempt to follow referrals.</LI>
 *   <LI>A referral hop limit, which indicates the maximum number of hops that
 *       the connection may take when trying to follow a referral.  The default
 *       referral hop limit is five.</LI>
 *   <LI>A flag that indicates whether to use the SO_KEEPALIVE socket option to
 *       attempt to more quickly detect when idle TCP connections have been lost
 *       or to prevent them from being unexpectedly closed by intermediate
 *       network hardware.  By default, the SO_KEEPALIVE socket option will be
 *       used.</LI>
 *   <LI>A flag that indicates whether to use the SO_LINGER socket option to
 *       indicate how long a connection should linger after it has been closed,
 *       and a value that specifies the length of time that it should linger.
 *       By default, the SO_LINGER option will be used with a timeout of 5
 *       seconds.</LI>
 *   <LI>A flag that indicates whether to use the SO_REUSEADDR socket option to
 *       indicate that a socket in a TIME_WAIT state may be reused.  By default,
 *       the SO_REUSEADDR socket option will be used.</LI>
 *   <LI>A flag that indicates whether to operate in synchronous mode, in which
 *       at most one operation may be in progress at any given time on an
 *       associated connection.</LI>
 *   <LI>A flag that indicates whether to use the TCP_NODELAY socket option to
 *       indicate that any data written to the socket will be sent immediately
 *       rather than delaying for a short amount of time to see if any more data
 *       is to be sent that could potentially be included in the same packet.
 *       By default, the TCP_NODELAY socket option will be used.</LI>
 *   <LI>A value which specifies the maximum length of time in milliseconds that
 *       an attempt to establish a connection should be allowed to block before
 *       failing.  By default, a timeout of 60,000 milliseconds (1 minute) will
 *       be used.</LI>
 *   <LI>A value which specifies the default timeout in milliseconds that the
 *       SDK should wait for a response from the server before failing.  By
 *       default, a timeout of 300,000 milliseconds (5 minutes) will be
 *       used.</LI>
 *   <LI>A value which specifies the largest LDAP message size that the SDK will
 *       be willing to read from the directory server.  By default, the SDK will
 *       not allow responses larger than 20971520 bytes (20MB).  If it
 *       encounters a message that may be larger than the maximum allowed
 *       message size, then the SDK will terminate the connection to the
 *       server.</LI>
 *   <LI>The {@link DisconnectHandler} that should be used to receive
 *       notification if connection is disconnected for any reason.  By default,
 *       no {@code DisconnectHandler} will be used.</LI>
 *   <LI>The {@link UnsolicitedNotificationHandler} that should be used to
 *       receive notification about any unsolicited notifications returned by
 *       the server.  By default, no {@code UnsolicitedNotificationHandler} will
 *       be used.</LI>
 * </UL>
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class LDAPConnectionOptions
{
  /**
   * The default value ({@code false}) for the setting that controls whether to
   * automatically attempt to reconnect if a connection is unexpectedly lost.
   */
  static final boolean DEFAULT_AUTO_RECONNECT = false;



  /**
   * The default value ({@code true}) for the setting that controls whether
   * simple bind requests with a DN are also required to contain a password.
   */
  static final boolean DEFAULT_BIND_WITH_DN_REQUIRES_PASSWORD = true;



  /**
   * The default value ({@code false}) for the setting that controls whether to
   * capture a thread stack trace whenever an attempt is made to establish a
   * connection.
   */
  static final boolean DEFAULT_CAPTURE_CONNECT_STACK_TRACE = false;



  /**
   * The default value ({@code false}) for the setting that controls whether to
   * attempt to automatically follow referrals.
   */
  static final boolean DEFAULT_FOLLOW_REFERRALS = false;



  /**
   * The default value ({@code true}) for the setting that controls whether to
   * use the {@code SO_KEEPALIVE} socket option.
   */
  static final boolean DEFAULT_USE_KEEPALIVE = true;



  /**
   * The default value ({@code true}) for the setting that controls whether to
   * use the {@code SO_LINGER} socket option.
   */
  static final boolean DEFAULT_USE_LINGER = true;



  /**
   * The default value ({@code true}) for the setting that controls whether to
   * use the {@code SO_REUSEADDR} socket option.
   */
  static final boolean DEFAULT_USE_REUSE_ADDRESS = true;



  /**
   * The default value ({@code false}) for the setting that controls whether to
   * use schema when reading data from the server.
   */
  static final boolean DEFAULT_USE_SCHEMA = false;



  /**
   * The default value ({@code false}) for the setting that controls whether to
   * operate in synchronous mode, in which only a single outstanding operation
   * may be in progress on an associated connection at any given time.
   */
  static final boolean DEFAULT_USE_SYNCHRONOUS_MODE = false;



  /**
   * The default value ({@code true}) for the setting that controls whether to
   * use the {@code TCP_NODELAY} socket option.
   */
  static final boolean DEFAULT_USE_TCP_NODELAY = true;



  /**
   * The default value (60000) for the setting that controls the timeout in
   * milliseconds when trying to establish a new connection.
   */
  static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 60000;



  /**
   * The default value (5) for the setting that controls the timeout in seconds
   * that will be used with the {@code SO_LINGER} socket option.
   */
  static final int DEFAULT_LINGER_TIMEOUT_SECONDS = 5;



  /**
   * The default value (20971520 bytes, or 20MB) for the setting that controls
   * the maximum LDAP message size in bytes that will be allowed when reading
   * data from a directory server.
   */
  static final int DEFAULT_MAX_MESSAGE_SIZE = 20971520;



  /**
   * The default size to use for the receive buffer.
   */
  static final int DEFAULT_RECEIVE_BUFFER_SIZE = 0;



  /**
   * The default value (5) for the setting that controls the referral hop limit.
   */
  static final int DEFAULT_REFERRAL_HOP_LIMIT = 5;



  /**
   * The default size to use for the send buffer.
   */
  static final int DEFAULT_SEND_BUFFER_SIZE = 0;



  /**
   * The default value (300000) for the setting that controls the default
   * response timeout in milliseconds.
   */
  static final long DEFAULT_RESPONSE_TIMEOUT_MILLIS = 300000L;



  // Indicates whether the connection should attempt to automatically reconnect
  // if the connection to the server is lost.
  private boolean autoReconnect;

  // Indicates whether to allow simple binds that contain a DN but no password.
  private boolean bindWithDNRequiresPassword;

  // Indicates whether to capture a thread stack trace whenever an attempt is
  // made to establish a connection;
  private boolean captureConnectStackTrace;

  // Indicates whether to attempt to follow any referrals that are encountered.
  private boolean followReferrals;

  // Indicates whether to use SO_KEEPALIVE for the underlying sockets.
  private boolean useKeepAlive;

  // Indicates whether to use SO_LINGER for the underlying sockets.
  private boolean useLinger;

  // Indicates whether to use SO_REUSEADDR for the underlying sockets.
  private boolean useReuseAddress;

  // Indicates whether to try to use schema information when reading data from
  // the server.
  private boolean useSchema;

  // Indicates whether to use synchronous mode in which only a single operation
  // may be in progress on associated connections at any given time.
  private boolean useSynchronousMode;

  // Indicates whether to use TCP_NODELAY for the underlying sockets.
  private boolean useTCPNoDelay;

  // The disconnect handler for associated connections.
  private DisconnectHandler disconnectHandler;

  // The connect timeout, in milliseconds.
  private int connectTimeout;

  // The linger timeout to use if SO_LINGER is to be used.
  private int lingerTimeout;

  // The maximum message size in bytes that will be allowed when reading data
  // from a directory server.
  private int maxMessageSize;

  // The socket receive buffer size to request.
  private int receiveBufferSize;

  // The referral hop limit to use if referral following is enabled.
  private int referralHopLimit;

  // The socket send buffer size to request.
  private int sendBufferSize;

  // The response timeout, in milliseconds.
  private long responseTimeout;

  // The unsolicited notification handler for associated connections.
  private UnsolicitedNotificationHandler unsolicitedNotificationHandler;



  /**
   * Creates a new set of LDAP connection options with the default settings.
   */
  public LDAPConnectionOptions()
  {
    autoReconnect                  = DEFAULT_AUTO_RECONNECT;
    bindWithDNRequiresPassword     = DEFAULT_BIND_WITH_DN_REQUIRES_PASSWORD;
    captureConnectStackTrace       = DEFAULT_CAPTURE_CONNECT_STACK_TRACE;
    followReferrals                = DEFAULT_FOLLOW_REFERRALS;
    useKeepAlive                   = DEFAULT_USE_KEEPALIVE;
    useLinger                      = DEFAULT_USE_LINGER;
    useReuseAddress                = DEFAULT_USE_REUSE_ADDRESS;
    useSchema                      = DEFAULT_USE_SCHEMA;
    useSynchronousMode             = DEFAULT_USE_SYNCHRONOUS_MODE;
    useTCPNoDelay                  = DEFAULT_USE_TCP_NODELAY;
    connectTimeout                 = DEFAULT_CONNECT_TIMEOUT_MILLIS;
    lingerTimeout                  = DEFAULT_LINGER_TIMEOUT_SECONDS;
    maxMessageSize                 = DEFAULT_MAX_MESSAGE_SIZE;
    referralHopLimit               = DEFAULT_REFERRAL_HOP_LIMIT;
    responseTimeout                = DEFAULT_RESPONSE_TIMEOUT_MILLIS;
    receiveBufferSize              = DEFAULT_RECEIVE_BUFFER_SIZE;
    sendBufferSize                 = DEFAULT_SEND_BUFFER_SIZE;
    disconnectHandler              = null;
    unsolicitedNotificationHandler = null;
  }



  /**
   * Returns a duplicate of this LDAP connection options object that may be
   * modified without impacting this instance.
   *
   * @return  A duplicate of this LDAP connection options object that may be
   *          modified without impacting this instance.
   */
  public LDAPConnectionOptions duplicate()
  {
    final LDAPConnectionOptions o = new LDAPConnectionOptions();

    o.autoReconnect                  = autoReconnect;
    o.bindWithDNRequiresPassword     = bindWithDNRequiresPassword;
    o.captureConnectStackTrace       = captureConnectStackTrace;
    o.followReferrals                = followReferrals;
    o.useKeepAlive                   = useKeepAlive;
    o.useLinger                      = useLinger;
    o.useReuseAddress                = useReuseAddress;
    o.useSchema                      = useSchema;
    o.useSynchronousMode             = useSynchronousMode;
    o.useTCPNoDelay                  = useTCPNoDelay;
    o.connectTimeout                 = connectTimeout;
    o.lingerTimeout                  = lingerTimeout;
    o.maxMessageSize                 = maxMessageSize;
    o.responseTimeout                = responseTimeout;
    o.referralHopLimit               = referralHopLimit;
    o.disconnectHandler              = disconnectHandler;
    o.unsolicitedNotificationHandler = unsolicitedNotificationHandler;
    o.receiveBufferSize              = receiveBufferSize;
    o.sendBufferSize                 = sendBufferSize;

    return o;
  }



  /**
   * Indicates whether associated connections should attempt to automatically
   * reconnect to the target server if the connection is lost.
   *
   * @return  {@code true} if associated connections should attempt to
   *          automatically reconnect to the target server if the connection is
   *          lost, or {@code false} if not.
   */
  public boolean autoReconnect()
  {
    return autoReconnect;
  }



  /**
   * Specifies whether associated connections should attempt to automatically
   * reconnect to the target server if the connection is lost.  Note that
   * automatic reconnection will only be available for authenticated clients if
   * the authentication mechanism used provides support for re-binding on a new
   * connection.
   *
   * @param  autoReconnect  Specifies whether associated connections should
   *                        attempt to automatically reconnect to the target
   *                        server if the connection is lost.
   */
  public void setAutoReconnect(final boolean autoReconnect)
  {
    this.autoReconnect = autoReconnect;
  }



  /**
   * Indicates whether the SDK should allow simple bind operations that contain
   * a bind DN but no password.  Binds of this type may represent a security
   * vulnerability in client applications because they may cause the client to
   * believe that the user is properly authenticated when the server considers
   * it to be an unauthenticated connection.
   *
   * @return  {@code true} if the SDK should allow simple bind operations that
   *          contain a bind DN but no password, or {@code false} if not.
   */
  public boolean bindWithDNRequiresPassword()
  {
    return bindWithDNRequiresPassword;
  }



  /**
   * Specifies whether the SDK should allow simple bind operations that contain
   * a bind DN but no password.
   *
   * @param  bindWithDNRequiresPassword  Indicates whether the SDK should allow
   *                                     simple bind operations that contain a
   *                                     bind DN but no password.
   */
  public void setBindWithDNRequiresPassword(
                   final boolean bindWithDNRequiresPassword)
  {
    this.bindWithDNRequiresPassword = bindWithDNRequiresPassword;
  }



  /**
   * Indicates whether the LDAP SDK should capture a thread stack trace for each
   * attempt made to establish a connection.  If this is enabled, then the
   * {@link LDAPConnection#getConnectStackTrace()}  method may be used to
   * retrieve the stack trace.
   *
   * @return  {@code true} if a thread stack trace should be captured whenever a
   *          connection is established, or {@code false} if not.
   */
  public boolean captureConnectStackTrace()
  {
    return captureConnectStackTrace;
  }



  /**
   * Specifies whether the LDAP SDK should capture a thread stack trace for each
   * attempt made to establish a connection.
   *
   * @param  captureConnectStackTrace  Indicates whether to capture a thread
   *                                   stack trace for each attempt made to
   *                                   establish a connection.
   */
  public void setCaptureConnectStackTrace(
                   final boolean captureConnectStackTrace)
  {
    this.captureConnectStackTrace = captureConnectStackTrace;
  }



  /**
   * Retrieves the maximum length of time in milliseconds that a connection
   * attempt should be allowed to continue before giving up.
   *
   * @return  The maximum length of time in milliseconds that a connection
   *          attempt should be allowed to continue before giving up, or zero
   *          to indicate that there should be no connect timeout.
   */
  public int getConnectTimeoutMillis()
  {
    return connectTimeout;
  }



  /**
   * Specifies the maximum length of time in milliseconds that a connection
   * attempt should be allowed to continue before giving up.  A value of zero
   * indicates that there should be no connect timeout.
   *
   * @param  connectTimeout  The maximum length of time in milliseconds that a
   *                         connection attempt should be allowed to continue
   *                         before giving up.
   */
  public void setConnectTimeoutMillis(final int connectTimeout)
  {
    this.connectTimeout = connectTimeout;
  }



  /**
   * Retrieves the maximum length of time in milliseconds that an operation
   * should be allowed to block while waiting for a response from the server.
   * This may be overridden on a per-operation basis.
   *
   * @return  The maximum length of time in milliseconds that an operation
   *          should be allowed to block while waiting for a response from the
   *          server, or zero if there should not be any default timeout.
   */
  public long getResponseTimeoutMillis()
  {
    return responseTimeout;
  }



  /**
   * Specifies the maximum length of time in milliseconds that an operation
   * should be allowed to block while waiting for a response from the server.  A
   * value of zero indicates that there should be no timeout.
   *
   * @param  responseTimeout  The maximum length of time in milliseconds that an
   *                          operation should be allowed to block while waiting
   *                          for a response from the server.
   *
   */
  public void setResponseTimeoutMillis(final long responseTimeout)
  {
    if (responseTimeout < 0)
    {
      this.responseTimeout = 0L;
    }
    else
    {
      this.responseTimeout = responseTimeout;
    }
  }



  /**
   * Indicates whether to use the SO_KEEPALIVE option for the underlying sockets
   * used by associated connections.
   *
   * @return  {@code true} if the SO_KEEPALIVE option should be used for the
   *          underlying sockets, or {@code false} if not.
   */
  public boolean useKeepAlive()
  {
    return useKeepAlive;
  }



  /**
   * Specifies whether to use the SO_KEEPALIVE option for the underlying sockets
   * used by associated connections.  Changes to this setting will take effect
   * only for new sockets, and not for existing sockets.
   *
   * @param  useKeepAlive  Indicates whether to use the SO_KEEPALIVE option for
   *                       the underlying sockets used by associated
   *                       connections.
   */
  public void setUseKeepAlive(final boolean useKeepAlive)
  {
    this.useKeepAlive = useKeepAlive;
  }



  /**
   * Indicates whether to use the SO_LINGER option for the underlying sockets
   * used by associated connections.
   *
   * @return  {@code true} if the SO_LINGER option should be used for the
   *          underlying sockets, or {@code false} if not.
   */
  public boolean useLinger()
  {
    return useLinger;
  }



  /**
   * Retrieves the linger timeout in seconds that will be used if the SO_LINGER
   * socket option is enabled.
   *
   * @return  The linger timeout in seconds that will be used if the SO_LINGER
   *          socket option is enabled.
   */
  public int getLingerTimeoutSeconds()
  {
    return lingerTimeout;
  }



  /**
   * Specifies whether to use the SO_LINGER option for the underlying sockets
   * used by associated connections.  Changes to this setting will take effect
   * only for new sockets, and not for existing sockets.
   *
   * @param  useLinger      Indicates whether to use the SO_LINGER option for
   *                        the underlying sockets used by associated
   *                        connections.
   * @param  lingerTimeout  The linger timeout in seconds that should be used if
   *                        this capability is enabled.
   */
  public void setUseLinger(final boolean useLinger, final int lingerTimeout)
  {
    this.useLinger     = useLinger;
    this.lingerTimeout = lingerTimeout;
  }



  /**
   * Indicates whether to use the SO_REUSEADDR option for the underlying sockets
   * used by associated connections.
   *
   * @return  {@code true} if the SO_REUSEADDR option should be used for the
   *          underlying sockets, or {@code false} if not.
   */
  public boolean useReuseAddress()
  {
    return useReuseAddress;
  }



  /**
   * Specifies whether to use the SO_KEEPALIVE option for the underlying sockets
   * used by associated connections.  Changes to this setting will take effect
   * only for new sockets, and not for existing sockets.
   *
   * @param  useReuseAddress  Indicates whether to use the SO_REUSEADDR option
   *                          for the underlying sockets used by associated
   *                          connections.
   */
  public void setUseReuseAddress(final boolean useReuseAddress)
  {
    this.useReuseAddress = useReuseAddress;
  }



  /**
   * Indicates whether to try to use schema information when reading data from
   * the server (e.g., to select the appropriate matching rules for the
   * attributes included in a search result entry).
   *
   * @return  {@code true} if schema should be used when reading data from the
   *          server, or {@code false} if not.
   */
  public boolean useSchema()
  {
    return useSchema;
  }



  /**
   * Specifies whether to try to use schema information when reading data from
   * the server  (e.g., to select the appropriate matching rules for the
   * attributes included in a search result entry).
   *
   * @param  useSchema  Indicates whether to try to use schema information when
   *                    reading data from the server.
   */
  public void setUseSchema(final boolean useSchema)
  {
    this.useSchema = useSchema;
  }



  /**
   * Indicates whether to operate in synchronous mode, in which at most one
   * operation may be in progress at any time on a given connection, which may
   * allow it to operate more efficiently and without requiring a separate
   * reader thread per connection.  The LDAP SDK will not absolutely enforce
   * this restriction, but when operating in this mode correct behavior
   * cannot be guaranteed when multiple attempts are made to use a connection
   * for multiple concurrent operations.
   * <BR><BR>
   * Note that if synchronous mode is to be used, then this connection option
   * must be set on the connection before any attempt is made to establish the
   * connection.  Once the connection has been established, then it will
   * continue to operate in synchronous or asynchronous mode based on the
   * options in place at the time it was connected.
   *
   * @return  {@code true} if associated connections should operate in
   *          synchronous mode, or {@code false} if not.
   */
  public boolean useSynchronousMode()
  {
    return useSynchronousMode;
  }



  /**
   * Specifies whether to operate in synchronous mode, in which at most one
   * operation may be in progress at any time on a given connection.
   * <BR><BR>
   * Note that if synchronous mode is to be used, then this connection option
   * must be set on the connection before any attempt is made to establish the
   * connection.  Once the connection has been established, then it will
   * continue to operate in synchronous or asynchronous mode based on the
   * options in place at the time it was connected.
   *
   * @param  useSynchronousMode  Indicates whether to operate in synchronous
   *                             mode.
   */
  public void setUseSynchronousMode(final boolean useSynchronousMode)
  {
    this.useSynchronousMode = useSynchronousMode;
  }



  /**
   * Indicates whether to use the TCP_NODELAY option for the underlying sockets
   * used by associated connections.
   *
   * @return  {@code true} if the TCP_NODELAY option should be used for the
   *          underlying sockets, or {@code false} if not.
   */
  public boolean useTCPNoDelay()
  {
    return useTCPNoDelay;
  }



  /**
   * Specifies whether to use the SO_KEEPALIVE option for the underlying sockets
   * used by associated connections.  Changes to this setting will take effect
   * only for new sockets, and not for existing sockets.
   *
   * @param  useTCPNoDelay  Indicates whether to use the TCP_NODELAY option for
   *                        the underlying sockets used by associated
   *                        connections.
   */
  public void setUseTCPNoDelay(final boolean useTCPNoDelay)
  {
    this.useTCPNoDelay = useTCPNoDelay;
  }



  /**
   * Indicates whether associated connections should attempt to follow any
   * referrals that they encounter.
   *
   * @return  {@code true} if associated connections should attempt to follow
   *          any referrals that they encounter, or {@code false} if not.
   */
  public boolean followReferrals()
  {
    return followReferrals;
  }



  /**
   * Specifies whether associated connections should attempt to follow any
   * referrals that they encounter, using the referral connector for the
   * associated connection.
   *
   * @param  followReferrals  Specifies whether associated connections should
   *                          attempt to follow any referrals that they
   *                          encounter.
   */
  public void setFollowReferrals(final boolean followReferrals)
  {
    this.followReferrals = followReferrals;
  }



  /**
   * Retrieves the maximum number of hops that a connection should take when
   * trying to follow a referral.
   *
   * @return  The maximum number of hops that a connection should take when
   *          trying to follow a referral.
   */
  public int getReferralHopLimit()
  {
    return referralHopLimit;
  }



  /**
   * Specifies the maximum number of hops that a connection should take when
   * trying to follow a referral.
   *
   * @param  referralHopLimit  The maximum number of hops that a connection
   *                           should take when trying to follow a referral.  It
   *                           must be greater than zero.
   */
  public void setReferralHopLimit(final int referralHopLimit)
  {
    ensureTrue(referralHopLimit > 0,
         "LDAPConnectionOptions.referralHopLimit must be greater than 0.");

    this.referralHopLimit = referralHopLimit;
  }



  /**
   * Retrieves the maximum size in bytes for an LDAP message that a connection
   * will attempt to read from the directory server.  If it encounters an LDAP
   * message that is larger than this size, then the connection will be
   * terminated.
   *
   * @return  The maximum size in bytes for an LDAP message that a connection
   *          will attempt to read from the directory server, or 0 if no limit
   *          will be enforced.
   */
  public int getMaxMessageSize()
  {
    return maxMessageSize;
  }



  /**
   * Specifies the maximum size in bytes for an LDAP message that a connection
   * will attempt to read from the directory server.  If it encounters an LDAP
   * message that is larger than this size, then the connection will be
   * terminated.
   *
   * @param  maxMessageSize  The maximum size in bytes for an LDAP message that
   *                         a connection will attempt to read from the
   *                         directory server.  A value less than or equal to
   *                         zero indicates that no limit should be enforced.
   */
  public void setMaxMessageSize(final int maxMessageSize)
  {
    if (maxMessageSize > 0)
    {
      this.maxMessageSize = maxMessageSize;
    }
    else
    {
      this.maxMessageSize = 0;
    }
  }



  /**
   * Retrieves the disconnect handler to use for associated connections.
   *
   * @return  the disconnect handler to use for associated connections, or
   *          {@code null} if none is defined.
   */
  public DisconnectHandler getDisconnectHandler()
  {
    return disconnectHandler;
  }



  /**
   * Specifies the disconnect handler to use for associated connections.
   *
   * @param  handler  The disconnect handler to use for associated connections.
   */
  public void setDisconnectHandler(final DisconnectHandler handler)
  {
    disconnectHandler = handler;
  }



  /**
   * Retrieves the unsolicited notification handler to use for associated
   * connections.
   *
   * @return  The unsolicited notification handler to use for associated
   *          connections, or {@code null} if none is defined.
   */
  public UnsolicitedNotificationHandler getUnsolicitedNotificationHandler()
  {
    return unsolicitedNotificationHandler;
  }



  /**
   * Specifies the unsolicited notification handler to use for associated
   * connections.
   *
   * @param  handler  The unsolicited notification handler to use for associated
   *                  connections.
   */
  public void setUnsolicitedNotificationHandler(
                   final UnsolicitedNotificationHandler handler)
  {
    unsolicitedNotificationHandler = handler;
  }



  /**
   * Retrieves the socket receive buffer size that should be requested when
   * establishing a connection.
   *
   * @return  The socket receive buffer size that should be requested when
   *          establishing a connection, or zero if the default size should be
   *          used.
   */
  public int getReceiveBufferSize()
  {
    return receiveBufferSize;
  }



  /**
   * Specifies the socket receive buffer size that should be requested when
   * establishing a connection.
   *
   * @param  receiveBufferSize  The socket receive buffer size that should be
   *                            requested when establishing a connection, or
   *                            zero if the default size should be used.
   */
  public void setReceiveBufferSize(final int receiveBufferSize)
  {
    if (receiveBufferSize < 0)
    {
      this.receiveBufferSize = 0;
    }
    else
    {
      this.receiveBufferSize = receiveBufferSize;
    }
  }



  /**
   * Retrieves the socket send buffer size that should be requested when
   * establishing a connection.
   *
   * @return  The socket send buffer size that should be requested when
   *          establishing a connection, or zero if the default size should be
   *          used.
   */
  public int getSendBufferSize()
  {
    return sendBufferSize;
  }



  /**
   * Specifies the socket send buffer size that should be requested when
   * establishing a connection.
   *
   * @param  sendBufferSize  The socket send buffer size that should be
   *                         requested when establishing a connection, or zero
   *                         if the default size should be used.
   */
  public void setSendBufferSize(final int sendBufferSize)
  {
    if (sendBufferSize < 0)
    {
      this.sendBufferSize = 0;
    }
    else
    {
      this.sendBufferSize = sendBufferSize;
    }
  }



  /**
   * Retrieves a string representation of this LDAP connection.
   *
   * @return  A string representation of this LDAP connection.
   */
  @Override()
  public String toString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this LDAP connection to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which to append a string representation of
   *                 this LDAP connection.
   */
  public void toString(final StringBuilder buffer)
  {
    buffer.append("LDAPConnectionOptions(autoReconnect=");
    buffer.append(autoReconnect);
    buffer.append(", followReferrals=");
    buffer.append(followReferrals);
    if (followReferrals)
    {
      buffer.append(", referralHopLimit=");
      buffer.append(referralHopLimit);
    }
    buffer.append(", useKeepAlive=");
    buffer.append(useKeepAlive);
    buffer.append(", useLinger=");
    if (useLinger)
    {
      buffer.append("true, lingerTimeoutSeconds=");
      buffer.append(lingerTimeout);
    }
    else
    {
      buffer.append("false");
    }
    buffer.append(", useReuseAddress=");
    buffer.append(useReuseAddress);
    buffer.append(", useSchema=");
    buffer.append(useSchema);
    buffer.append(", useSynchronousMode=");
    buffer.append(useSynchronousMode);
    buffer.append(", useTCPNoDelay=");
    buffer.append(useTCPNoDelay);
    buffer.append(", captureConnectStackTrace=");
    buffer.append(captureConnectStackTrace);
    buffer.append(", connectTimeoutMillis=");
    buffer.append(connectTimeout);
    buffer.append(", responseTimeoutMillis=");
    buffer.append(responseTimeout);
    buffer.append(", maxMessageSize=");
    buffer.append(maxMessageSize);
    buffer.append(", receiveBufferSize=");
    buffer.append(receiveBufferSize);
    buffer.append(", sendBufferSize=");
    buffer.append(sendBufferSize);
    buffer.append(')');
  }
}
