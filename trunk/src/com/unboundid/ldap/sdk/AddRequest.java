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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.unboundid.asn1.ASN1Buffer;
import com.unboundid.asn1.ASN1BufferSequence;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.protocol.LDAPMessage;
import com.unboundid.ldap.protocol.LDAPResponse;
import com.unboundid.ldap.protocol.ProtocolOp;
import com.unboundid.ldif.LDIFAddChangeRecord;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.InternalUseOnly;
import com.unboundid.util.Mutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.LDAPMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class implements the processing necessary to perform an LDAPv3 add
 * operation, which creates a new entry in the directory.  An add request
 * contains the DN for the entry and the set of attributes to include.  It may
 * also include a set of controls to send to the server.
 * <BR><BR>
 * The contents of the entry to may be specified as a separate DN and collection
 * of attributes, as an {@link Entry} object, or as a list of the lines that
 * comprise the LDIF representation of the entry to add as described in
 * <A HREF="http://www.ietf.org/rfc/rfc2849.txt">RFC 2849</A>.  For example, the
 * following code demonstrates creating an add request from the LDIF
 * representation of the entry:
 * <PRE>
 *   AddRequest addRequest = new AddRequest(
 *     "dn: dc=example,dc=com",
 *     "objectClass: top",
 *     "objectClass: domain",
 *     "dc: example");
 * </PRE>
 * <BR><BR>
 * {@code AddRequest} objects are mutable and therefore can be altered and
 * re-used for multiple requests.  Note, however, that {@code AddRequest}
 * objects are not threadsafe and therefore a single {@code AddRequest} object
 * instance should not be used to process multiple requests at the same time.
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class AddRequest
       extends UpdatableLDAPRequest
       implements ReadOnlyAddRequest, ResponseAcceptor, ProtocolOp
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 1320730292848237219L;



  // The queue that will be used to receive response messages from the server.
  private final LinkedBlockingQueue<LDAPResponse> responseQueue =
       new LinkedBlockingQueue<LDAPResponse>();

  // The set of attributes to include in the entry to add.
  private ArrayList<Attribute> attributes;

  // The message ID from the last LDAP message sent from this request.
  private int messageID = -1;

  // The DN of the entry to be added.
  private String dn;



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   */
  public AddRequest(final String dn, final Attribute... attributes)
  {
    super(null);

    ensureNotNull(dn, attributes);

    this.dn = dn;

    this.attributes = new ArrayList<Attribute>(attributes.length);
    this.attributes.addAll(Arrays.asList(attributes));
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   * @param  controls    The set of controls to include in the request.
   */
  public AddRequest(final String dn, final Attribute[] attributes,
                    final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, attributes);

    this.dn = dn;

    this.attributes = new ArrayList<Attribute>(attributes.length);
    this.attributes.addAll(Arrays.asList(attributes));
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   */
  public AddRequest(final String dn, final Collection<Attribute> attributes)
  {
    super(null);

    ensureNotNull(dn, attributes);

    this.dn         = dn;
    this.attributes = new ArrayList<Attribute>(attributes);
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   * @param  controls    The set of controls to include in the request.
   */
  public AddRequest(final String dn, final Collection<Attribute> attributes,
                    final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, attributes);

    this.dn         = dn;
    this.attributes = new ArrayList<Attribute>(attributes);
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   */
  public AddRequest(final DN dn, final Attribute... attributes)
  {
    super(null);

    ensureNotNull(dn, attributes);

    this.dn = dn.toString();

    this.attributes = new ArrayList<Attribute>(attributes.length);
    this.attributes.addAll(Arrays.asList(attributes));
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   * @param  controls    The set of controls to include in the request.
   */
  public AddRequest(final DN dn, final Attribute[] attributes,
                    final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, attributes);

    this.dn = dn.toString();

    this.attributes = new ArrayList<Attribute>(attributes.length);
    this.attributes.addAll(Arrays.asList(attributes));
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   */
  public AddRequest(final DN dn, final Collection<Attribute> attributes)
  {
    super(null);

    ensureNotNull(dn, attributes);

    this.dn         = dn.toString();
    this.attributes = new ArrayList<Attribute>(attributes);
  }



  /**
   * Creates a new add request with the provided DN and set of attributes.
   *
   * @param  dn          The DN for the entry to add.  It must not be
   *                     {@code null}.
   * @param  attributes  The set of attributes to include in the entry to add.
   *                     It must not be {@code null}.
   * @param  controls    The set of controls to include in the request.
   */
  public AddRequest(final DN dn, final Collection<Attribute> attributes,
                    final Control[] controls)
  {
    super(controls);

    ensureNotNull(dn, attributes);

    this.dn         = dn.toString();
    this.attributes = new ArrayList<Attribute>(attributes);
  }



  /**
   * Creates a new add request to add the provided entry.
   *
   * @param  entry  The entry to be added.  It must not be {@code null}.
   */
  public AddRequest(final Entry entry)
  {
    super(null);

    ensureNotNull(entry);

    dn         = entry.getDN();
    attributes = new ArrayList<Attribute>(entry.getAttributes());
  }



  /**
   * Creates a new add request to add the provided entry.
   *
   * @param  entry     The entry to be added.  It must not be {@code null}.
   * @param  controls  The set of controls to include in the request.
   */
  public AddRequest(final Entry entry, final Control[] controls)
  {
    super(controls);

    ensureNotNull(entry);

    dn         = entry.getDN();
    attributes = new ArrayList<Attribute>(entry.getAttributes());
  }



  /**
   * Creates a new add request with the provided entry in LDIF form.
   *
   * @param  ldifLines  The lines that comprise the LDIF representation of the
   *                    entry to add.  It must not be {@code null} or empty.
   *
   * @throws  LDIFException  If the provided LDIF data cannot be decoded as an
   *                         entry.
   */
  public AddRequest(final String... ldifLines)
         throws LDIFException
  {
    this(LDIFReader.decodeEntry(ldifLines));
  }



  /**
   * {@inheritDoc}
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Specifies the DN for this add request.
   *
   * @param  dn  The DN for this add request.  It must not be {@code null}.
   */
  public void setDN(final String dn)
  {
    ensureNotNull(dn);

    this.dn = dn;
  }



  /**
   * Specifies the DN for this add request.
   *
   * @param  dn  The DN for this add request.  It must not be {@code null}.
   */
  public void setDN(final DN dn)
  {
    ensureNotNull(dn);

    this.dn = dn.toString();
  }



  /**
   * {@inheritDoc}
   */
  public List<Attribute> getAttributes()
  {
    return Collections.unmodifiableList(attributes);
  }



  /**
   * Specifies the set of attributes for this add request.  It must not be
   * {@code null}.
   *
   * @param  attributes  The set of attributes for this add request.
   */
  public void setAttributes(final Attribute[] attributes)
  {
    ensureNotNull(attributes);

    this.attributes.clear();
    this.attributes.addAll(Arrays.asList(attributes));
  }



  /**
   * Specifies the set of attributes for this add request.  It must not be
   * {@code null}.
   *
   * @param  attributes  The set of attributes for this add request.
   */
  public void setAttributes(final Collection<Attribute> attributes)
  {
    ensureNotNull(attributes);

    this.attributes.clear();
    this.attributes.addAll(attributes);
  }



  /**
   * Adds the provided attribute to the entry to add.
   *
   * @param  attribute  The attribute to be added to the entry to add.  It must
   *                    not be {@code null}.
   */
  public void addAttribute(final Attribute attribute)
  {
    ensureNotNull(attribute);

    for (int i=0 ; i < attributes.size(); i++)
    {
      final Attribute a = attributes.get(i);
      if (a.getName().equalsIgnoreCase(attribute.getName()))
      {
        attributes.set(i, Attribute.mergeAttributes(a, attribute));
        return;
      }
    }

    attributes.add(attribute);
  }



  /**
   * Adds the provided attribute to the entry to add.
   *
   * @param  name   The name of the attribute to add.  It must not be
   *                {@code null}.
   * @param  value  The value for the attribute to add.  It must not be
   *                {@code null}.
   */
  public void addAttribute(final String name, final String value)
  {
    ensureNotNull(name, value);
    addAttribute(new Attribute(name, value));
  }



  /**
   * Adds the provided attribute to the entry to add.
   *
   * @param  name   The name of the attribute to add.  It must not be
   *                {@code null}.
   * @param  value  The value for the attribute to add.  It must not be
   *                {@code null}.
   */
  public void addAttribute(final String name, final byte[] value)
  {
    ensureNotNull(name, value);
    addAttribute(new Attribute(name, value));
  }



  /**
   * Adds the provided attribute to the entry to add.
   *
   * @param  name    The name of the attribute to add.  It must not be
   *                 {@code null}.
   * @param  values  The set of values for the attribute to add.  It must not be
   *                 {@code null}.
   */
  public void addAttribute(final String name, final String... values)
  {
    ensureNotNull(name, values);
    addAttribute(new Attribute(name, values));
  }



  /**
   * Adds the provided attribute to the entry to add.
   *
   * @param  name    The name of the attribute to add.  It must not be
   *                 {@code null}.
   * @param  values  The set of values for the attribute to add.  It must not be
   *                 {@code null}.
   */
  public void addAttribute(final String name, final byte[]... values)
  {
    ensureNotNull(name, values);
    addAttribute(new Attribute(name, values));
  }



  /**
   * Removes the attribute with the specified name from the entry to add.
   *
   * @param  attributeName  The name of the attribute to remove.  It must not be
   *                        {@code null}.
   *
   * @return  {@code true} if the attribute was removed from this add request,
   *          or {@code false} if the add request did not include the specified
   *          attribute.
   */
  public boolean removeAttribute(final String attributeName)
  {
    ensureNotNull(attributeName);

    final Iterator<Attribute> iterator = attributes.iterator();
    while (iterator.hasNext())
    {
      final Attribute a = iterator.next();
      if (a.getName().equalsIgnoreCase(attributeName))
      {
        iterator.remove();
        return true;
      }
    }

    return false;
  }



  /**
   * Removes the specified attribute value from this add request.
   *
   * @param  name   The name of the attribute to remove.  It must not be
   *                {@code null}.
   * @param  value  The value of the attribute to remove.  It must not be
   *                {@code null}.
   *
   * @return  {@code true} if the attribute value was removed from this add
   *          request, or {@code false} if the add request did not include the
   *          specified attribute value.
   */
  public boolean removeAttributeValue(final String name, final String value)
  {
    ensureNotNull(name, value);

    int pos = -1;
    for (int i=0; i < attributes.size(); i++)
    {
      final Attribute a = attributes.get(i);
      if (a.getName().equalsIgnoreCase(name))
      {
        pos = i;
        break;
      }
    }

    if (pos < 0)
    {
      return false;
    }

    final Attribute a = attributes.get(pos);
    final Attribute newAttr =
         Attribute.removeValues(a, new Attribute(name, value));

    if (a.getRawValues().length == newAttr.getRawValues().length)
    {
      return false;
    }

    if (newAttr.getRawValues().length == 0)
    {
      attributes.remove(pos);
    }
    else
    {
      attributes.set(pos, newAttr);
    }

    return true;
  }



  /**
   * Removes the specified attribute value from this add request.
   *
   * @param  name   The name of the attribute to remove.  It must not be
   *                {@code null}.
   * @param  value  The value of the attribute to remove.  It must not be
   *                {@code null}.
   *
   * @return  {@code true} if the attribute value was removed from this add
   *          request, or {@code false} if the add request did not include the
   *          specified attribute value.
   */
  public boolean removeAttribute(final String name, final byte[] value)
  {
    ensureNotNull(name, value);

    int pos = -1;
    for (int i=0; i < attributes.size(); i++)
    {
      final Attribute a = attributes.get(i);
      if (a.getName().equalsIgnoreCase(name))
      {
        pos = i;
        break;
      }
    }

    if (pos < 0)
    {
      return false;
    }

    final Attribute a = attributes.get(pos);
    final Attribute newAttr =
         Attribute.removeValues(a, new Attribute(name, value));

    if (a.getRawValues().length == newAttr.getRawValues().length)
    {
      return false;
    }

    if (newAttr.getRawValues().length == 0)
    {
      attributes.remove(pos);
    }
    else
    {
      attributes.set(pos, newAttr);
    }

    return true;
  }



  /**
   * Replaces the specified attribute in the entry to add.  If no attribute with
   * the given name exists in the add request, it will be added.
   *
   * @param  attribute  The attribute to be replaced in this add request.  It
   *                    must not be {@code null}.
   */
  public void replaceAttribute(final Attribute attribute)
  {
    ensureNotNull(attribute);

    for (int i=0; i < attributes.size(); i++)
    {
      if (attributes.get(i).getName().equalsIgnoreCase(attribute.getName()))
      {
        attributes.set(i, attribute);
        return;
      }
    }

    attributes.add(attribute);
  }



  /**
   * Replaces the specified attribute in the entry to add.  If no attribute with
   * the given name exists in the add request, it will be added.
   *
   * @param  name   The name of the attribute to be replaced.  It must not be
   *                {@code null}.
   * @param  value  The new value for the attribute.  It must not be
   *                {@code null}.
   */
  public void replaceAttribute(final String name, final String value)
  {
    ensureNotNull(name, value);

    for (int i=0; i < attributes.size(); i++)
    {
      if (attributes.get(i).getName().equalsIgnoreCase(name))
      {
        attributes.set(i, new Attribute(name, value));
        return;
      }
    }

    attributes.add(new Attribute(name, value));
  }



  /**
   * Replaces the specified attribute in the entry to add.  If no attribute with
   * the given name exists in the add request, it will be added.
   *
   * @param  name   The name of the attribute to be replaced.  It must not be
   *                {@code null}.
   * @param  value  The new value for the attribute.  It must not be
   *                {@code null}.
   */
  public void replaceAttribute(final String name, final byte[] value)
  {
    ensureNotNull(name, value);

    for (int i=0; i < attributes.size(); i++)
    {
      if (attributes.get(i).getName().equalsIgnoreCase(name))
      {
        attributes.set(i, new Attribute(name, value));
        return;
      }
    }

    attributes.add(new Attribute(name, value));
  }



  /**
   * Replaces the specified attribute in the entry to add.  If no attribute with
   * the given name exists in the add request, it will be added.
   *
   * @param  name    The name of the attribute to be replaced.  It must not be
   *                 {@code null}.
   * @param  values  The new set of values for the attribute.  It must not be
   *                 {@code null}.
   */
  public void replaceAttribute(final String name, final String... values)
  {
    ensureNotNull(name, values);

    for (int i=0; i < attributes.size(); i++)
    {
      if (attributes.get(i).getName().equalsIgnoreCase(name))
      {
        attributes.set(i, new Attribute(name, values));
        return;
      }
    }

    attributes.add(new Attribute(name, values));
  }



  /**
   * Replaces the specified attribute in the entry to add.  If no attribute with
   * the given name exists in the add request, it will be added.
   *
   * @param  name    The name of the attribute to be replaced.  It must not be
   *                 {@code null}.
   * @param  values  The new set of values for the attribute.  It must not be
   *                 {@code null}.
   */
  public void replaceAttribute(final String name, final byte[]... values)
  {
    ensureNotNull(name, values);

    for (int i=0; i < attributes.size(); i++)
    {
      if (attributes.get(i).getName().equalsIgnoreCase(name))
      {
        attributes.set(i, new Attribute(name, values));
        return;
      }
    }

    attributes.add(new Attribute(name, values));
  }



  /**
   * {@inheritDoc}
   */
  public byte getProtocolOpType()
  {
    return LDAPMessage.PROTOCOL_OP_TYPE_ADD_REQUEST;
  }



  /**
   * {@inheritDoc}
   */
  public void writeTo(final ASN1Buffer buffer)
  {
    final ASN1BufferSequence requestSequence =
         buffer.beginSequence(LDAPMessage.PROTOCOL_OP_TYPE_ADD_REQUEST);
    buffer.addOctetString(dn);

    final ASN1BufferSequence attrSequence = buffer.beginSequence();
    for (final Attribute a : attributes)
    {
      a.writeTo(buffer);
    }
    attrSequence.end();

    requestSequence.end();
  }



  /**
   * Encodes the add request protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element with the encoded add request protocol op.
   */
  ASN1Element encodeProtocolOp()
  {
    // Create the add request protocol op.
    final ASN1Element[] attrElements = new ASN1Element[attributes.size()];
    for (int i=0; i < attrElements.length; i++)
    {
      attrElements[i] = attributes.get(i).encode();
    }

    final ASN1Element[] addRequestElements =
    {
      new ASN1OctetString(dn),
      new ASN1Sequence(attrElements)
    };

    return new ASN1Sequence(LDAPMessage.PROTOCOL_OP_TYPE_ADD_REQUEST,
                            addRequestElements);
  }



  /**
   * Sends this add request to the directory server over the provided connection
   * and returns the associated response.
   *
   * @param  connection  The connection to use to communicate with the directory
   *                     server.
   * @param  depth       The current referral depth for this request.  It should
   *                     always be one for the initial request, and should only
   *                     be incremented when following referrals.
   *
   * @return  An LDAP result object that provides information about the result
   *          of the add processing.
   *
   * @throws  LDAPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  @Override()
  protected LDAPResult process(final LDAPConnection connection, final int depth)
            throws LDAPException
  {
    if (connection.synchronousMode())
    {
      return processSync(connection, depth);
    }

    final long requestTime = System.nanoTime();
    processAsync(connection, null);

    try
    {
      // Wait for and process the response.
      final LDAPResponse response;
      try
      {
        final long responseTimeout = getResponseTimeoutMillis(connection);
        if (responseTimeout > 0)
        {
          response = responseQueue.poll(responseTimeout, TimeUnit.MILLISECONDS);
        }
        else
        {
          response = responseQueue.take();
        }
      }
      catch (InterruptedException ie)
      {
        debugException(ie);
        throw new LDAPException(ResultCode.LOCAL_ERROR,
             ERR_ADD_INTERRUPTED.get(connection.getHostPort()), ie);
      }

      return handleResponse(connection, response, requestTime, depth);
    }
    finally
    {
      connection.deregisterResponseAcceptor(messageID);
    }
  }



  /**
   * Sends this add request to the directory server over the provided connection
   * and returns the message ID for the request.
   *
   * @param  connection      The connection to use to communicate with the
   *                         directory server.
   * @param  resultListener  The async result listener that is to be notified
   *                         when the response is received.  It may be
   *                         {@code null} only if the result is to be processed
   *                         by this class.
   *
   * @return  The LDAP message ID for the add request that was sent to the
   *          server.
   *
   * @throws  LDAPException  If a problem occurs while sending the request.
   */
  int processAsync(final LDAPConnection connection,
                   final AsyncResultListener resultListener)
      throws LDAPException
  {
    // Create the LDAP message.
    messageID = connection.nextMessageID();
    final LDAPMessage message =
         new LDAPMessage(messageID,  this, getControls());


    // If the provided async result listener is {@code null}, then we'll use
    // this class as the message acceptor.  Otherwise, create an async helper
    // and use it as the message acceptor.
    if (resultListener == null)
    {
      connection.registerResponseAcceptor(messageID, this);
    }
    else
    {
      final AsyncHelper helper = new AsyncHelper(connection,
           LDAPMessage.PROTOCOL_OP_TYPE_ADD_RESPONSE, resultListener,
           getIntermediateResponseListener());
      connection.registerResponseAcceptor(messageID, helper);
    }


    // Send the request to the server.
    try
    {
      debugLDAPRequest(this);
      connection.getConnectionStatistics().incrementNumAddRequests();
      connection.sendMessage(message);
      return messageID;
    }
    catch (LDAPException le)
    {
      debugException(le);

      connection.deregisterResponseAcceptor(messageID);
      throw le;
    }
  }



  /**
   * Processes this add operation in synchronous mode, in which the same thread
   * will send the request and read the response.
   *
   * @param  connection  The connection to use to communicate with the directory
   *                     server.
   * @param  depth       The current referral depth for this request.  It should
   *                     always be one for the initial request, and should only
   *                     be incremented when following referrals.
   *
   * @return  An LDAP result object that provides information about the result
   *          of the add processing.
   *
   * @throws  LDAPException  If a problem occurs while sending the request or
   *                         reading the response.
   */
  private LDAPResult processSync(final LDAPConnection connection,
                                 final int depth)
          throws LDAPException
  {
    // Create the LDAP message.
    messageID = connection.nextMessageID();
    final LDAPMessage message =
         new LDAPMessage(messageID,  this, getControls());


    // Set the appropriate timeout on the socket.
    try
    {
      connection.getConnectionInternals().getSocket().setSoTimeout(
           (int) getResponseTimeoutMillis(connection));
    }
    catch (Exception e)
    {
      debugException(e);
    }


    // Send the request to the server.
    final long requestTime = System.nanoTime();
    debugLDAPRequest(this);
    connection.getConnectionStatistics().incrementNumAddRequests();
    connection.sendMessage(message);

    final LDAPResponse response = connection.readResponse(messageID);
    return handleResponse(connection, response, requestTime, depth);
  }



  /**
   * Performs the necessary processing for handling a response.
   *
   * @param  connection   The connection used to read the response.
   * @param  response     The response to be processed.
   * @param  requestTime  The time the request was sent to the server.
   * @param  depth        The current referral depth for this request.  It
   *                      should always be one for the initial request, and
   *                      should only be incremented when following referrals.
   *
   * @return  The add result.
   *
   * @throws  LDAPException  If a problem occurs.
   */
  private LDAPResult handleResponse(final LDAPConnection connection,
                                    final LDAPResponse response,
                                    final long requestTime, final int depth)
          throws LDAPException
  {
    if (response == null)
    {
      final long waitTime = nanosToMillis(System.nanoTime() - requestTime);
      throw new LDAPException(ResultCode.TIMEOUT,
           ERR_ADD_CLIENT_TIMEOUT.get(waitTime, connection.getHostPort()));
    }

    connection.getConnectionStatistics().incrementNumAddResponses(
         System.nanoTime() - requestTime);

    if (response instanceof ConnectionClosedResponse)
    {
      // The connection was closed while waiting for the response.
      final ConnectionClosedResponse ccr = (ConnectionClosedResponse) response;
      final String message = ccr.getMessage();
      if (message == null)
      {
        throw new LDAPException(ResultCode.SERVER_DOWN,
             ERR_CONN_CLOSED_WAITING_FOR_ADD_RESPONSE.get(
                  connection.getHostPort(), toString()));
      }
      else
      {
        throw new LDAPException(ResultCode.SERVER_DOWN,
             ERR_CONN_CLOSED_WAITING_FOR_ADD_RESPONSE_WITH_MESSAGE.get(
                  connection.getHostPort(), toString(), message));
      }
    }

    final LDAPResult result = (LDAPResult) response;
    if ((result.getResultCode().equals(ResultCode.REFERRAL)) &&
        followReferrals(connection))
    {
      if (depth >= connection.getConnectionOptions().getReferralHopLimit())
      {
        return new LDAPResult(messageID, ResultCode.REFERRAL_LIMIT_EXCEEDED,
                              ERR_TOO_MANY_REFERRALS.get(),
                              result.getMatchedDN(),
                              result.getReferralURLs(),
                              result.getResponseControls());
      }

      return followReferral(result, connection, depth);
    }
    else
    {
      return result;
    }
  }



  /**
   * Attempts to follow a referral to perform an add operation in the target
   * server.
   *
   * @param  referralResult  The LDAP result object containing information about
   *                         the referral to follow.
   * @param  connection      The connection on which the referral was received.
   * @param  depth           The number of referrals followed in the course of
   *                         processing this request.
   *
   * @return  The result of attempting to process the add operation by following
   *          the referral.
   *
   * @throws  LDAPException  If a problem occurs while attempting to establish
   *                         the referral connection, sending the request, or
   *                         reading the result.
   */
  private LDAPResult followReferral(final LDAPResult referralResult,
                                    final LDAPConnection connection,
                                    final int depth)
          throws LDAPException
  {
    for (final String urlString : referralResult.getReferralURLs())
    {
      try
      {
        final LDAPURL referralURL = new LDAPURL(urlString);
        final String host = referralURL.getHost();

        if (host == null)
        {
          // We can't handle a referral in which there is no host.
          continue;
        }

        final AddRequest addRequest;
        if (referralURL.baseDNProvided())
        {
          addRequest = new AddRequest(referralURL.getBaseDN(), attributes,
                                      getControls());
        }
        else
        {
          addRequest = this;
        }

        final LDAPConnection referralConn =
             connection.getReferralConnection(referralURL, connection);
        try
        {
          return addRequest.process(referralConn, (depth+1));
        }
        finally
        {
          referralConn.setDisconnectInfo(DisconnectType.REFERRAL, null, null);
          referralConn.close();
        }
      }
      catch (LDAPException le)
      {
        debugException(le);
      }
    }

    // If we've gotten here, then we could not follow any of the referral URLs,
    // so we'll just return the original referral result.
    return referralResult;
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
  public AddRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  public AddRequest duplicate(final Control[] controls)
  {
    final ArrayList<Attribute> attrs = new ArrayList<Attribute>(attributes);
    final AddRequest r = new AddRequest(dn, attrs, controls);

    if (followReferralsInternal() != null)
    {
      r.setFollowReferrals(followReferralsInternal());
    }

    r.setResponseTimeoutMillis(getResponseTimeoutMillis(null));

    return r;
  }



  /**
   * {@inheritDoc}
   */
  @InternalUseOnly()
  public void responseReceived(final LDAPResponse response)
         throws LDAPException
  {
    try
    {
      responseQueue.put(response);
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.LOCAL_ERROR,
           ERR_EXCEPTION_HANDLING_RESPONSE.get(getExceptionMessage(e)), e);
    }
  }



  /**
   * {@inheritDoc}
   */
  public LDIFAddChangeRecord toLDIFChangeRecord()
  {
    return new LDIFAddChangeRecord(this);
  }



  /**
   * {@inheritDoc}
   */
  public String[] toLDIF()
  {
    return toLDIFChangeRecord().toLDIF();
  }



  /**
   * {@inheritDoc}
   */
  public String toLDIFString()
  {
    return toLDIFChangeRecord().toLDIFString();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("AddRequest(dn='");
    buffer.append(dn);
    buffer.append("', attrs={");

    for (int i=0; i < attributes.size(); i++)
    {
      if (i > 0)
      {
        buffer.append(", ");
      }

      buffer.append(attributes.get(i));
    }
    buffer.append('}');

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
