/*
 * Copyright 2008-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2010 UnboundID Corp.
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
package com.unboundid.ldap.sdk.extensions;



import java.util.ArrayList;

import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.ExtendedRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.extensions.ExtOpMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides an implementation of the LDAP password modify extended
 * request as defined in
 * <A HREF="http://www.ietf.org/rfc/rfc3062.txt">RFC 3062</A>.  It may be used
 * to change the password for a user in the directory, and provides the ability
 * to specify the current password for verification.  It also offers the ability
 * to request that the server generate a new password for the user.
 * <BR><BR>
 * The elements of a password modify extended request include:
 * <UL>
 *   <LI>{@code userIdentity} -- This specifies the user for which to change the
 *       password.  It should generally be the DN for the target user (although
 *       the specification does indicate that some servers may accept other
 *       values).  If no value is provided, then the server will attempt to
 *       change the password for the currently-authenticated user.</LI>
 *   <LI>{@code oldPassword} -- This specifies the current password for the
 *       user.  Some servers may require that the old password be provided when
 *       a user is changing his or her own password as an extra level of
 *       verification, but it is generally not necessary when an administrator
 *       is resetting the password for another user.</LI>
 *   <LI>{@code newPassword} -- This specifies the new password to use for the
 *       user.  If it is not provided, then the server may attempt to generate a
 *       new password for the user, and in that case it will be included in the
 *       {@code generatedPassword} field of the corresponding
 *       {@link PasswordModifyExtendedResult}.  Note that some servers may not
 *       support generating a new password, in which case the client will always
 *       be required to provide it.</LI>
 * </UL>
 * <H2>Example</H2>
 * The following example demonstrates the use of the password modify extended
 * operation to change the password for user
 * "uid=john.doe,ou=People,dc=example,dc=com".  Neither the current password nor
 * a new password will be provided, so the server will generate a new password
 * for the user.
 * <PRE>
 *   PasswordModifyExtendedRequest passwordModifyRequest =
 *        new PasswordModifyExtendedRequest(
 *                 "uid=john.doe,ou=People,dc=example,dc=com", null, null);
 *   PasswordModifyExtendedResult passwordModifyResult =
 *        (PasswordModifyExtendedResult)
 *        connection.processExtendedOperation(passwordModifyRequest);
 *
 *   // NOTE:  The processExtendedOperation method will only throw an exception
 *   // if a problem occurs while trying to send the request or read the
 *   // response.  It will not throw an exception because of a non-success
 *   // response.
 *
 *   if (passwordModifyResult.getResultCode() == ResultCode.SUCCESS)
 *   {
 *     System.out.println("The password change was successful.");
 *     System.out.println("The new password for the user is " +
 *          passwordModifyResult.getGeneratedPassword());
 *   }
 *   else
 *   {
 *     System.err.println("An error occurred while attempting to process " +
 *                        "the password modify extended request.");
 *   }
 * </PRE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class PasswordModifyExtendedRequest
       extends ExtendedRequest
{
  /**
   * The OID for the password modify extended request.
   */
  public static final String PASSWORD_MODIFY_REQUEST_OID =
       "1.3.6.1.4.1.4203.1.11.1";



  /**
   * The BER type for the user identity element.
   */
  private static final byte TYPE_USER_IDENTITY = (byte) 0x80;



  /**
   * The BER type for the old password element.
   */
  private static final byte TYPE_OLD_PASSWORD = (byte) 0x81;



  /**
   * The BER type for the new password element.
   */
  private static final byte TYPE_NEW_PASSWORD = (byte) 0x82;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 4965048727456933570L;



  // The old password for this request.
  private final ASN1OctetString oldPassword;

  // The new password for this request.
  private final ASN1OctetString newPassword;

  // The user identity string for this request.
  private final String userIdentity;



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password of the currently-authenticated user.
   *
   * @param  newPassword  The new password for the user.  It may be {@code null}
   *                      if the new password should be generated by the
   *                      directory server.
   */
  public PasswordModifyExtendedRequest(final String newPassword)
  {
    this(null, null, newPassword, null);
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password of the currently-authenticated user.
   *
   * @param  newPassword  The new password for the user.  It may be {@code null}
   *                      if the new password should be generated by the
   *                      directory server.
   */
  public PasswordModifyExtendedRequest(final byte[] newPassword)
  {
    this(null, null, newPassword, null);
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password of the currently-authenticated user.
   *
   * @param  oldPassword  The current password for the user.  It may be
   *                      {@code null} if the directory server does not require
   *                      the user's current password for self changes.
   * @param  newPassword  The new password for the user.  It may be {@code null}
   *                      if the new password should be generated by the
   *                      directory server.
   */
  public PasswordModifyExtendedRequest(final String oldPassword,
                                       final String newPassword)
  {
    this(null, oldPassword, newPassword, null);
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password of the currently-authenticated user.
   *
   * @param  oldPassword  The current password for the user.  It may be
   *                      {@code null} if the directory server does not require
   *                      the user's current password for self changes.
   * @param  newPassword  The new password for the user.  It may be {@code null}
   *                      if the new password should be generated by the
   *                      directory server.
   */
  public PasswordModifyExtendedRequest(final byte[] oldPassword,
                                       final byte[] newPassword)
  {
    this(null, oldPassword, newPassword, null);
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password for the specified user.
   *
   * @param  userIdentity  The string that identifies the user whose password
   *                       should be changed.  It may or may not be a DN, but if
   *                       it is not a DN, then the directory server must be
   *                       able to identify the appropriate user from the
   *                       provided identifier.  It may be {@code null} to
   *                       indicate that the password change should be for the
   *                       currently-authenticated user.
   * @param  oldPassword   The current password for the user.  It may be
   *                       {@code null} if the directory server does not require
   *                       the user's current password for self changes.
   * @param  newPassword   The new password for the user.  It may be
   *                       {@code null} if the new password should be generated
   *                       by the directory server.
   */
  public PasswordModifyExtendedRequest(final String userIdentity,
                                       final String oldPassword,
                                       final String newPassword)
  {
    this(userIdentity, oldPassword, newPassword, null);
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password for the specified user.
   *
   * @param  userIdentity  The string that identifies the user whose password
   *                       should be changed.  It may or may not be a DN, but if
   *                       it is not a DN, then the directory server must be
   *                       able to identify the appropriate user from the
   *                       provided identifier.  It may be {@code null} to
   *                       indicate that the password change should be for the
   *                       currently-authenticated user.
   * @param  oldPassword   The current password for the user.  It may be
   *                       {@code null} if the directory server does not require
   *                       the user's current password for self changes.
   * @param  newPassword   The new password for the user.  It may be
   *                       {@code null} if the new password should be generated
   *                       by the directory server.
   */
  public PasswordModifyExtendedRequest(final String userIdentity,
                                       final byte[] oldPassword,
                                       final byte[] newPassword)
  {
    this(userIdentity, oldPassword, newPassword, null);
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password for the specified user.
   *
   * @param  userIdentity  The string that identifies the user whose password
   *                       should be changed.  It may or may not be a DN, but if
   *                       it is not a DN, then the directory server must be
   *                       able to identify the appropriate user from the
   *                       provided identifier.  It may be {@code null} to
   *                       indicate that the password change should be for the
   *                       currently-authenticated user.
   * @param  oldPassword   The current password for the user.  It may be
   *                       {@code null} if the directory server does not require
   *                       the user's current password for self changes.
   * @param  newPassword   The new password for the user.  It may be
   *                       {@code null} if the new password should be generated
   *                       by the directory server.
   * @param  controls      The set of controls to include in the request.
   */
  public PasswordModifyExtendedRequest(final String userIdentity,
                                       final String oldPassword,
                                       final String newPassword,
                                       final Control[] controls)
  {
    super(PASSWORD_MODIFY_REQUEST_OID,
          encodeValue(userIdentity, oldPassword, newPassword), controls);

    this.userIdentity = userIdentity;

    if (oldPassword == null)
    {
      this.oldPassword = null;
    }
    else
    {
      this.oldPassword = new ASN1OctetString(TYPE_OLD_PASSWORD, oldPassword);
    }

    if (newPassword == null)
    {
      this.newPassword = null;
    }
    else
    {
      this.newPassword = new ASN1OctetString(TYPE_NEW_PASSWORD, newPassword);
    }
  }



  /**
   * Creates a new password modify extended request that will attempt to change
   * the password for the specified user.
   *
   * @param  userIdentity  The string that identifies the user whose password
   *                       should be changed.  It may or may not be a DN, but if
   *                       it is not a DN, then the directory server must be
   *                       able to identify the appropriate user from the
   *                       provided identifier.  It may be {@code null} to
   *                       indicate that the password change should be for the
   *                       currently-authenticated user.
   * @param  oldPassword   The current password for the user.  It may be
   *                       {@code null} if the directory server does not require
   *                       the user's current password for self changes.
   * @param  newPassword   The new password for the user.  It may be
   *                       {@code null} if the new password should be generated
   *                       by the directory server.
   * @param  controls      The set of controls to include in the request.
   */
  public PasswordModifyExtendedRequest(final String userIdentity,
                                       final byte[] oldPassword,
                                       final byte[] newPassword,
                                       final Control[] controls)
  {
    super(PASSWORD_MODIFY_REQUEST_OID,
          encodeValue(userIdentity, oldPassword, newPassword), controls);

    this.userIdentity = userIdentity;

    if (oldPassword == null)
    {
      this.oldPassword = null;
    }
    else
    {
      this.oldPassword = new ASN1OctetString(TYPE_OLD_PASSWORD, oldPassword);
    }

    if (newPassword == null)
    {
      this.newPassword = null;
    }
    else
    {
      this.newPassword = new ASN1OctetString(TYPE_NEW_PASSWORD, newPassword);
    }
  }



  /**
   * Creates a new password modify extended request from the provided generic
   * extended request.
   *
   * @param  extendedRequest  The generic extended request to use to create this
   *                          password modify extended request.
   *
   * @throws  LDAPException  If a problem occurs while decoding the request.
   */
  public PasswordModifyExtendedRequest(final ExtendedRequest extendedRequest)
         throws LDAPException
  {
    super(extendedRequest);

    final ASN1OctetString value = extendedRequest.getValue();
    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_PW_MODIFY_REQUEST_NO_VALUE.get());
    }

    try
    {
      ASN1OctetString oldPW  = null;
      ASN1OctetString newPW  = null;
      String          userID = null;

      final ASN1Element valueElement = ASN1Element.decode(value.getValue());
      final ASN1Element[] elements =
           ASN1Sequence.decodeAsSequence(valueElement).elements();
      for (final ASN1Element e : elements)
      {
        switch (e.getType())
        {
          case TYPE_USER_IDENTITY:
            userID = ASN1OctetString.decodeAsOctetString(e).stringValue();
            break;

          case TYPE_OLD_PASSWORD:
            oldPW = ASN1OctetString.decodeAsOctetString(e);
            break;

          case TYPE_NEW_PASSWORD:
            newPW = ASN1OctetString.decodeAsOctetString(e);
            break;

          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                                    ERR_PW_MODIFY_REQUEST_INVALID_TYPE.get(
                                         toHex(e.getType())));
        }
      }

      userIdentity = userID;
      oldPassword  = oldPW;
      newPassword  = newPW;
    }
    catch (LDAPException le)
    {
      debugException(le);
      throw le;
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_PW_MODIFY_REQUEST_CANNOT_DECODE.get(e), e);
    }
  }



  /**
   * Encodes the provided information into an ASN.1 octet string suitable for
   * use as the value of this extended request.
   *
   * @param  userIdentity  The string that identifies the user whose password
   *                       should be changed.  It may or may not be a DN, but if
   *                       it is not a DN, then the directory server must be
   *                       able to identify the appropriate user from the
   *                       provided identifier.  It may be {@code null} to
   *                       indicate that the password change should be for the
   *                       currently-authenticated user.
   * @param  oldPassword   The current password for the user.  It may be
   *                       {@code null} if the directory server does not require
   *                       the user's current password for self changes.
   * @param  newPassword   The new password for the user.  It may be
   *                       {@code null} if the new password should be generated
   *                       by the directory server.
   *
   * @return  The ASN.1 octet string containing the encoded value.
   */
  private static ASN1OctetString encodeValue(final String userIdentity,
                                             final String oldPassword,
                                             final String newPassword)
  {
    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(3);

    if (userIdentity != null)
    {
      elements.add(new ASN1OctetString(TYPE_USER_IDENTITY, userIdentity));
    }

    if (oldPassword != null)
    {
      elements.add(new ASN1OctetString(TYPE_OLD_PASSWORD, oldPassword));
    }

    if (newPassword != null)
    {
      elements.add(new ASN1OctetString(TYPE_NEW_PASSWORD, newPassword));
    }

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Encodes the provided information into an ASN.1 octet string suitable for
   * use as the value of this extended request.
   *
   * @param  userIdentity  The string that identifies the user whose password
   *                       should be changed.  It may or may not be a DN, but if
   *                       it is not a DN, then the directory server must be
   *                       able to identify the appropriate user from the
   *                       provided identifier.  It may be {@code null} to
   *                       indicate that the password change should be for the
   *                       currently-authenticated user.
   * @param  oldPassword   The current password for the user.  It may be
   *                       {@code null} if the directory server does not require
   *                       the user's current password for self changes.
   * @param  newPassword   The new password for the user.  It may be
   *                       {@code null} if the new password should be generated
   *                       by the directory server.
   *
   * @return  The ASN.1 octet string containing the encoded value.
   */
  private static ASN1OctetString encodeValue(final String userIdentity,
                                             final byte[] oldPassword,
                                             final byte[] newPassword)
  {
    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(3);

    if (userIdentity != null)
    {
      elements.add(new ASN1OctetString(TYPE_USER_IDENTITY, userIdentity));
    }

    if (oldPassword != null)
    {
      elements.add(new ASN1OctetString(TYPE_OLD_PASSWORD, oldPassword));
    }

    if (newPassword != null)
    {
      elements.add(new ASN1OctetString(TYPE_NEW_PASSWORD, newPassword));
    }

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Retrieves the user identity for this request, if available.
   *
   * @return  The user identity for this request, or {@code null} if the
   *          password change should target the currently-authenticated user.
   */
  public String getUserIdentity()
  {
    return userIdentity;
  }



  /**
   * Retrieves the string representation of the old password for this request,
   * if available.
   *
   * @return  The string representation of the old password for this request, or
   *          {@code null} if it was not provided.
   */
  public String getOldPassword()
  {
    if (oldPassword == null)
    {
      return null;
    }
    else
    {
      return oldPassword.stringValue();
    }
  }



  /**
   * Retrieves the binary representation of the old password for this request,
   * if available.
   *
   * @return  The binary representation of the old password for this request, or
   *          {@code null} if it was not provided.
   */
  public byte[] getOldPasswordBytes()
  {
    if (oldPassword == null)
    {
      return null;
    }
    else
    {
      return oldPassword.getValue();
    }
  }



  /**
   * Retrieves the raw old password for this request, if available.
   *
   * @return  The raw old password for this request, or {@code null} if it was
   *          not provided.
   */
  public ASN1OctetString getRawOldPassword()
  {
    return oldPassword;
  }



  /**
   * Retrieves the string representation of the new password for this request,
   * if available.
   *
   * @return  The string representation of the new password for this request, or
   *          {@code null} if it was not provided.
   */
  public String getNewPassword()
  {
    if (newPassword == null)
    {
      return null;
    }
    else
    {
      return newPassword.stringValue();
    }
  }



  /**
   * Retrieves the binary representation of the new password for this request,
   * if available.
   *
   * @return  The binary representation of the new password for this request, or
   *          {@code null} if it was not provided.
   */
  public byte[] getNewPasswordBytes()
  {
    if (newPassword == null)
    {
      return null;
    }
    else
    {
      return newPassword.getValue();
    }
  }



  /**
   * Retrieves the raw new password for this request, if available.
   *
   * @return  The raw new password for this request, or {@code null} if it was
   *          not provided.
   */
  public ASN1OctetString getRawNewPassword()
  {
    return newPassword;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PasswordModifyExtendedResult process(final LDAPConnection connection,
                                              final int depth)
         throws LDAPException
  {
    final ExtendedResult extendedResponse = super.process(connection, depth);
    return new PasswordModifyExtendedResult(extendedResponse);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PasswordModifyExtendedRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PasswordModifyExtendedRequest duplicate(final Control[] controls)
  {
    final byte[] oldPWBytes =
         (oldPassword == null) ? null : oldPassword.getValue();
    final byte[] newPWBytes =
         (newPassword == null) ? null : newPassword.getValue();

    final PasswordModifyExtendedRequest r =
         new PasswordModifyExtendedRequest(userIdentity, oldPWBytes,
              newPWBytes, controls);
    r.setResponseTimeoutMillis(getResponseTimeoutMillis(null));
    return r;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getExtendedRequestName()
  {
    return INFO_EXTENDED_REQUEST_NAME_PASSWORD_MODIFY.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("PasswordModifyExtendedRequest(");

    boolean dataAdded = false;

    if (userIdentity != null)
    {
      buffer.append("userIdentity='");
      buffer.append(userIdentity);
      buffer.append('\'');
      dataAdded = true;
    }

    if (oldPassword != null)
    {
      if (dataAdded)
      {
        buffer.append(", ");
      }

      buffer.append("oldPassword='");
      buffer.append(oldPassword.stringValue());
      buffer.append('\'');
      dataAdded = true;
    }

    if (newPassword != null)
    {
      if (dataAdded)
      {
        buffer.append(", ");
      }

      buffer.append("newPassword='");
      buffer.append(newPassword.stringValue());
      buffer.append('\'');
      dataAdded = true;
    }

    final Control[] controls = getControls();
    if (controls.length > 0)
    {
      if (dataAdded)
      {
        buffer.append(", ");
      }

      buffer.append("controls={");
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