/*
 * Copyright 2009 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009 UnboundID Corp.
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



import java.io.IOException;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.unboundid.asn1.ASN1Buffer;
import com.unboundid.asn1.ASN1BufferSequence;
import com.unboundid.asn1.ASN1StreamReader;
import com.unboundid.asn1.ASN1StreamReaderSequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.InternalSDKHelper;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.InternalUseOnly;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.protocol.ProtocolMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides a data structure that may be used to represent LDAP
 * protocol messages.  Each LDAP message contains a message ID, a protocol op,
 * and an optional set of controls.
 */
@InternalUseOnly()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class LDAPMessage
       implements Serializable
{
  /**
   * The BER type to use for the bind request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_BIND_REQUEST = 0x60;



  /**
   * The BER type to use for the bind response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_BIND_RESPONSE = 0x61;



  /**
   * The BER type to use for the unbind request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_UNBIND_REQUEST = 0x42;



  /**
   * The BER type to use for the search request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_SEARCH_REQUEST = 0x63;



  /**
   * The BER type to use for the search result entry protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_SEARCH_RESULT_ENTRY = 0x64;



  /**
   * The BER type to use for the search result reference protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_SEARCH_RESULT_REFERENCE = 0x73;



  /**
   * The BER type to use for the search result done protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_SEARCH_RESULT_DONE = 0x65;



  /**
   * The BER type to use for the modify request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_MODIFY_REQUEST = 0x66;



  /**
   * The BER type to use for the modify response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_MODIFY_RESPONSE = 0x67;



  /**
   * The BER type to use for the add request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_ADD_REQUEST = 0x68;



  /**
   * The BER type to use for the add response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_ADD_RESPONSE = 0x69;



  /**
   * The BER type to use for the delete request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_DELETE_REQUEST = 0x4A;



  /**
   * The BER type to use for the delete response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_DELETE_RESPONSE = 0x6B;



  /**
   * The BER type to use for the modify DN request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST = 0x6C;



  /**
   * The BER type to use for the modify DN response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_MODIFY_DN_RESPONSE = 0x6D;



  /**
   * The BER type to use for the compare request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_COMPARE_REQUEST = 0x6E;



  /**
   * The BER type to use for the compare response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_COMPARE_RESPONSE = 0x6F;



  /**
   * The BER type to use for the abandon request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_ABANDON_REQUEST = 0x50;



  /**
   * The BER type to use for the extended request protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_EXTENDED_REQUEST = 0x77;



  /**
   * The BER type to use for the extended response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_EXTENDED_RESPONSE = 0x78;



  /**
   * The BER type to use for the intermediate response protocol op.
   */
  public static final byte PROTOCOL_OP_TYPE_INTERMEDIATE_RESPONSE = 0x79;



  /**
   * The BER type to use for the set of controls.
   */
  public static final byte MESSAGE_TYPE_CONTROLS = (byte) 0xA0;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 909272448857832592L;



  // The message ID for this LDAP message.
  private final int messageID;

  // The protocol op for this LDAP message.
  private final ProtocolOp protocolOp;

  // The set of controls for this LDAP message.
  private final List<Control> controls;



  /**
   * Creates a new LDAP message with the provided information.
   *
   * @param  messageID   The message ID for this LDAP message.
   * @param  protocolOp  The protocol op for this LDAP message.  It must not be
   *                     {@code null}.
   * @param  controls    The set of controls for this LDAP message.  It may be
   *                     {@code null} or empty if no controls are required.
   */
  public LDAPMessage(final int messageID, final ProtocolOp protocolOp,
                     final Control... controls)
  {
    this.messageID  = messageID;
    this.protocolOp = protocolOp;

    if (controls == null)
    {
      this.controls = Collections.emptyList();
    }
    else
    {
      this.controls = Collections.unmodifiableList(Arrays.asList(controls));
    }
  }



  /**
   * Creates a new LDAP message with the provided information.
   *
   * @param  messageID   The message ID for this LDAP message.
   * @param  protocolOp  The protocol op for this LDAP message.  It must not be
   *                     {@code null}.
   * @param  controls    The set of controls for this LDAP message.  It may be
   *                     {@code null} or empty if no controls are required.
   */
  public LDAPMessage(final int messageID, final ProtocolOp protocolOp,
                     final List<Control> controls)
  {
    this.messageID  = messageID;
    this.protocolOp = protocolOp;

    if (controls == null)
    {
      this.controls = Collections.emptyList();
    }
    else
    {
      this.controls = Collections.unmodifiableList(controls);
    }
  }



  /**
   * Retrieves the message ID for this LDAP message.
   *
   * @return  The message ID for this LDAP message.
   */
  public int getMessageID()
  {
    return messageID;
  }



  /**
   * Retrieves the protocol op for this LDAP message.
   *
   * @return  The protocol op for this LDAP message.
   */
  public ProtocolOp getProtocolOp()
  {
    return protocolOp;
  }



  /**
   * Retrieves the BER type for the protocol op contained in this LDAP message.
   *
   * @return  The BER type for the protocol op contained in this LDAP message.
   */
  public byte getProtocolOpType()
  {
    return protocolOp.getProtocolOpType();
  }



  /**
   * Retrieves the abandon request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The abandon request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an abandon request protocol op.
   */
  public AbandonRequestProtocolOp getAbandonRequestProtocolOp()
         throws ClassCastException
  {
    return (AbandonRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the add request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The add request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an add request protocol op.
   */
  public AddRequestProtocolOp getAddRequestProtocolOp()
         throws ClassCastException
  {
    return (AddRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the add response protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The add response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an add response protocol op.
   */
  public AddResponseProtocolOp getAddResponseProtocolOp()
         throws ClassCastException
  {
    return (AddResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the bind request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The bind request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a bind request protocol op.
   */
  public BindRequestProtocolOp getBindRequestProtocolOp()
         throws ClassCastException
  {
    return (BindRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the bind response protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The bind response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a bind response protocol op.
   */
  public BindResponseProtocolOp getBindResponseProtocolOp()
         throws ClassCastException
  {
    return (BindResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the compare request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The compare request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a compare request protocol op.
   */
  public CompareRequestProtocolOp getCompareRequestProtocolOp()
         throws ClassCastException
  {
    return (CompareRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the compare response protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The compare response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a compare response protocol op.
   */
  public CompareResponseProtocolOp getCompareResponseProtocolOp()
         throws ClassCastException
  {
    return (CompareResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the delete request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The delete request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a delete request protocol op.
   */
  public DeleteRequestProtocolOp getDeleteRequestProtocolOp()
         throws ClassCastException
  {
    return (DeleteRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the delete response protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The delete response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a delete response protocol op.
   */
  public DeleteResponseProtocolOp getDeleteResponseProtocolOp()
         throws ClassCastException
  {
    return (DeleteResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the extended request protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The extended request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an extended request protocol op.
   */
  public ExtendedRequestProtocolOp getExtendedRequestProtocolOp()
         throws ClassCastException
  {
    return (ExtendedRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the extended response protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The extended response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an extended response protocol op.
   */
  public ExtendedResponseProtocolOp getExtendedResponseProtocolOp()
         throws ClassCastException
  {
    return (ExtendedResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the modify request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The modify request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a modify request protocol op.
   */
  public ModifyRequestProtocolOp getModifyRequestProtocolOp()
         throws ClassCastException
  {
    return (ModifyRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the modify response protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The modify response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a modify response protocol op.
   */
  public ModifyResponseProtocolOp getModifyResponseProtocolOp()
         throws ClassCastException
  {
    return (ModifyResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the modify DN request protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The modify DN request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a modify DN request protocol op.
   */
  public ModifyDNRequestProtocolOp getModifyDNRequestProtocolOp()
         throws ClassCastException
  {
    return (ModifyDNRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the modify DN response protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The modify DN response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a modify DN response protocol op.
   */
  public ModifyDNResponseProtocolOp getModifyDNResponseProtocolOp()
         throws ClassCastException
  {
    return (ModifyDNResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the search request protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The search request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a search request protocol op.
   */
  public SearchRequestProtocolOp getSearchRequestProtocolOp()
         throws ClassCastException
  {
    return (SearchRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the search result entry protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The search result entry protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a search result entry protocol op.
   */
  public SearchResultEntryProtocolOp getSearchResultEntryProtocolOp()
         throws ClassCastException
  {
    return (SearchResultEntryProtocolOp) protocolOp;
  }



  /**
   * Retrieves the search result reference protocol op from this LDAP message.
   * This may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The search result reference protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a search result reference protocol op.
   */
  public SearchResultReferenceProtocolOp getSearchResultReferenceProtocolOp()
         throws ClassCastException
  {
    return (SearchResultReferenceProtocolOp) protocolOp;
  }



  /**
   * Retrieves the search result done protocol op from this LDAP message.  This
   * may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The search result done protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not a search result done protocol op.
   */
  public SearchResultDoneProtocolOp getSearchResultDoneProtocolOp()
         throws ClassCastException
  {
    return (SearchResultDoneProtocolOp) protocolOp;
  }



  /**
   * Retrieves the unbind request protocol op from this LDAP message.  This may
   * only be used if this LDAP message was obtained using the {@link #readFrom}
   * method.
   *
   * @return  The unbind request protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an unbind request protocol op.
   */
  public UnbindRequestProtocolOp getUnbindRequestProtocolOp()
         throws ClassCastException
  {
    return (UnbindRequestProtocolOp) protocolOp;
  }



  /**
   * Retrieves the intermediate response protocol op from this LDAP message.
   * This may only be used if this LDAP message was obtained using the
   * {@link #readFrom} method.
   *
   * @return  The intermediate response protocol op from this LDAP message.
   *
   * @throws  ClassCastException  If the protocol op for this LDAP message is
   *                              not an intermediate response protocol op.
   */
  public IntermediateResponseProtocolOp getIntermediateResponseProtocolOp()
         throws ClassCastException
  {
    return (IntermediateResponseProtocolOp) protocolOp;
  }



  /**
   * Retrieves the set of controls for this LDAP message.
   *
   * @return  The set of controls for this LDAP message.
   */
  public List<Control> getControls()
  {
    return controls;
  }



  /**
   * Writes an encoded representation of this LDAP message to the provided ASN.1
   * buffer.
   *
   * @param  buffer  The ASN.1 buffer to which the encoded representation should
   *                 be written.
   */
  public void writeTo(final ASN1Buffer buffer)
  {
    final ASN1BufferSequence messageSequence = buffer.beginSequence();
    buffer.addInteger(messageID);
    protocolOp.writeTo(buffer);

    if (! controls.isEmpty())
    {
      final ASN1BufferSequence controlsSequence =
           buffer.beginSequence(MESSAGE_TYPE_CONTROLS);
      for (final Control c : controls)
      {
        c.writeTo(buffer);
      }
      controlsSequence.end();
    }
    messageSequence.end();
  }



  /**
   * Reads an LDAP message from the provided ASN.1 stream reader.
   *
   * @param  reader               The ASN.1 stream reader from which the LDAP
   *                              message should be read.
   * @param  ignoreSocketTimeout  Indicates whether to ignore socket timeout
   *                              exceptions caught during processing.  This
   *                              should be {@code true} when the associated
   *                              connection is operating in asynchronous mode,
   *                              and {@code false} when operating in
   *                              synchronous mode.  In either case, exceptions
   *                              will not be ignored for the first read, since
   *                              that will be handled by the connection reader.
   *
   * @return  The decoded LDAP message, or {@code null} if the end of the input
   *          stream has been reached..
   *
   * @throws  LDAPException  If an error occurs while attempting to read or
   *                         decode the LDAP message.
   */
  public static LDAPMessage readFrom(final ASN1StreamReader reader,
                                     final boolean ignoreSocketTimeout)
         throws LDAPException
  {
    final ASN1StreamReaderSequence messageSequence;
    try
    {
      reader.setIgnoreSocketTimeout(false);
      messageSequence = reader.beginSequence();
      if (messageSequence == null)
      {
        return null;
      }
    }
    catch (IOException ioe)
    {
      debugException(ioe);

      throw new LDAPException(ResultCode.SERVER_DOWN,
           ERR_MESSAGE_IO_ERROR.get(getExceptionMessage(ioe)), ioe);
    }
    catch (Exception e)
    {
      debugException(e);

      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MESSAGE_CANNOT_DECODE.get(getExceptionMessage(e)), e);
    }

    try
    {

      reader.setIgnoreSocketTimeout(ignoreSocketTimeout);
      final int messageID = reader.readInteger();

      final ProtocolOp protocolOp;
      final byte protocolOpType = (byte) reader.peek();
      switch (protocolOpType)
      {
        case PROTOCOL_OP_TYPE_BIND_REQUEST:
          protocolOp = new BindRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_BIND_RESPONSE:
          protocolOp = new BindResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_UNBIND_REQUEST:
          protocolOp = new UnbindRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_SEARCH_REQUEST:
          protocolOp = new SearchRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_SEARCH_RESULT_ENTRY:
          protocolOp = new SearchResultEntryProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_SEARCH_RESULT_REFERENCE:
          protocolOp = new SearchResultReferenceProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_SEARCH_RESULT_DONE:
          protocolOp = new SearchResultDoneProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_MODIFY_REQUEST:
          protocolOp = new ModifyRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_MODIFY_RESPONSE:
          protocolOp = new ModifyResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_ADD_REQUEST:
          protocolOp = new AddRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_ADD_RESPONSE:
          protocolOp = new AddResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_DELETE_REQUEST:
          protocolOp = new DeleteRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_DELETE_RESPONSE:
          protocolOp = new DeleteResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST:
          protocolOp = new ModifyDNRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_MODIFY_DN_RESPONSE:
          protocolOp = new ModifyDNResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_COMPARE_REQUEST:
          protocolOp = new CompareRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_COMPARE_RESPONSE:
          protocolOp = new CompareResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_ABANDON_REQUEST:
          protocolOp = new AbandonRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_EXTENDED_REQUEST:
          protocolOp = new ExtendedRequestProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_EXTENDED_RESPONSE:
          protocolOp = new ExtendedResponseProtocolOp(reader);
          break;
        case PROTOCOL_OP_TYPE_INTERMEDIATE_RESPONSE:
          protocolOp = new IntermediateResponseProtocolOp(reader);
          break;
        default:
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_MESSAGE_INVALID_PROTOCOL_OP_TYPE.get(toHex(protocolOpType)));
      }

      final LinkedList<Control> controls = new LinkedList<Control>();
      if (messageSequence.hasMoreElements())
      {
        final ASN1StreamReaderSequence controlSequence = reader.beginSequence();
        while (controlSequence.hasMoreElements())
        {
          controls.add(Control.readFrom(reader));
        }
      }

      return new LDAPMessage(messageID, protocolOp, controls);
    }
    catch (LDAPException le)
    {
      debugException(le);
      throw le;
    }
    catch (SocketTimeoutException ste)
    {
      debugException(ste);

      // We don't want to provide this exception as the cause because we want
      // to ensure that a failure in the middle of the response causes the
      // connection to be terminated.
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MESSAGE_CANNOT_DECODE.get(getExceptionMessage(ste)));
    }
    catch (IOException ioe)
    {
      debugException(ioe);

      throw new LDAPException(ResultCode.SERVER_DOWN,
           ERR_MESSAGE_IO_ERROR.get(getExceptionMessage(ioe)), ioe);
    }
    catch (Exception e)
    {
      debugException(e);

      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MESSAGE_CANNOT_DECODE.get(getExceptionMessage(e)), e);
    }
  }



  /**
   * Reads {@link LDAPResponse} object from the provided ASN.1 stream reader.
   *
   * @param  reader               The ASN.1 stream reader from which the LDAP
   *                              message should be read.
   * @param  ignoreSocketTimeout  Indicates whether to ignore socket timeout
   *                              exceptions caught during processing.  This
   *                              should be {@code true} when the associated
   *                              connection is operating in asynchronous mode,
   *                              and {@code false} when operating in
   *                              synchronous mode.  In either case, exceptions
   *                              will not be ignored for the first read, since
   *                              that will be handled by the connection reader.
   *
   * @return  The decoded LDAP message, or {@code null} if the end of the input
   *          stream has been reached..
   *
   * @throws  LDAPException  If an error occurs while attempting to read or
   *                         decode the LDAP message.
   */
  public static LDAPResponse readLDAPResponseFrom(final ASN1StreamReader reader,
                                  final boolean ignoreSocketTimeout)
         throws LDAPException
  {
    final ASN1StreamReaderSequence messageSequence;
    try
    {
      reader.setIgnoreSocketTimeout(false);
      messageSequence = reader.beginSequence();
      if (messageSequence == null)
      {
        return null;
      }
    }
    catch (IOException ioe)
    {
      debugException(ioe);

      throw new LDAPException(ResultCode.SERVER_DOWN,
           ERR_MESSAGE_IO_ERROR.get(getExceptionMessage(ioe)), ioe);
    }
    catch (Exception e)
    {
      debugException(e);

      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MESSAGE_CANNOT_DECODE.get(getExceptionMessage(e)), e);
    }

    try
    {
      reader.setIgnoreSocketTimeout(ignoreSocketTimeout);
      final int messageID = reader.readInteger();

      final byte protocolOpType = (byte) reader.peek();
      switch (protocolOpType)
      {
        case PROTOCOL_OP_TYPE_ADD_RESPONSE:
        case PROTOCOL_OP_TYPE_DELETE_RESPONSE:
        case PROTOCOL_OP_TYPE_MODIFY_RESPONSE:
        case PROTOCOL_OP_TYPE_MODIFY_DN_RESPONSE:
          return InternalSDKHelper.readLDAPResultFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_BIND_RESPONSE:
          return InternalSDKHelper.readBindResultFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_COMPARE_RESPONSE:
          return InternalSDKHelper.readCompareResultFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_EXTENDED_RESPONSE:
          return InternalSDKHelper.readExtendedResultFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_SEARCH_RESULT_ENTRY:
          return InternalSDKHelper.readSearchResultEntryFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_SEARCH_RESULT_REFERENCE:
          return InternalSDKHelper.readSearchResultReferenceFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_SEARCH_RESULT_DONE:
          return InternalSDKHelper.readSearchResultFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_INTERMEDIATE_RESPONSE:
          return InternalSDKHelper.readIntermediateResponseFrom(messageID,
                      messageSequence, reader);

        case PROTOCOL_OP_TYPE_ABANDON_REQUEST:
        case PROTOCOL_OP_TYPE_ADD_REQUEST:
        case PROTOCOL_OP_TYPE_BIND_REQUEST:
        case PROTOCOL_OP_TYPE_COMPARE_REQUEST:
        case PROTOCOL_OP_TYPE_DELETE_REQUEST:
        case PROTOCOL_OP_TYPE_EXTENDED_REQUEST:
        case PROTOCOL_OP_TYPE_MODIFY_REQUEST:
        case PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST:
        case PROTOCOL_OP_TYPE_SEARCH_REQUEST:
        case PROTOCOL_OP_TYPE_UNBIND_REQUEST:
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_MESSAGE_PROTOCOL_OP_TYPE_NOT_RESPONSE.get(
                    toHex(protocolOpType)));

        default:
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_MESSAGE_INVALID_PROTOCOL_OP_TYPE.get(toHex(protocolOpType)));
      }
    }
    catch (LDAPException le)
    {
      debugException(le);
      throw le;
    }
    catch (SocketTimeoutException ste)
    {
      debugException(ste);

      // We don't want to provide this exception as the cause because we want
      // to ensure that a failure in the middle of the response causes the
      // connection to be terminated.
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MESSAGE_CANNOT_DECODE.get(getExceptionMessage(ste)));
    }
    catch (IOException ioe)
    {
      debugException(ioe);

      throw new LDAPException(ResultCode.SERVER_DOWN,
           ERR_MESSAGE_IO_ERROR.get(getExceptionMessage(ioe)), ioe);
    }
    catch (Exception e)
    {
      debugException(e);

      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MESSAGE_CANNOT_DECODE.get(getExceptionMessage(e)), e);
    }
  }



  /**
   * Retrieves a string representation of this LDAP message.
   *
   * @return  A string representation of this LDAP message.
   */
  @Override()
  public String toString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this LDAP message to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which the string representation should be
   *                 appended.
   */
  public void toString(final StringBuilder buffer)
  {
    buffer.append("LDAPMessage(msgID=");
    buffer.append(messageID);
    buffer.append(", protocolOp=");
    protocolOp.toString(buffer);

    if (! controls.isEmpty())
    {
      buffer.append(", controls={");
      final Iterator<Control> iterator = controls.iterator();
      while (iterator.hasNext())
      {
        iterator.next().toString(buffer);
        if (iterator.hasNext())
        {
          buffer.append(',');
        }
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}
