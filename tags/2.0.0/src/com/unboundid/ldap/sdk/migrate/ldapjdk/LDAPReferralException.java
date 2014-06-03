/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2010 UnboundID Corp.
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
package com.unboundid.ldap.sdk.migrate.ldapjdk;



import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides an exception that may be returned if a referral is
 * returned in response for an operation.
 * <BR><BR>
 * This class is primarily intended to be used in the process of updating
 * applications which use the Netscape Directory SDK for Java to switch to or
 * coexist with the UnboundID LDAP SDK for Java.  For applications not written
 * using the Netscape Directory SDK for Java, the
 * {@link com.unboundid.ldap.sdk.LDAPException} class should be used instead.
 */
@NotExtensible()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public class LDAPReferralException
       extends LDAPException
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 7867903105944011998L;



  // The referral URLs for this exception.
  private final String[] referralURLs;



  /**
   * Creates a new LDAP referral exception with no information.
   */
  public LDAPReferralException()
  {
    super(null, REFERRAL);

    referralURLs = new String[0];
  }



  /**
   * Creates a new LDAP referral exception with the provided information.
   *
   * @param  message             The message for this LDAP referral exception.
   * @param  resultCode          The result code for this LDAP referral
   *                             exception.
   * @param  serverErrorMessage  The error message returned from the server.
   */
  public LDAPReferralException(final String message, final int resultCode,
                               final String serverErrorMessage)
  {
    super(message, resultCode, serverErrorMessage, null);

    referralURLs = new String[0];
  }



  /**
   * Creates a new LDAP referral exception with the provided information.
   *
   * @param  message     The message for this LDAP referral exception.
   * @param  resultCode  The result code for this LDAP referral exception.
   * @param  referrals   The set of referrals for this exception.
   */
  public LDAPReferralException(final String message, final int resultCode,
                               final String[] referrals)
  {
    super(message, resultCode, null, null);

    referralURLs = referrals;
  }



  /**
   * Creates a new LDAP referral exception from the provided
   * {@link com.unboundid.ldap.sdk.LDAPException} object.
   *
   * @param  ldapException  The {@code LDAPException} object to use for this
   *                        LDAP interrupted exception.
   */
  public LDAPReferralException(
              final com.unboundid.ldap.sdk.LDAPException ldapException)
  {
    super(ldapException);

    referralURLs = ldapException.getReferralURLs();
  }



  /**
   * Creates a new LDAP referral exception from the provided
   * {@link SearchResultReference} object.
   *
   * @param  reference  The {@code SearchResultReference} object to use to
   *                    create this exception.
   */
  public LDAPReferralException(final SearchResultReference reference)
  {
    super(null, REFERRAL);

    referralURLs = reference.getReferralURLs();
  }



  /**
   * Retrieves the set of referral URLs for this exception.
   *
   * @return  The set of referral URLs for this exception.
   */
  public String[] getURLs()
  {
    return referralURLs;
  }
}