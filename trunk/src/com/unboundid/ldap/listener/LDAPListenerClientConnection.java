/*
 * Copyright 2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2010 UnboundID Corp.
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
package com.unboundid.ldap.listener;



import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.unboundid.asn1.ASN1Buffer;
import com.unboundid.asn1.ASN1StreamReader;
import com.unboundid.ldap.protocol.IntermediateResponseProtocolOp;
import com.unboundid.ldap.protocol.LDAPMessage;
import com.unboundid.ldap.protocol.SearchResultEntryProtocolOp;
import com.unboundid.ldap.protocol.SearchResultReferenceProtocolOp;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.Debug;
import com.unboundid.util.InternalUseOnly;
import com.unboundid.util.ObjectPair;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.Validator;

import static com.unboundid.ldap.listener.ListenerMessages.*;



/**
 * This class provides an object which will be used to represent a connection to
 * a client accepted by an {@link LDAPListener}, although connections may also
 * be created independently if they were accepted in some other way.  Each
 * connection has its own thread that will be used to read requests from the
 * client, and connections created outside of an {@code LDAPListener} instance,
 * then the thread must be explicitly started.
 */
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class LDAPListenerClientConnection
       extends Thread
{
  /**
   * A pre-allocated empty array of controls.
   */
  private static final Control[] EMPTY_CONTROL_ARRAY = new Control[0];



  // The buffer used to hold responses to be sent to the client.
  private final ASN1Buffer asn1Buffer;

  // The ASN.1 stream reader used to read requests from the client.
  private final ASN1StreamReader asn1Reader;

  // The set of intermediate response transformers for this connection.
  private final CopyOnWriteArrayList<IntermediateResponseTransformer>
       intermediateResponseTransformers;

  // The set of search result entry transformers for this connection.
  private final CopyOnWriteArrayList<SearchEntryTransformer>
       searchEntryTransformers;

  // The set of search result reference transformers for this connection.
  private final CopyOnWriteArrayList<SearchReferenceTransformer>
       searchReferenceTransformers;

  // The listener that accepted this connection.
  private final LDAPListener listener;

  // The exception handler to use for this connection, if any.
  private final LDAPListenerExceptionHandler exceptionHandler;

  // The request handler to use for this connection.
  private final LDAPListenerRequestHandler requestHandler;

  // The connection ID assigned to this connection.
  private final long connectionID;

  // The output stream used to write responses to the client.
  private final OutputStream outputStream;

  // The socket used to communicate with the client.
  private final Socket socket;



  /**
   * Creates a new LDAP listener client connection that will communicate with
   * the client using the provided socket.  The {@link #start} method must be
   * called to start listening for requests from the client.
   *
   * @param  listener          The listener that accepted this client
   *                           connection.  It may be {@code null} if this
   *                           connection was not accepted by a listener.
   * @param  socket            The socket that may be used to communicate with
   *                           the client.  It must not be {@code null}.
   * @param  requestHandler    The request handler that will be used to process
   *                           requests read from the client.  The
   *                           {@link LDAPListenerRequestHandler#newInstance}
   *                           method will be called on the provided object to
   *                           obtain a new instance to use for this connection.
   *                           The provided request handler must not be
   *                           {@code null}.
   * @param  exceptionHandler  The disconnect handler to be notified when this
   *                           connection is closed.  It may be {@code null} if
   *                           no disconnect handler should be used.
   *
   * @throws  LDAPException  If a problem occurs while preparing this client
   *                         connection. for use.  If this is thrown, then the
   *                         provided socket will be closed.
   */
  public LDAPListenerClientConnection(final LDAPListener listener,
              final Socket socket,
              final LDAPListenerRequestHandler requestHandler,
              final LDAPListenerExceptionHandler exceptionHandler)
         throws LDAPException
  {
    Validator.ensureNotNull(socket, requestHandler);

    this.listener         = listener;
    this.socket           = socket;
    this.exceptionHandler = exceptionHandler;

    intermediateResponseTransformers =
         new CopyOnWriteArrayList<IntermediateResponseTransformer>();
    searchEntryTransformers =
         new CopyOnWriteArrayList<SearchEntryTransformer>();
    searchReferenceTransformers =
         new CopyOnWriteArrayList<SearchReferenceTransformer>();

    if (listener == null)
    {
      connectionID = -1L;
    }
    else
    {
      connectionID = listener.nextConnectionID();
    }

    try
    {
      final LDAPListenerConfig config;
      if (listener == null)
      {
        config = new LDAPListenerConfig(0, requestHandler);
      }
      else
      {
        config = listener.getConfig();
      }

      socket.setKeepAlive(config.useKeepAlive());
      socket.setReuseAddress(config.useReuseAddress());
      socket.setSoLinger(config.useLinger(), config.getLingerTimeoutSeconds());
      socket.setTcpNoDelay(config.useTCPNoDelay());

      final int sendBufferSize = config.getSendBufferSize();
      if (sendBufferSize > 0)
      {
        socket.setSendBufferSize(sendBufferSize);
      }

      asn1Reader = new ASN1StreamReader(socket.getInputStream());
    }
    catch (final IOException ioe)
    {
      Debug.debugException(ioe);

      try
      {
        socket.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      throw new LDAPException(ResultCode.CONNECT_ERROR,
           ERR_CONN_CREATE_IO_EXCEPTION.get(
                StaticUtils.getExceptionMessage(ioe)),
           ioe);
    }

    try
    {
      outputStream = socket.getOutputStream();
    }
    catch (final IOException ioe)
    {
      Debug.debugException(ioe);

      try
      {
        asn1Reader.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      try
      {
        socket.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      throw new LDAPException(ResultCode.CONNECT_ERROR,
           ERR_CONN_CREATE_IO_EXCEPTION.get(
                StaticUtils.getExceptionMessage(ioe)),
           ioe);
    }

    try
    {
      this.requestHandler = requestHandler.newInstance(this);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);

      try
      {
        asn1Reader.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      try
      {
        outputStream.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      try
      {
        socket.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      throw le;
    }

    asn1Buffer = new ASN1Buffer();
  }



  /**
   * Closes the connection to the client.
   *
   * @throws  IOException  If a problem occurs while closing the socket.
   */
  public void close()
         throws IOException
  {
    try
    {
      requestHandler.closeInstance();
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
    }

    try
    {
      asn1Reader.close();
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
    }

    try
    {
      outputStream.close();
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
    }

    socket.close();
  }



  /**
   * Closes the connection to the client as a result of an exception encountered
   * during processing.  Any associated exception handler will be notified
   * prior to the connection closure.
   *
   * @param  le  The exception providing information about the reason that this
   *             connection will be terminated.
   */
  void close(final LDAPException le)
  {
    if (exceptionHandler == null)
    {
      Debug.debugException(le);
    }
    else
    {
      try
      {
        exceptionHandler.connectionTerminated(this, le);
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }
    }

    try
    {
      close();
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
    }
  }



  /**
   * Operates in a loop, waiting for a request to arrive from the client and
   * handing it off to the request handler for processing.  This method is for
   * internal use only and must not be invoked by external callers.
   */
  @InternalUseOnly()
  @Override()
  public void run()
  {
    try
    {
      while (true)
      {
        final LDAPMessage requestMessage;
        try
        {
          requestMessage = LDAPMessage.readFrom(asn1Reader, false);
          if (requestMessage == null)
          {
            // This indicates that the client has closed the connection without
            // an unbind request.  It's not all that nice, but it isn't an error
            // so we won't notify the exception handler.
            try
            {
              close();
            }
            catch (final IOException ioe)
            {
              Debug.debugException(ioe);
            }

            return;
          }
        }
        catch (final LDAPException le)
        {
          Debug.debugException(le);
          close(le);
          return;
        }

        try
        {
          final int messageID = requestMessage.getMessageID();
          final List<Control> controls = requestMessage.getControls();

          final LDAPMessage responseMessage;
          switch (requestMessage.getProtocolOpType())
          {
            case LDAPMessage.PROTOCOL_OP_TYPE_ABANDON_REQUEST:
              requestHandler.processAbandonRequest(messageID,
                   requestMessage.getAbandonRequestProtocolOp(), controls);
              responseMessage = null;
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_ADD_REQUEST:
              responseMessage = requestHandler.processAddRequest(messageID,
                   requestMessage.getAddRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_BIND_REQUEST:
              responseMessage = requestHandler.processBindRequest(messageID,
                   requestMessage.getBindRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_COMPARE_REQUEST:
              responseMessage = requestHandler.processCompareRequest(messageID,
                   requestMessage.getCompareRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_DELETE_REQUEST:
              responseMessage = requestHandler.processDeleteRequest(messageID,
                   requestMessage.getDeleteRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_EXTENDED_REQUEST:
              responseMessage = requestHandler.processExtendedRequest(messageID,
                   requestMessage.getExtendedRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_REQUEST:
              responseMessage = requestHandler.processModifyRequest(messageID,
                   requestMessage.getModifyRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST:
              responseMessage = requestHandler.processModifyDNRequest(messageID,
                   requestMessage.getModifyDNRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_SEARCH_REQUEST:
              responseMessage = requestHandler.processSearchRequest(messageID,
                   requestMessage.getSearchRequestProtocolOp(), controls);
              break;

            case LDAPMessage.PROTOCOL_OP_TYPE_UNBIND_REQUEST:
              requestHandler.processUnbindRequest(messageID,
                   requestMessage.getUnbindRequestProtocolOp(), controls);
              close();
              return;

            default:
              close(new LDAPException(ResultCode.PROTOCOL_ERROR,
                   ERR_CONN_INVALID_PROTOCOL_OP_TYPE.get(StaticUtils.toHex(
                        requestMessage.getProtocolOpType()))));
              return;
          }

          if (responseMessage != null)
          {
            try
            {
              sendMessage(responseMessage);
            }
            catch (final LDAPException le)
            {
              Debug.debugException(le);
              close(le);
              return;
            }
          }
        }
        catch (final Exception e)
        {
          close(new LDAPException(ResultCode.LOCAL_ERROR,
               ERR_CONN_EXCEPTION_IN_REQUEST_HANDLER.get(
                    String.valueOf(requestMessage),
                    StaticUtils.getExceptionMessage(e))));
          return;
        }
      }
    }
    finally
    {
      if (listener != null)
      {
        listener.connectionClosed(this);
      }
    }
  }



  /**
   * Sends the provided message to the client.
   *
   * @param  message  The message to be written to the client.
   *
   * @throws  LDAPException  If a problem occurs while attempting to send the
   *                         response to the client.
   */
  private synchronized void sendMessage(final LDAPMessage message)
          throws LDAPException
  {
    asn1Buffer.clear();
    message.writeTo(asn1Buffer);

    try
    {
      asn1Buffer.writeTo(outputStream);
    }
    catch (final IOException ioe)
    {
      Debug.debugException(ioe);

      throw new LDAPException(ResultCode.LOCAL_ERROR,
           ERR_CONN_SEND_MESSAGE_EXCEPTION.get(
                StaticUtils.getExceptionMessage(ioe)),
           ioe);
    }
  }



  /**
   * Sends a search result entry message to the client with the provided
   * information.
   *
   * @param  messageID   The message ID for the LDAP message to send to the
   *                     client.  It must match the message ID of the associated
   *                     search request.
   * @param  protocolOp  The search result entry protocol op to include in the
   *                     LDAP message to send to the client.  It must not be
   *                     {@code null}.
   * @param  controls    The set of controls to include in the response message.
   *                     It may be empty or {@code null} if no controls should
   *                     be included.
   *
   * @throws  LDAPException  If a problem occurs while attempting to send the
   *                         provided response message.  If an exception is
   *                         thrown, then the client connection will have been
   *                         terminated.
   */
  public void sendSearchResultEntry(final int messageID,
                   final SearchResultEntryProtocolOp protocolOp,
                   final Control... controls)
         throws LDAPException
  {
    if (searchEntryTransformers.isEmpty())
    {
      sendMessage(new LDAPMessage(messageID, protocolOp, controls));
    }
    else
    {
      Control[] c;
      SearchResultEntryProtocolOp op = protocolOp;
      if (controls == null)
      {
        c = EMPTY_CONTROL_ARRAY;
      }
      else
      {
        c = controls;
      }

      for (final SearchEntryTransformer t : searchEntryTransformers)
      {
        final ObjectPair<SearchResultEntryProtocolOp,Control[]> p =
             t.transformEntry(messageID, op, c);
        if (p == null)
        {
          return;
        }

        op = p.getFirst();
        c  = p.getSecond();
      }

      sendMessage(new LDAPMessage(messageID, op, c));
    }
  }



  /**
   * Sends a search result entry message to the client with the provided
   * information.
   *
   * @param  messageID  The message ID for the LDAP message to send to the
   *                    client.  It must match the message ID of the associated
   *                    search request.
   * @param  entry      The entry to return to the client.  It must not be
   *                    {@code null}.
   * @param  controls   The set of controls to include in the response message.
   *                    It may be empty or {@code null} if no controls should be
   *                    included.
   *
   * @throws  LDAPException  If a problem occurs while attempting to send the
   *                         provided response message.  If an exception is
   *                         thrown, then the client connection will have been
   *                         terminated.
   */
  public void sendSearchResultEntry(final int messageID, final Entry entry,
                                    final Control... controls)
         throws LDAPException
  {
    sendSearchResultEntry(messageID,
         new SearchResultEntryProtocolOp(entry.getDN(),
              new ArrayList<Attribute>(entry.getAttributes())),
         controls);
  }



  /**
   * Sends a search result reference message to the client with the provided
   * information.
   *
   * @param  messageID   The message ID for the LDAP message to send to the
   *                     client.  It must match the message ID of the associated
   *                     search request.
   * @param  protocolOp  The search result reference protocol op to include in
   *                     the LDAP message to send to the client.
   * @param  controls    The set of controls to include in the response message.
   *                     It may be empty or {@code null} if no controls should
   *                     be included.
   *
   * @throws  LDAPException  If a problem occurs while attempting to send the
   *                         provided response message.  If an exception is
   *                         thrown, then the client connection will have been
   *                         terminated.
   */
  public void sendSearchResultReference(final int messageID,
                   final SearchResultReferenceProtocolOp protocolOp,
                   final Control... controls)
         throws LDAPException
  {
    if (searchReferenceTransformers.isEmpty())
    {
      sendMessage(new LDAPMessage(messageID, protocolOp, controls));
    }
    else
    {
      Control[] c;
      SearchResultReferenceProtocolOp op = protocolOp;
      if (controls == null)
      {
        c = EMPTY_CONTROL_ARRAY;
      }
      else
      {
        c = controls;
      }

      for (final SearchReferenceTransformer t : searchReferenceTransformers)
      {
        final ObjectPair<SearchResultReferenceProtocolOp,Control[]> p =
             t.transformReference(messageID, op, c);
        if (p == null)
        {
          return;
        }

        op = p.getFirst();
        c  = p.getSecond();
      }

      sendMessage(new LDAPMessage(messageID, op, c));
    }
  }



  /**
   * Sends an intermediate response message to the client with the provided
   * information.
   *
   * @param  messageID   The message ID for the LDAP message to send to the
   *                     client.  It must match the message ID of the associated
   *                     search request.
   * @param  protocolOp  The intermediate response protocol op to include in the
   *                     LDAP message to send to the client.
   * @param  controls    The set of controls to include in the response message.
   *                     It may be empty or {@code null} if no controls should
   *                     be included.
   *
   * @throws  LDAPException  If a problem occurs while attempting to send the
   *                         provided response message.  If an exception is
   *                         thrown, then the client connection will have been
   *                         terminated.
   */
  public void sendIntermediateResponse(final int messageID,
                   final IntermediateResponseProtocolOp protocolOp,
                   final Control... controls)
         throws LDAPException
  {
    if (intermediateResponseTransformers.isEmpty())
    {
      sendMessage(new LDAPMessage(messageID, protocolOp, controls));
    }
    else
    {
      Control[] c;
      IntermediateResponseProtocolOp op = protocolOp;
      if (controls == null)
      {
        c = EMPTY_CONTROL_ARRAY;
      }
      else
      {
        c = controls;
      }

      for (final IntermediateResponseTransformer t :
           intermediateResponseTransformers)
      {
        final ObjectPair<IntermediateResponseProtocolOp,Control[]> p =
             t.transformIntermediateResponse(messageID, op, c);
        if (p == null)
        {
          return;
        }

        op = p.getFirst();
        c  = p.getSecond();
      }

      sendMessage(new LDAPMessage(messageID, op, c));
    }
  }



  /**
   * Retrieves the socket used to communicate with the client.
   *
   * @return  The socket used to communicate with the client.
   */
  public Socket getSocket()
  {
    return socket;
  }



  /**
   * Retrieves the connection ID that has been assigned to this connection by
   * the associated listener.
   *
   * @return  The connection ID that has been assigned to this connection by
   *          the associated listener, or -1 if it is not associated with a
   *          listener.
   */
  public long getConnectionID()
  {
    return connectionID;
  }



  /**
   * Adds the provided search entry transformer to this client connection.
   *
   * @param  t  A search entry transformer to be used to intercept and/or alter
   *            search result entries before they are returned to the client.
   */
  public void addSearchEntryTransformer(final SearchEntryTransformer t)
  {
    searchEntryTransformers.add(t);
  }



  /**
   * Removes the provided search entry transformer from this client connection.
   *
   * @param  t  The search entry transformer to be removed.
   */
  public void removeSearchEntryTransformer(final SearchEntryTransformer t)
  {
    searchEntryTransformers.remove(t);
  }



  /**
   * Adds the provided search reference transformer to this client connection.
   *
   * @param  t  A search reference transformer to be used to intercept and/or
   *            alter search result references before they are returned to the
   *            client.
   */
  public void addSearchReferenceTransformer(final SearchReferenceTransformer t)
  {
    searchReferenceTransformers.add(t);
  }



  /**
   * Removes the provided search reference transformer from this client
   * connection.
   *
   * @param  t  The search reference transformer to be removed.
   */
  public void removeSearchReferenceTransformer(
                   final SearchReferenceTransformer t)
  {
    searchReferenceTransformers.remove(t);
  }



  /**
   * Adds the provided intermediate response transformer to this client
   * connection.
   *
   * @param  t  An intermediate response transformer to be used to intercept
   *            and/or alter intermediate responses before they are returned to
   *            the client.
   */
  public void addIntermediateResponseTransformer(
                   final IntermediateResponseTransformer t)
  {
    intermediateResponseTransformers.add(t);
  }



  /**
   * Removes the provided intermediate response transformer from this client
   * connection.
   *
   * @param  t  The intermediate response transformer to be removed.
   */
  public void removeIntermediateResponseTransformer(
                   final IntermediateResponseTransformer t)
  {
    intermediateResponseTransformers.remove(t);
  }
}
