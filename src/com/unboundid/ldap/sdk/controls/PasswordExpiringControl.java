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
package com.unboundid.ldap.sdk.controls;



import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DecodeableControl;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.controls.ControlMessages.*;
import static com.unboundid.util.Debug.*;



/**
 * This class provides an implementation of the expiring expiring control as
 * described in draft-vchu-ldap-pwd-policy.  It may be used to indicate that the
 * authenticated user's password will expire in the near future.  The value of
 * this control includes the length of time in seconds until the user's
 * password actually expires.
 * <BR><BR>
 * No request control is required to trigger the server to send the password
 * expiring response control.  If the server supports the use of this control
 * and the user's password will expire within a time frame that the server
 * considers to be the near future, then it will be included in the bind
 * response returned to the client.
 * <BR><BR>
 * See the documentation for the {@link PasswordExpiredControl} to see an
 * example that demonstrates the use of both the password expiring and password
 * expired controls.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class PasswordExpiringControl
       extends Control
       implements DecodeableControl
{
  /**
   * The OID (2.16.840.1.113730.3.4.5) for the password expiring response
   * control.
   */
  public static final String PASSWORD_EXPIRING_OID = "2.16.840.1.113730.3.4.5";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 1250220480854441338L;



  // The length of time in seconds until the password expires.
  private final int secondsUntilExpiration;



  /**
   * Creates a new empty control instance that is intended to be used only for
   * decoding controls via the {@code DecodeableControl} interface.
   */
  PasswordExpiringControl()
  {
    secondsUntilExpiration = -1;
  }



  /**
   * Creates a new password expiring control with the provided information.
   *
   * @param  secondsUntilExpiration  The length of time in seconds until the
   *                                 password expires.
   */
  public PasswordExpiringControl(final int secondsUntilExpiration)
  {
    super(PASSWORD_EXPIRING_OID, false,
          new ASN1OctetString(String.valueOf(secondsUntilExpiration)));

    this.secondsUntilExpiration = secondsUntilExpiration;
  }



  /**
   * Creates a new password expiring control with the provided information.
   *
   * @param  oid         The OID for the control.
   * @param  isCritical  Indicates whether the control should be marked
   *                     critical.
   * @param  value       The encoded value for the control.  This may be
   *                     {@code null} if no value was provided.
   *
   * @throws  LDAPException  If the provided control cannot be decoded as a
   *                         password expiring response control.
   */
  public PasswordExpiringControl(final String oid, final boolean isCritical,
                                 final ASN1OctetString value)
         throws LDAPException
  {
    super(oid, isCritical, value);

    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_PW_EXPIRING_NO_VALUE.get());
    }

    try
    {
      secondsUntilExpiration = Integer.parseInt(value.stringValue());
    }
    catch (NumberFormatException nfe)
    {
      debugException(nfe);
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_PW_EXPIRING_VALUE_NOT_INTEGER.get(), nfe);
    }
  }



  /**
   * {@inheritDoc}
   */
  public PasswordExpiringControl
              decodeControl(final String oid, final boolean isCritical,
                            final ASN1OctetString value)
         throws LDAPException
  {
    return new PasswordExpiringControl(oid, isCritical, value);
  }



  /**
   * Retrieves the length of time in seconds until the password expires.
   *
   * @return  The length of time in seconds until the password expires.
   */
  public int getSecondsUntilExpiration()
  {
    return secondsUntilExpiration;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getControlName()
  {
    return INFO_CONTROL_NAME_PW_EXPIRING.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("PasswordExpiringControl(secondsUntilExpiration=");
    buffer.append(secondsUntilExpiration);
    buffer.append(", isCritical=");
    buffer.append(isCritical());
    buffer.append(')');
  }
}
