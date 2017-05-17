/*
 * Copyright 2009-2017 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2017 Ping Identity Corporation
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
package com.unboundid.ldap.protocol;



import java.io.ByteArrayInputStream;

import org.testng.annotations.Test;

import com.unboundid.asn1.ASN1Buffer;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1StreamReader;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSDKTestCase;



/**
 * This class provides a set of test cases for the
 * {@code CompareRequestProtocolOp} class.
 */
public class CompareRequestProtocolOpTestCase
       extends LDAPSDKTestCase
{
  /**
   * Provides test coverage for the compare request protocol op.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testCompareRequestProtocolOp()
         throws Exception
  {
    CompareRequestProtocolOp op = new CompareRequestProtocolOp(
         "dc=example,dc=com", "dc", new ASN1OctetString("example"));
    ASN1Buffer buffer = new ASN1Buffer();
    op.writeTo(buffer);

    byte[] opBytes = buffer.toByteArray();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(opBytes);
    ASN1StreamReader reader = new ASN1StreamReader(inputStream);

    op = new CompareRequestProtocolOp(reader);

    op = CompareRequestProtocolOp.decodeProtocolOp(op.encodeProtocolOp());

    op = new CompareRequestProtocolOp(op.toCompareRequest());

    assertEquals(new DN(op.getDN()),
                 new DN("dc=example,dc=com"));

    assertNotNull(op.getAttributeName());
    assertEquals(op.getAttributeName(), "dc");

    assertNotNull(op.getAssertionValue());
    assertEquals(op.getAssertionValue().stringValue(), "example");

    assertEquals(op.getProtocolOpType(), (byte) 0x6E);

    assertNotNull(op.toString());
  }



  /**
   * Tests the behavior when trying to read a malformed compare request.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(expectedExceptions = { LDAPException.class })
  public void testReadMalformedRequest()
         throws Exception
  {
    byte[] requestBytes = { (byte) 0x6E, 0x00 };
    ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBytes);
    ASN1StreamReader reader = new ASN1StreamReader(inputStream);
    new CompareRequestProtocolOp(reader);
  }



  /**
   * Tests the behavior when trying to decode a malformed compare request.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(expectedExceptions = { LDAPException.class })
  public void testDecodeMalformedRequest()
         throws Exception
  {
    CompareRequestProtocolOp.decodeProtocolOp(
         new ASN1Element(LDAPMessage.PROTOCOL_OP_TYPE_COMPARE_REQUEST));
  }
}