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
package com.unboundid.ldap.sdk.unboundidds.logs;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.testng.annotations.Test;

import com.unboundid.ldap.sdk.LDAPSDKTestCase;



/**
 * This class provides test coverage for the
 * {@code BindForwardAccessLogMessage} class.
 */
public class BindForwardAccessLogMessageTestCase
       extends LDAPSDKTestCase
{
  /**
   * Tests the ability to create a log message from a string containing only a
   * timestamp.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testOnlyTimestamp()
         throws Exception
  {
    // Get a timestamp that doesn't include milliseconds.
    Date d = new Date();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(GregorianCalendar.MILLISECOND, 0);
    d = c.getTime();

    SimpleDateFormat f = new SimpleDateFormat("'['dd/MMM/yyyy:HH:mm:ss Z']'");
    String s = f.format(d);

    BindForwardAccessLogMessage m = new BindForwardAccessLogMessage(s);
    m = new BindForwardAccessLogMessage(m);

    assertNotNull(m);

    assertNotNull(m.getTimestamp());
    assertEquals(m.getTimestamp(), d);

    assertNotNull(m.getNamedValues());
    assertTrue(m.getNamedValues().isEmpty());

    assertNotNull(m.getUnnamedValues());
    assertTrue(m.getUnnamedValues().isEmpty());

    assertNotNull(m.getMessageType());
    assertEquals(m.getMessageType(), AccessLogMessageType.FORWARD);

    assertNotNull(m.getOperationType());
    assertEquals(m.getOperationType(), AccessLogOperationType.BIND);

    assertNull(m.getProductName());

    assertNull(m.getInstanceName());

    assertNull(m.getStartupID());

    assertNull(m.getConnectionID());

    assertNull(m.getOperationID());

    assertNull(m.getMessageID());

    assertNull(m.getOrigin());

    assertNull(m.getRequesterDN());

    assertNull(m.getRequesterIPAddress());

    assertNull(m.getIntermediateClientRequest());

    assertNull(m.getOperationPurpose());

    assertNull(m.getProtocolVersion());

    assertNull(m.getDN());

    assertNull(m.getAuthenticationType());

    assertNull(m.getSASLMechanismName());

    assertNull(m.getTargetHost());

    assertNull(m.getTargetPort());

    assertNull(m.getTargetProtocol());

    assertNotNull(m.toString());
    assertEquals(m.toString(), s);
  }



  /**
   * Tests the ability to create a log message from a string containing only a
   * basic set of information.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testBasicContents()
         throws Exception
  {
    // Get a timestamp that doesn't include milliseconds.
    Date d = new Date();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(GregorianCalendar.MILLISECOND, 0);
    d = c.getTime();

    SimpleDateFormat f = new SimpleDateFormat("'['dd/MMM/yyyy:HH:mm:ss Z']'");
    String s = f.format(d) + " BIND FORWARD conn=1 op=2 msgID=3 " +
               "targetHost=\"5.6.7.8\" targetPort=389 targetProtocol=\"LDAP\"";

    BindForwardAccessLogMessage m = new BindForwardAccessLogMessage(s);
    m = new BindForwardAccessLogMessage(m);

    assertNotNull(m);

    assertNotNull(m.getTimestamp());
    assertEquals(m.getTimestamp(), d);

    assertNotNull(m.getNamedValues());
    assertFalse(m.getNamedValues().isEmpty());

    assertNotNull(m.getUnnamedValues());
    assertFalse(m.getUnnamedValues().isEmpty());

    assertNotNull(m.getMessageType());
    assertEquals(m.getMessageType(), AccessLogMessageType.FORWARD);

    assertNotNull(m.getOperationType());
    assertEquals(m.getOperationType(), AccessLogOperationType.BIND);

    assertNull(m.getProductName());

    assertNull(m.getInstanceName());

    assertNull(m.getStartupID());

    assertNotNull(m.getConnectionID());
    assertEquals(m.getConnectionID(), Long.valueOf(1));

    assertNotNull(m.getOperationID());
    assertEquals(m.getOperationID(), Long.valueOf(2));

    assertNotNull(m.getMessageID());
    assertEquals(m.getMessageID(), Integer.valueOf(3));

    assertNull(m.getOrigin());

    assertNull(m.getRequesterDN());

    assertNull(m.getRequesterIPAddress());

    assertNull(m.getIntermediateClientRequest());

    assertNull(m.getOperationPurpose());

    assertNull(m.getProtocolVersion());

    assertNull(m.getDN());

    assertNull(m.getAuthenticationType());

    assertNull(m.getSASLMechanismName());

    assertNotNull(m.getTargetHost());
    assertEquals(m.getTargetHost(), "5.6.7.8");

    assertNotNull(m.getTargetPort());
    assertEquals(m.getTargetPort(), Integer.valueOf(389));

    assertNotNull(m.getTargetProtocol());
    assertEquals(m.getTargetProtocol(), "LDAP");

    assertNotNull(m.toString());
    assertEquals(m.toString(), s);
  }



  /**
   * Tests the ability to create a log message from a string containing a
   * complete set of information for a simple bind.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testCompleteContentsSimple()
         throws Exception
  {
    // Get a timestamp that doesn't include milliseconds.
    Date d = new Date();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(GregorianCalendar.MILLISECOND, 0);
    d = c.getTime();

    SimpleDateFormat f = new SimpleDateFormat("'['dd/MMM/yyyy:HH:mm:ss Z']'");
    String s = f.format(d) + " BIND FORWARD " +
               "product=\"Directory Server\" " +
               "instanceName=\"server.example.com:389\" " +
               "startupID=\"ABCDEFG\" conn=1 op=2 msgID=3 " +
               "origin=\"internal\" requesterIP=\"1.2.3.4\" " +
               "via=\"app='UnboundID Directory Proxy Server'\" version=3 " +
               "opPurpose=\"app='Some Client' purpose='foo'\" " +
               "dn=\"uid=test.user,ou=People,dc=example,dc=com\" " +
               "authType=\"SIMPLE\" targetHost=\"5.6.7.8\" targetPort=389 " +
               "targetProtocol=\"LDAP\"";

    BindForwardAccessLogMessage m = new BindForwardAccessLogMessage(s);
    m = new BindForwardAccessLogMessage(m);

    assertNotNull(m);

    assertNotNull(m.getTimestamp());
    assertEquals(m.getTimestamp(), d);

    assertNotNull(m.getNamedValues());
    assertFalse(m.getNamedValues().isEmpty());

    assertNotNull(m.getUnnamedValues());
    assertFalse(m.getUnnamedValues().isEmpty());

    assertNotNull(m.getMessageType());
    assertEquals(m.getMessageType(), AccessLogMessageType.FORWARD);

    assertNotNull(m.getOperationType());
    assertEquals(m.getOperationType(), AccessLogOperationType.BIND);

    assertNotNull(m.getProductName());
    assertEquals(m.getProductName(), "Directory Server");

    assertNotNull(m.getInstanceName());
    assertEquals(m.getInstanceName(), "server.example.com:389");

    assertNotNull(m.getStartupID());
    assertEquals(m.getStartupID(), "ABCDEFG");

    assertNotNull(m.getConnectionID());
    assertEquals(m.getConnectionID(), Long.valueOf(1));

    assertNotNull(m.getOperationID());
    assertEquals(m.getOperationID(), Long.valueOf(2));

    assertNotNull(m.getMessageID());
    assertEquals(m.getMessageID(), Integer.valueOf(3));

    assertNotNull(m.getOrigin());
    assertEquals(m.getOrigin(), "internal");

    assertNull(m.getRequesterDN());

    assertNotNull(m.getRequesterIPAddress());
    assertEquals(m.getRequesterIPAddress(), "1.2.3.4");

    assertNotNull(m.getIntermediateClientRequest());
    assertEquals(m.getIntermediateClientRequest(),
                 "app='UnboundID Directory Proxy Server'");

    assertNotNull(m.getOperationPurpose());
    assertEquals(m.getOperationPurpose(),
                 "app='Some Client' purpose='foo'");

    assertNotNull(m.getProtocolVersion());
    assertEquals(m.getProtocolVersion(), "3");

    assertNotNull(m.getDN());
    assertEquals(m.getDN(), "uid=test.user,ou=People,dc=example,dc=com");

    assertNotNull(m.getAuthenticationType());
    assertEquals(m.getAuthenticationType(),
                 BindRequestAuthenticationType.SIMPLE);

    assertNull(m.getSASLMechanismName());

    assertNotNull(m.getTargetHost());
    assertEquals(m.getTargetHost(), "5.6.7.8");

    assertNotNull(m.getTargetPort());
    assertEquals(m.getTargetPort(), Integer.valueOf(389));

    assertNotNull(m.getTargetProtocol());
    assertEquals(m.getTargetProtocol(), "LDAP");

    assertNotNull(m.toString());
    assertEquals(m.toString(), s);
  }



  /**
   * Tests the ability to create a log message from a string containing a
   * complete set of information for a SASL bind.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testCompleteContentsSASL()
         throws Exception
  {
    // Get a timestamp that doesn't include milliseconds.
    Date d = new Date();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(GregorianCalendar.MILLISECOND, 0);
    d = c.getTime();

    SimpleDateFormat f = new SimpleDateFormat("'['dd/MMM/yyyy:HH:mm:ss Z']'");
    String s = f.format(d) + " BIND FORWARD " +
               "product=\"Directory Server\" " +
               "instanceName=\"server.example.com:389\" " +
               "startupID=\"ABCDEFG\" conn=1 op=2 msgID=3 " +
               "origin=\"internal\" requesterIP=\"1.2.3.4\" " +
               "via=\"app='UnboundID Directory Proxy Server'\" version=3 " +
               "opPurpose=\"app='Some Client' purpose='foo'\" " +
               "dn=\"\" authType=\"SASL\" saslMechanism=\"EXTERNAL\" " +
               "targetHost=\"5.6.7.8\" targetPort=389 targetProtocol=\"LDAP\"";

    BindForwardAccessLogMessage m = new BindForwardAccessLogMessage(s);
    m = new BindForwardAccessLogMessage(m);

    assertNotNull(m);

    assertNotNull(m.getTimestamp());
    assertEquals(m.getTimestamp(), d);

    assertNotNull(m.getNamedValues());
    assertFalse(m.getNamedValues().isEmpty());

    assertNotNull(m.getUnnamedValues());
    assertFalse(m.getUnnamedValues().isEmpty());

    assertNotNull(m.getMessageType());
    assertEquals(m.getMessageType(), AccessLogMessageType.FORWARD);

    assertNotNull(m.getOperationType());
    assertEquals(m.getOperationType(), AccessLogOperationType.BIND);

    assertNotNull(m.getProductName());
    assertEquals(m.getProductName(), "Directory Server");

    assertNotNull(m.getInstanceName());
    assertEquals(m.getInstanceName(), "server.example.com:389");

    assertNotNull(m.getStartupID());
    assertEquals(m.getStartupID(), "ABCDEFG");

    assertNotNull(m.getConnectionID());
    assertEquals(m.getConnectionID(), Long.valueOf(1));

    assertNotNull(m.getOperationID());
    assertEquals(m.getOperationID(), Long.valueOf(2));

    assertNotNull(m.getMessageID());
    assertEquals(m.getMessageID(), Integer.valueOf(3));

    assertNotNull(m.getOrigin());
    assertEquals(m.getOrigin(), "internal");

    assertNull(m.getRequesterDN());

    assertNotNull(m.getRequesterIPAddress());
    assertEquals(m.getRequesterIPAddress(), "1.2.3.4");

    assertNotNull(m.getIntermediateClientRequest());
    assertEquals(m.getIntermediateClientRequest(),
                 "app='UnboundID Directory Proxy Server'");

    assertNotNull(m.getOperationPurpose());
    assertEquals(m.getOperationPurpose(),
                 "app='Some Client' purpose='foo'");

    assertNotNull(m.getProtocolVersion());
    assertEquals(m.getProtocolVersion(), "3");

    assertNotNull(m.getDN());
    assertEquals(m.getDN(), "");

    assertNotNull(m.getAuthenticationType());
    assertEquals(m.getAuthenticationType(),
                 BindRequestAuthenticationType.SASL);

    assertNotNull(m.getSASLMechanismName());
    assertEquals(m.getSASLMechanismName(), "EXTERNAL");

    assertNotNull(m.getTargetHost());
    assertEquals(m.getTargetHost(), "5.6.7.8");

    assertNotNull(m.getTargetPort());
    assertEquals(m.getTargetPort(), Integer.valueOf(389));

    assertNotNull(m.getTargetProtocol());
    assertEquals(m.getTargetProtocol(), "LDAP");

    assertNotNull(m.toString());
    assertEquals(m.toString(), s);
  }



  /**
   * Tests the ability to create a log message from a string containing a
   * complete set of information for an internal bind.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test()
  public void testCompleteContentsInternal()
         throws Exception
  {
    // Get a timestamp that doesn't include milliseconds.
    Date d = new Date();
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(GregorianCalendar.MILLISECOND, 0);
    d = c.getTime();

    SimpleDateFormat f = new SimpleDateFormat("'['dd/MMM/yyyy:HH:mm:ss Z']'");
    String s = f.format(d) + " BIND FORWARD " +
               "product=\"Directory Server\" " +
               "instanceName=\"server.example.com:389\" " +
               "startupID=\"ABCDEFG\" conn=1 op=2 msgID=3 " +
               "origin=\"internal\" requesterIP=\"1.2.3.4\" " +
               "via=\"app='UnboundID Directory Proxy Server'\" version=3 " +
               "opPurpose=\"app='Some Client' purpose='foo'\" " +
               "dn=\"uid=test.user,ou=People,dc=example,dc=com\" " +
               "authType=\"INTERNAL\" targetHost=\"5.6.7.8\" targetPort=389 " +
               "targetProtocol=\"LDAP\"";

    BindForwardAccessLogMessage m = new BindForwardAccessLogMessage(s);
    m = new BindForwardAccessLogMessage(m);

    assertNotNull(m);

    assertNotNull(m.getTimestamp());
    assertEquals(m.getTimestamp(), d);

    assertNotNull(m.getNamedValues());
    assertFalse(m.getNamedValues().isEmpty());

    assertNotNull(m.getUnnamedValues());
    assertFalse(m.getUnnamedValues().isEmpty());

    assertNotNull(m.getMessageType());
    assertEquals(m.getMessageType(), AccessLogMessageType.FORWARD);

    assertNotNull(m.getOperationType());
    assertEquals(m.getOperationType(), AccessLogOperationType.BIND);

    assertNotNull(m.getProductName());
    assertEquals(m.getProductName(), "Directory Server");

    assertNotNull(m.getInstanceName());
    assertEquals(m.getInstanceName(), "server.example.com:389");

    assertNotNull(m.getStartupID());
    assertEquals(m.getStartupID(), "ABCDEFG");

    assertNotNull(m.getConnectionID());
    assertEquals(m.getConnectionID(), Long.valueOf(1));

    assertNotNull(m.getOperationID());
    assertEquals(m.getOperationID(), Long.valueOf(2));

    assertNotNull(m.getMessageID());
    assertEquals(m.getMessageID(), Integer.valueOf(3));

    assertNotNull(m.getOrigin());
    assertEquals(m.getOrigin(), "internal");

    assertNull(m.getRequesterDN());

    assertNotNull(m.getRequesterIPAddress());
    assertEquals(m.getRequesterIPAddress(), "1.2.3.4");

    assertNotNull(m.getIntermediateClientRequest());
    assertEquals(m.getIntermediateClientRequest(),
                 "app='UnboundID Directory Proxy Server'");

    assertNotNull(m.getOperationPurpose());
    assertEquals(m.getOperationPurpose(),
                 "app='Some Client' purpose='foo'");

    assertNotNull(m.getProtocolVersion());
    assertEquals(m.getProtocolVersion(), "3");

    assertNotNull(m.getDN());
    assertEquals(m.getDN(), "uid=test.user,ou=People,dc=example,dc=com");

    assertNotNull(m.getAuthenticationType());
    assertEquals(m.getAuthenticationType(),
                 BindRequestAuthenticationType.INTERNAL);

    assertNull(m.getSASLMechanismName());

    assertNotNull(m.getTargetHost());
    assertEquals(m.getTargetHost(), "5.6.7.8");

    assertNotNull(m.getTargetPort());
    assertEquals(m.getTargetPort(), Integer.valueOf(389));

    assertNotNull(m.getTargetProtocol());
    assertEquals(m.getTargetProtocol(), "LDAP");

    assertNotNull(m.toString());
    assertEquals(m.toString(), s);
  }
}