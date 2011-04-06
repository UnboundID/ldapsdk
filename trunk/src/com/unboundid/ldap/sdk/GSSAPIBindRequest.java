/*
 * Copyright 2009-2011 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2011 UnboundID Corp.
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



import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.util.DebugType;
import com.unboundid.util.InternalUseOnly;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class provides a SASL GSSAPI bind request implementation as described in
 * <A HREF="http://www.ietf.org/rfc/rfc4752.txt">RFC 4752</A>.  It provides the
 * ability to authenticate to a directory server using Kerberos V, which can
 * serve as a kind of single sign-on mechanism that may be shared across
 * client applications that support Kerberos.  At present, this implementation
 * may only be used for authentication, as it does not yet offer support for
 * integrity or confidentiality.
 * <BR><BR>
 * This class uses the Java Authentication and Authorization Service (JAAS)
 * behind the scenes to perform all Kerberos processing.  This framework
 * requires a configuration file to indicate the underlying mechanism to be
 * used.  It is possible for clients to explicitly specify the path to the
 * configuration file that should be used, but if none is given then a default
 * file will be created and used.  This default file should be sufficient for
 * Sun-provided JVMs, but a custom file may be required for JVMs provided by
 * other vendors.
 * <BR><BR>
 * Elements included in a GSSAPI bind request include:
 * <UL>
 *   <LI>Authentication ID -- A string which identifies the user that is
 *       attempting to authenticate.  It should be the user's Kerberos
 *       principal.</LI>
 *   <LI>Authorization ID -- An optional string which specifies an alternate
 *       authorization identity that should be used for subsequent operations
 *       requested on the connection.  Like the authentication ID, the
 *       authorization ID should be a Kerberos principal.</LI>
 *   <LI>KDC Address -- An optional string which specifies the IP address or
 *       resolvable name for the Kerberos key distribution center.  If this is
 *       not provided, an attempt will be made to determine the appropriate
 *       value from the system configuration.</LI>
 *   <LI>Realm -- An optional string which specifies the realm into which the
 *       user should authenticate.  If this is not provided, an attempt will be
 *       made to determine the appropriate value from the system
 *       configuration</LI>
 *   <LI>Password -- The clear-text password for the target user in the Kerberos
 *       realm.</LI>
 * </UL>
 * <H2>Example</H2>
 * The following example demonstrates the process for performing a GSSAPI bind
 * against a directory server with a username of "john.doe" and a password
 * of "password":
 * <PRE>
 *   GSSAPIBindRequest bindRequest =
 *        new GSSAPIBindRequest("john.doe@EXAMPLE.COM", "password");
 *   try
 *   {
 *     BindResult bindResult = connection.bind(bindRequest);
 *     // If we get here, then the bind was successful.
 *   }
 *   catch (LDAPException le)
 *   {
 *     // The bind failed for some reason.
 *   }
 * </PRE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class GSSAPIBindRequest
       extends SASLBindRequest
       implements CallbackHandler, PrivilegedExceptionAction<Object>
{
  /**
   * The name for the GSSAPI SASL mechanism.
   */
  public static final String GSSAPI_MECHANISM_NAME = "GSSAPI";



  /**
   * The name of the configuration property used to specify the address of the
   * Kerberos key distribution center.
   */
  private static final String PROPERTY_KDC_ADDRESS = "java.security.krb5.kdc";



  /**
   * The name of the configuration property used to specify the Kerberos realm.
   */
  private static final String PROPERTY_REALM = "java.security.krb5.realm";



  /**
   * The name of the configuration property used to specify the path to the JAAS
   * configuration file.
   */
  private static final String PROPERTY_CONFIG_FILE =
       "java.security.auth.login.config";



  /**
   * The name of the configuration property used to indicate whether credentials
   * can come from somewhere other than the location specified in the JAAS
   * configuration file.
   */
  private static final String PROPERTY_SUBJECT_CREDS_ONLY =
       "javax.security.auth.useSubjectCredsOnly";



  /**
   * The name that will identify this client to the JAAS framework.
   */
  private static final String JAAS_CLIENT_NAME = "GSSAPIBindRequest";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 2511890818146955112L;



  // The password for the GSSAPI bind request.
  private final ASN1OctetString password;

  // A reference to the connection to use for bind processing.
  private final AtomicReference<LDAPConnection> conn;

  // Indicates whether to enable JVM-level debugging for GSSAPI processing.
  private final boolean enableGSSAPIDebugging;

  // Indicates whether to attempt to renew the client's existing ticket-granting
  // ticket if authentication uses an existing Kerberos session.
  private boolean renewTGT;

  // Indicates whether to require that the credentials be obtained from the
  // ticket cache such that authentication will fail if the client does not have
  // an existing Kerberos session.
  private boolean requireCachedCredentials;

  // Indicates whether to enable the use pf a ticket cache.
  private boolean useTicketCache;

  // The message ID from the last LDAP message sent from this request.
  private int messageID;

  // The authentication ID string for the GSSAPI bind request.
  private final String authenticationID;

  // The authorization ID string for the GSSAPI bind request, if available.
  private final String authorizationID;

  // The path to the JAAS configuration file to use for bind processing.
  private final String configFilePath;

  // The KDC address for the GSSAPI bind request, if available.
  private final String kdcAddress;

  // The realm for the GSSAPI bind request, if available.
  private final String realm;

  // The protocol that should be used in the Kerberos service principal for
  // the server system.
  private final String servicePrincipalProtocol;

  // The path to the Kerberos ticket cache to use.
  private String ticketCachePath;



  /**
   * Creates a new SASL GSSAPI bind request with the provided authentication ID
   * and password.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID, final String password)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, password));
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided authentication ID
   * and password.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID, final byte[] password)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, password));
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided authentication ID
   * and password.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  controls          The set of controls to include in the request.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID, final String password,
                           final Control[] controls)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, password), controls);
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided authentication ID
   * and password.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  controls          The set of controls to include in the request.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID, final byte[] password,
                           final Control[] controls)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, password), controls);
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided information.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  authorizationID   The authorization ID for this bind request.  It
   *                           may be {@code null} if the authorization ID
   *                           should be the same as the authentication ID.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  realm             The realm to use for the authentication.  It may
   *                           be {@code null} to attempt to use the default
   *                           realm from the system configuration.
   * @param  kdcAddress        The address of the Kerberos key distribution
   *                           center.  It may be {@code null} to attempt to use
   *                           the default KDC from the system configuration.
   * @param  configFilePath    The path to the JAAS configuration file to use
   *                           for the authentication processing.  It may be
   *                           {@code null} to use the default JAAS
   *                           configuration.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID,
                           final String authorizationID, final String password,
                           final String realm, final String kdcAddress,
                           final String configFilePath)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, authorizationID,
         new ASN1OctetString(password), realm, kdcAddress, configFilePath));
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided information.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  authorizationID   The authorization ID for this bind request.  It
   *                           may be {@code null} if the authorization ID
   *                           should be the same as the authentication ID.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  realm             The realm to use for the authentication.  It may
   *                           be {@code null} to attempt to use the default
   *                           realm from the system configuration.
   * @param  kdcAddress        The address of the Kerberos key distribution
   *                           center.  It may be {@code null} to attempt to use
   *                           the default KDC from the system configuration.
   * @param  configFilePath    The path to the JAAS configuration file to use
   *                           for the authentication processing.  It may be
   *                           {@code null} to use the default JAAS
   *                           configuration.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID,
                           final String authorizationID, final byte[] password,
                           final String realm, final String kdcAddress,
                           final String configFilePath)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, authorizationID,
         new ASN1OctetString(password), realm, kdcAddress, configFilePath));
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided information.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  authorizationID   The authorization ID for this bind request.  It
   *                           may be {@code null} if the authorization ID
   *                           should be the same as the authentication ID.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  realm             The realm to use for the authentication.  It may
   *                           be {@code null} to attempt to use the default
   *                           realm from the system configuration.
   * @param  kdcAddress        The address of the Kerberos key distribution
   *                           center.  It may be {@code null} to attempt to use
   *                           the default KDC from the system configuration.
   * @param  configFilePath    The path to the JAAS configuration file to use
   *                           for the authentication processing.  It may be
   *                           {@code null} to use the default JAAS
   *                           configuration.
   * @param  controls          The set of controls to include in the request.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID,
                           final String authorizationID, final String password,
                           final String realm, final String kdcAddress,
                           final String configFilePath,
                           final Control[] controls)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, authorizationID,
         new ASN1OctetString(password), realm, kdcAddress, configFilePath),
         controls);
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided information.
   *
   * @param  authenticationID  The authentication ID for this bind request.  It
   *                           must not be {@code null}.
   * @param  authorizationID   The authorization ID for this bind request.  It
   *                           may be {@code null} if the authorization ID
   *                           should be the same as the authentication ID.
   * @param  password          The password for this bind request.  It must not
   *                           be {@code null}.
   * @param  realm             The realm to use for the authentication.  It may
   *                           be {@code null} to attempt to use the default
   *                           realm from the system configuration.
   * @param  kdcAddress        The address of the Kerberos key distribution
   *                           center.  It may be {@code null} to attempt to use
   *                           the default KDC from the system configuration.
   * @param  configFilePath    The path to the JAAS configuration file to use
   *                           for the authentication processing.  It may be
   *                           {@code null} to use the default JAAS
   *                           configuration.
   * @param  controls          The set of controls to include in the request.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final String authenticationID,
                           final String authorizationID, final byte[] password,
                           final String realm, final String kdcAddress,
                           final String configFilePath,
                           final Control[] controls)
         throws LDAPException
  {
    this(new GSSAPIBindRequestProperties(authenticationID, authorizationID,
         new ASN1OctetString(password), realm, kdcAddress, configFilePath),
         controls);
  }



  /**
   * Creates a new SASL GSSAPI bind request with the provided set of properties.
   *
   * @param  gssapiProperties  The set of properties that should be used for
   *                           the GSSAPI bind request.  It must not be
   *                           {@code null}.
   * @param  controls          The set of controls to include in the request.
   *
   * @throws  LDAPException  If a problem occurs while creating the JAAS
   *                         configuration file to use during authentication
   *                         processing.
   */
  public GSSAPIBindRequest(final GSSAPIBindRequestProperties gssapiProperties,
                           final Control... controls)
          throws LDAPException
  {
    super(controls);

    ensureNotNull(gssapiProperties);

    authenticationID         = gssapiProperties.getAuthenticationID();
    password                 = gssapiProperties.getPassword();
    realm                    = gssapiProperties.getRealm();
    kdcAddress               = gssapiProperties.getKDCAddress();
    servicePrincipalProtocol = gssapiProperties.getServicePrincipalProtocol();
    enableGSSAPIDebugging    = gssapiProperties.enableGSSAPIDebugging();
    useTicketCache           = gssapiProperties.useTicketCache();
    requireCachedCredentials = gssapiProperties.requireCachedCredentials();
    renewTGT                 = gssapiProperties.renewTGT();
    ticketCachePath          = gssapiProperties.getTicketCachePath();

    conn      = new AtomicReference<LDAPConnection>();
    messageID = -1;

    final String authzID = gssapiProperties.getAuthorizationID();
    if (authzID == null)
    {
      authorizationID = authenticationID;
    }
    else
    {
      authorizationID = authzID;
    }

    final String cfgPath = gssapiProperties.getConfigFilePath();
    if (cfgPath == null)
    {
      configFilePath = getConfigFilePath(gssapiProperties);
    }
    else
    {
      configFilePath = cfgPath;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getSASLMechanismName()
  {
    return GSSAPI_MECHANISM_NAME;
  }



  /**
   * Retrieves the authentication ID for the GSSAPI bind request, if defined.
   *
   * @return  The authentication ID for the GSSAPI bind request, or {@code null}
   *          if an existing Kerberos session should be used.
   */
  public String getAuthenticationID()
  {
    return authenticationID;
  }



  /**
   * Retrieves the authorization ID for this bind request, if any.
   *
   * @return  The authorization ID for this bind request, or {@code null} if
   *          there should not be a separate authorization identity.
   */
  public String getAuthorizationID()
  {
    return authorizationID;
  }



  /**
   * Retrieves the string representation of the password for this bind request,
   * if defined.
   *
   * @return  The string representation of the password for this bind request,
   *          or {@code null} if an existing Kerberos session should be used.
   */
  public String getPasswordString()
  {
    if (password == null)
    {
      return null;
    }
    else
    {
      return password.stringValue();
    }
  }



  /**
   * Retrieves the bytes that comprise the the password for this bind request,
   * if defined.
   *
   * @return  The bytes that comprise the password for this bind request, or
   *          {@code null} if an existing Kerberos session should be used.
   */
  public byte[] getPasswordBytes()
  {
    if (password == null)
    {
      return null;
    }
    else
    {
      return password.getValue();
    }
  }



  /**
   * Retrieves the realm for this bind request, if any.
   *
   * @return  The realm for this bind request, or {@code null} if none was
   *          defined and the client should attempt to determine the realm from
   *          the system configuration.
   */
  public String getRealm()
  {
    return realm;
  }



  /**
   * Retrieves the address of the Kerberos key distribution center.
   *
   * @return  The address of the Kerberos key distribution center, or
   *          {@code null} if none was defined and the client should attempt to
   *          determine the KDC address from the system configuration.
   */
  public String getKDCAddress()
  {
    return kdcAddress;
  }



  /**
   * Retrieves the path to the JAAS configuration file that will be used during
   * authentication processing.
   *
   * @return  The path to the JAAS configuration file that will be used during
   *          authentication processing.
   */
  public String getConfigFilePath()
  {
    return configFilePath;
  }



  /**
   * Retrieves the protocol specified in the service principal that the
   * directory server uses for its communication with the KDC.
   *
   * @return  The protocol specified in the service principal that the directory
   *          server uses for its communication with the KDC.
   */
  public String getServicePrincipalProtocol()
  {
    return servicePrincipalProtocol;
  }



  /**
   * Indicates whether to enable the use of a ticket cache to to avoid the need
   * to supply credentials if the client already has an existing Kerberos
   * session.
   *
   * @return  {@code true} if a ticket cache may be used to take advantage of an
   *          existing Kerberos session, or {@code false} if Kerberos
   *          credentials should always be provided.
   */
  public boolean useTicketCache()
  {
    return useTicketCache;
  }



  /**
   * Indicates whether GSSAPI authentication should only occur using an existing
   * Kerberos session.
   *
   * @return  {@code true} if GSSAPI authentication should only use an existing
   *          Kerberos session and should fail if the client does not have an
   *          existing session, or {@code false} if the client will be allowed
   *          to create a new session if one does not already exist.
   */
  public boolean requireCachedCredentials()
  {
    return requireCachedCredentials;
  }



  /**
   * Retrieves the path to the Kerberos ticket cache file that should be used
   * during authentication, if defined.
   *
   * @return  The path to the Kerberos ticket cache file that should be used
   *          during authentication, or {@code null} if the default ticket cache
   *          file should be used.
   */
  public String getTicketCachePath()
  {
    return ticketCachePath;
  }



  /**
   * Indicates whether to attempt to renew the client's ticket-granting ticket
   * (TGT) if an existing Kerberos session is used to authenticate.
   *
   * @return  {@code true} if the client should attempt to renew its
   *          ticket-granting ticket if the authentication is processed using an
   *          existing Kerberos session, or {@code false} if not.
   */
  public boolean renewTGT()
  {
    return renewTGT;
  }



  /**
   * Indicates whether JVM-level debugging should be enabled for GSSAPI bind
   * processing.
   *
   * @return  {@code true} if JVM-level debugging should be enabled for GSSAPI
   *          bind processing, or {@code false} if not.
   */
  public boolean enableGSSAPIDebugging()
  {
    return enableGSSAPIDebugging;
  }



  /**
   * Retrieves the path to the default JAAS configuration file that will be used
   * if no file was explicitly provided.  A new file may be created if
   * necessary.
   *
   * @param  properties  The GSSAPI properties that should be used for
   *                     authentication.
   *
   * @return  The path to the default JAAS configuration file that will be used
   *          if no file was explicitly provided.
   *
   * @throws  LDAPException  If an error occurs while attempting to create the
   *                         configuration file.
   */
  private static String getConfigFilePath(
                             final GSSAPIBindRequestProperties properties)
          throws LDAPException
  {
    try
    {
      final File f =
           File.createTempFile("GSSAPIBindRequest-JAAS-Config-", ".conf");
      f.deleteOnExit();
      final PrintWriter w = new PrintWriter(new FileWriter(f));
      try
      {
        w.println(JAAS_CLIENT_NAME + " {");
        w.println("  com.sun.security.auth.module.Krb5LoginModule required");
        w.println("  client=true");

        if (properties.useTicketCache())
        {
          w.println("  useTicketCache=true");
          w.println("  renewTGT=" + properties.renewTGT());
          w.println("  doNotPrompt=" + properties.requireCachedCredentials());

          final String ticketCachePath = properties.getTicketCachePath();
          if (ticketCachePath != null)
          {
            w.println("  ticketCache=\"" + ticketCachePath + '"');
          }
        }
        else
        {
          w.println("  useTicketCache=false");
        }

        if (properties.enableGSSAPIDebugging())
        {
          w.println(" debug=true");
        }

        w.println("  ;");
        w.println("};");
      }
      finally
      {
        w.close();
      }

      return f.getAbsolutePath();
    }
    catch (Exception e)
    {
      debugException(e);

      throw new LDAPException(ResultCode.LOCAL_ERROR,
           ERR_GSSAPI_CANNOT_CREATE_JAAS_CONFIG.get(getExceptionMessage(e)), e);
    }
  }



  /**
   * Sends this bind request to the target server over the provided connection
   * and returns the corresponding response.
   *
   * @param  connection  The connection to use to send this bind request to the
   *                     server and read the associated response.
   * @param  depth       The current referral depth for this request.  It should
   *                     always be one for the initial request, and should only
   *                     be incremented when following referrals.
   *
   * @return  The bind response read from the server.
   *
   * @throws  LDAPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  @Override()
  protected BindResult process(final LDAPConnection connection, final int depth)
            throws LDAPException
  {
    if (! conn.compareAndSet(null, connection))
    {
      throw new LDAPException(ResultCode.LOCAL_ERROR,
                     ERR_GSSAPI_MULTIPLE_CONCURRENT_REQUESTS.get());
    }

    System.setProperty(PROPERTY_CONFIG_FILE, configFilePath);
    System.setProperty(PROPERTY_SUBJECT_CREDS_ONLY, "true");

    if (kdcAddress != null)
    {
      System.setProperty(PROPERTY_KDC_ADDRESS, kdcAddress);
    }

    if (realm != null)
    {
      System.setProperty(PROPERTY_REALM, realm);
    }

    try
    {
      final LoginContext context;
      try
      {
        context = new LoginContext(JAAS_CLIENT_NAME, this);
        context.login();
      }
      catch (Exception e)
      {
        debugException(e);

        throw new LDAPException(ResultCode.LOCAL_ERROR,
                       ERR_GSSAPI_CANNOT_INITIALIZE_JAAS_CONTEXT.get(
                            getExceptionMessage(e)), e);
      }

      try
      {
        return (BindResult) Subject.doAs(context.getSubject(), this);
      }
      catch (Exception e)
      {
        debugException(e);
        if (e instanceof LDAPException)
        {
          throw (LDAPException) e;
        }
        else
        {
          throw new LDAPException(ResultCode.LOCAL_ERROR,
                         ERR_GSSAPI_AUTHENTICATION_FAILED.get(
                              getExceptionMessage(e)), e);
        }
      }
    }
    finally
    {
      conn.set(null);
    }
  }



  /**
   * Perform the privileged portion of the authentication processing.
   *
   * @return  {@code null}, since no return value is actually needed.
   *
   * @throws  LDAPException  If a problem occurs during processing.
   */
  @InternalUseOnly()
  public Object run()
         throws LDAPException
  {
    final LDAPConnection connection = conn.get();

    final String[] mechanisms = { GSSAPI_MECHANISM_NAME };

    final HashMap<String,Object> saslProperties = new HashMap<String,Object>(2);
    saslProperties.put(Sasl.QOP, "auth");
    saslProperties.put(Sasl.SERVER_AUTH, "true");

    final SaslClient saslClient;
    try
    {
      saslClient = Sasl.createSaslClient(mechanisms, authorizationID,
           servicePrincipalProtocol, connection.getConnectedAddress(),
           saslProperties, this);
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.LOCAL_ERROR,
           ERR_GSSAPI_CANNOT_CREATE_SASL_CLIENT.get(getExceptionMessage(e)), e);
    }

    final SASLHelper helper = new SASLHelper(this, connection,
         GSSAPI_MECHANISM_NAME, saslClient, getControls(),
         getResponseTimeoutMillis(connection));

    try
    {
      return helper.processSASLBind();
    }
    finally
    {
      messageID = helper.getMessageID();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public GSSAPIBindRequest getRebindRequest(final String host, final int port)
  {
    try
    {
      final GSSAPIBindRequestProperties gssapiProperties =
           new GSSAPIBindRequestProperties(authenticationID, authorizationID,
                password, realm, kdcAddress, configFilePath);
      gssapiProperties.setServicePrincipalProtocol(servicePrincipalProtocol);
      gssapiProperties.setUseTicketCache(useTicketCache);
      gssapiProperties.setRequireCachedCredentials(requireCachedCredentials);
      gssapiProperties.setRenewTGT(renewTGT);
      gssapiProperties.setTicketCachePath(ticketCachePath);
      gssapiProperties.setEnableGSSAPIDebugging(enableGSSAPIDebugging);

      return new GSSAPIBindRequest(gssapiProperties, getControls());
    }
    catch (Exception e)
    {
      // This should never happen.
      debugException(e);
      return null;
    }
  }



  /**
   * Handles any necessary callbacks required for SASL authentication.
   *
   * @param  callbacks  The set of callbacks to be handled.
   */
  @InternalUseOnly()
  public void handle(final Callback[] callbacks)
  {
    for (final Callback callback : callbacks)
    {
      if (callback instanceof NameCallback)
      {
        ((NameCallback) callback).setName(authenticationID);
      }
      else if (callback instanceof PasswordCallback)
      {
        ((PasswordCallback) callback).setPassword(
             password.stringValue().toCharArray());
      }
      else if (callback instanceof RealmCallback)
      {
        if (realm != null)
        {
          ((RealmCallback) callback).setText(realm);
        }
      }
      else
      {
        // This is an unexpected callback.
        if (debugEnabled(DebugType.LDAP))
        {
          debug(Level.WARNING, DebugType.LDAP,
                "Unexpected GSSAPI SASL callback of type " +
                callback.getClass().getName());
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public int getLastMessageID()
  {
    return messageID;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public GSSAPIBindRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public GSSAPIBindRequest duplicate(final Control[] controls)
  {
    try
    {
      final GSSAPIBindRequestProperties gssapiProperties =
           new GSSAPIBindRequestProperties(authenticationID, authorizationID,
                password, realm, kdcAddress, configFilePath);
      gssapiProperties.setServicePrincipalProtocol(servicePrincipalProtocol);
      gssapiProperties.setUseTicketCache(useTicketCache);
      gssapiProperties.setRequireCachedCredentials(requireCachedCredentials);
      gssapiProperties.setRenewTGT(renewTGT);
      gssapiProperties.setTicketCachePath(ticketCachePath);
      gssapiProperties.setEnableGSSAPIDebugging(enableGSSAPIDebugging);

      final GSSAPIBindRequest bindRequest =
           new GSSAPIBindRequest(gssapiProperties, controls);
      bindRequest.setResponseTimeoutMillis(getResponseTimeoutMillis(null));
      return bindRequest;
    }
    catch (Exception e)
    {
      // This should never happen.
      debugException(e);
      return null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("GSSAPIBindRequest(authenticationID='");
    buffer.append(authenticationID);
    buffer.append('\'');

    if (authorizationID != null)
    {
      buffer.append(", authorizationID='");
      buffer.append(authorizationID);
      buffer.append('\'');
    }

    if (realm != null)
    {
      buffer.append(", realm='");
      buffer.append(realm);
      buffer.append('\'');
    }

    if (kdcAddress != null)
    {
      buffer.append(", kdcAddress='");
      buffer.append(kdcAddress);
      buffer.append('\'');
    }

    buffer.append(", configFilePath='");
    buffer.append(configFilePath);
    buffer.append("', servicePrincipalProtocol='");
    buffer.append(servicePrincipalProtocol);
    buffer.append("', enableGSSAPIDebugging=");
    buffer.append(enableGSSAPIDebugging);

    final Control[] controls = getControls();
    if (controls.length > 0)
    {
      buffer.append(", controls={");
      for (int i=0; i < controls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(controls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}
