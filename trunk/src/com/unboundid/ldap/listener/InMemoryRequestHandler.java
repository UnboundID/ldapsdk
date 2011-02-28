/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2011 UnboundID Corp.
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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.protocol.AddRequestProtocolOp;
import com.unboundid.ldap.protocol.AddResponseProtocolOp;
import com.unboundid.ldap.protocol.BindRequestProtocolOp;
import com.unboundid.ldap.protocol.BindResponseProtocolOp;
import com.unboundid.ldap.protocol.CompareRequestProtocolOp;
import com.unboundid.ldap.protocol.CompareResponseProtocolOp;
import com.unboundid.ldap.protocol.DeleteRequestProtocolOp;
import com.unboundid.ldap.protocol.DeleteResponseProtocolOp;
import com.unboundid.ldap.protocol.ExtendedRequestProtocolOp;
import com.unboundid.ldap.protocol.ExtendedResponseProtocolOp;
import com.unboundid.ldap.protocol.LDAPMessage;
import com.unboundid.ldap.protocol.ModifyRequestProtocolOp;
import com.unboundid.ldap.protocol.ModifyResponseProtocolOp;
import com.unboundid.ldap.protocol.ModifyDNRequestProtocolOp;
import com.unboundid.ldap.protocol.ModifyDNResponseProtocolOp;
import com.unboundid.ldap.protocol.SearchRequestProtocolOp;
import com.unboundid.ldap.protocol.SearchResultDoneProtocolOp;
import com.unboundid.ldap.matchingrules.DistinguishedNameMatchingRule;
import com.unboundid.ldap.matchingrules.GeneralizedTimeMatchingRule;
import com.unboundid.ldap.matchingrules.IntegerMatchingRule;
import com.unboundid.ldap.matchingrules.MatchingRule;
import com.unboundid.ldap.matchingrules.OctetStringMatchingRule;
import com.unboundid.ldap.protocol.SearchResultReferenceProtocolOp;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.ChangeLogEntry;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ExtendedRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.Version;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.EntryValidator;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ldap.sdk.controls.AssertionRequestControl;
import com.unboundid.ldap.sdk.controls.AuthorizationIdentityRequestControl;
import com.unboundid.ldap.sdk.controls.AuthorizationIdentityResponseControl;
import com.unboundid.ldap.sdk.controls.ManageDsaITRequestControl;
import com.unboundid.ldap.sdk.controls.PermissiveModifyRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadResponseControl;
import com.unboundid.ldap.sdk.controls.PreReadRequestControl;
import com.unboundid.ldap.sdk.controls.PreReadResponseControl;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV1RequestControl;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.ldap.sdk.controls.SubentriesRequestControl;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.ldif.LDIFAddChangeRecord;
import com.unboundid.ldif.LDIFDeleteChangeRecord;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFModifyChangeRecord;
import com.unboundid.ldif.LDIFModifyDNChangeRecord;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.ldif.LDIFWriter;
import com.unboundid.util.Debug;
import com.unboundid.util.Mutable;
import com.unboundid.util.ObjectPair;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.listener.ListenerMessages.*;



/**
 * This class provides an implementation of an LDAP request handler that can be
 * used to store entries in memory and process operations on those entries.
 * It is primarily intended for use in creating a simple embeddable directory
 * server that can be used for testing purposes.  It performs only very basic
 * validation, and is not intended to be a fully standards-compliant server.
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class InMemoryRequestHandler
       extends LDAPListenerRequestHandler
{
  // TODO -- Add support for prohibiting object class modifications
  // TODO -- Use schema when applying modifications
  // TODO -- Add support for schema modifications
  // TODO -- Merge schemas



  /**
   * The OID for a proprietary control that can be used to indicate that the
   * NO-USER-MODIFICATION flag should be ignored when processing an add.
   */
  static final String OID_ADD_IGNORE_NO_USER_MODIFICATION =
       "1.3.6.1.4.1.30221.2.5.5";



  // The change number for the first changelog entry in the server.
  private final AtomicLong firstChangeNumber;

  // The change number for the last changelog entry in the server.
  private final AtomicLong lastChangeNumber;

  // Indicates whether to generate operational attributes for writes.
  private final boolean generateOperationalAttributes;

  // The DN of the currently-authenticated user for the associated connection.
  private DN authenticatedDN;

  // The base DN for the server changelog.
  private final DN changeLogBaseDN;

  // The DN of the subschema subentry.
  private final DN subschemaSubentryDN;

  // The entry validator that will be used for schema checking, if
  // appropriate.
  private final EntryValidator entryValidator;

  // The maximum number of changelog entries to maintain.
  private final int maxChangelogEntries;

  // The client connection for this request handler instance.
  private final LDAPListenerClientConnection connection;

  // An additional set of credentials that may be used for bind operations.
  private final Map<DN,byte[]> additionalBindCredentials;

  // A map of the available extended operation handlers by request OID.
  private final Map<String,InMemoryExtendedOperationHandler>
       extendedRequestHandlers;

  // A map of the available SASL bind handlers by mechanism name.
  private final Map<String,InMemorySASLBindHandler> saslBindHandlers;

  // A map of state information specific to the associated connection.
  private final Map<String,Object> connectionState;

  // The entry to use as the subschema subentry.
  private final ReadOnlyEntry subschemaSubentry;

  // The schema that will be used for this request handler.  It may be null.
  private final Schema schema;

  // The set of base DNs for the server.
  private final Set<DN> baseDNs;

  // The map of entries currently held in the server.
  private final TreeMap<DN,Entry> entryMap;



  /**
   * Creates a new instance of this request handler with an initially-empty
   * data set.
   *
   * @param  config  The configuration that should be used for the in-memory
   *                 directory server.
   *
   * @throws  LDAPException  If there is a problem with the provided
   *                         configuration.
   */
  public InMemoryRequestHandler(final InMemoryDirectoryServerConfig config)
         throws LDAPException
  {
    schema = config.getSchema();
    if (schema == null)
    {
      entryValidator = null;
    }
    else
    {
      entryValidator = new EntryValidator(schema);
    }

    final DN[] baseDNArray = config.getBaseDNs();
    if ((baseDNArray == null) || (baseDNArray.length == 0))
    {
      throw new LDAPException(ResultCode.PARAM_ERROR,
           ERR_MEM_HANDLER_NO_BASE_DNS.get());
    }

    entryMap = new TreeMap<DN,Entry>();

    final LinkedHashSet<DN> baseDNSet =
         new LinkedHashSet<DN>(Arrays.asList(baseDNArray));
    if (baseDNSet.contains(DN.NULL_DN))
    {
      throw new LDAPException(ResultCode.PARAM_ERROR,
           ERR_MEM_HANDLER_NULL_BASE_DN.get());
    }

    changeLogBaseDN = new DN("cn=changelog");
    if (baseDNSet.contains(changeLogBaseDN))
    {
      throw new LDAPException(ResultCode.PARAM_ERROR,
           ERR_MEM_HANDLER_CHANGELOG_BASE_DN.get());
    }

    maxChangelogEntries = config.getMaxChangeLogEntries();

    final TreeMap<String,InMemoryExtendedOperationHandler> extOpHandlers =
         new TreeMap<String,InMemoryExtendedOperationHandler>();
    for (final InMemoryExtendedOperationHandler h :
         config.getExtendedOperationHandlers())
    {
      for (final String oid : h.getSupportedExtendedRequestOIDs())
      {
        if (extOpHandlers.containsKey(oid))
        {
          throw new LDAPException(ResultCode.PARAM_ERROR,
               ERR_MEM_HANDLER_EXTENDED_REQUEST_HANDLER_CONFLICT.get(oid));
        }
        else
        {
          extOpHandlers.put(oid, h);
        }
      }
    }
    extendedRequestHandlers = Collections.unmodifiableMap(extOpHandlers);

    final TreeMap<String,InMemorySASLBindHandler> saslHandlers =
         new TreeMap<String,InMemorySASLBindHandler>();
    for (final InMemorySASLBindHandler h : config.getSASLBindHandlers())
    {
      final String mech = h.getSASLMechanismName();
      if (saslHandlers.containsKey(mech))
      {
        throw new LDAPException(ResultCode.PARAM_ERROR,
             ERR_MEM_HANDLER_SASL_BIND_HANDLER_CONFLICT.get(mech));
      }
      else
      {
        saslHandlers.put(mech, h);
      }
    }
    saslBindHandlers = Collections.unmodifiableMap(saslHandlers);

    additionalBindCredentials = Collections.unmodifiableMap(
         config.getAdditionalBindCredentials());

    baseDNs = Collections.unmodifiableSet(baseDNSet);
    generateOperationalAttributes = config.generateOperationalAttributes();
    authenticatedDN               = DN.NULL_DN;
    connection                    = null;
    connectionState               = Collections.emptyMap();
    firstChangeNumber             = new AtomicLong(0L);
    lastChangeNumber              = new AtomicLong(0L);
    subschemaSubentry             = generateSubschemaSubentry(schema);
    subschemaSubentryDN           = subschemaSubentry.getParsedDN();

    if (baseDNs.contains(subschemaSubentryDN))
    {
      throw new LDAPException(ResultCode.PARAM_ERROR,
           ERR_MEM_HANDLER_SCHEMA_BASE_DN.get());
    }

    if (maxChangelogEntries > 0)
    {
      baseDNSet.add(changeLogBaseDN);
      entryMap.put(changeLogBaseDN, new Entry(changeLogBaseDN,
           new Attribute("objectClass", "top", "namedObject"),
           new Attribute("cn", "changelog"),
           new Attribute("entryDN",
                DistinguishedNameMatchingRule.getInstance(),
                "cn=changelog"),
           new Attribute("entryUUID", UUID.randomUUID().toString()),
           new Attribute("creatorsName",
                DistinguishedNameMatchingRule.getInstance(),
                DN.NULL_DN.toString()),
           new Attribute("createTimestamp",
                GeneralizedTimeMatchingRule.getInstance(),
                StaticUtils.encodeGeneralizedTime(new Date())),
           new Attribute("modifiersName",
                DistinguishedNameMatchingRule.getInstance(),
                DN.NULL_DN.toString()),
           new Attribute("modifyTimestamp",
                GeneralizedTimeMatchingRule.getInstance(),
                StaticUtils.encodeGeneralizedTime(new Date())),
           new Attribute("subschemaSubentry",
                DistinguishedNameMatchingRule.getInstance(),
                subschemaSubentryDN.toString())));
    }
  }



  /**
   * Creates a new instance of this request handler that will use the provided
   * entry map object.
   *
   * @param  parent      The parent request handler instance.
   * @param  connection  The client connection for this instance.
   */
  private InMemoryRequestHandler(final InMemoryRequestHandler parent,
               final LDAPListenerClientConnection connection)
  {
    this.connection = connection;

    authenticatedDN = DN.NULL_DN;
    connectionState = new LinkedHashMap<String,Object>(0);

    generateOperationalAttributes = parent.generateOperationalAttributes;
    additionalBindCredentials     = parent.additionalBindCredentials;
    baseDNs                       = parent.baseDNs;
    changeLogBaseDN               = parent.changeLogBaseDN;
    firstChangeNumber             = parent.firstChangeNumber;
    lastChangeNumber              = parent.lastChangeNumber;
    maxChangelogEntries           = parent.maxChangelogEntries;
    entryMap                      = parent.entryMap;
    entryValidator                = parent.entryValidator;
    extendedRequestHandlers       = parent.extendedRequestHandlers;
    saslBindHandlers              = parent.saslBindHandlers;
    schema                        = parent.schema;
    subschemaSubentry             = parent.subschemaSubentry;
    subschemaSubentryDN           = parent.subschemaSubentryDN;
  }



  /**
   * Creates a new instance of this request handler that will be used to process
   * requests read by the provided connection.
   *
   * @param  connection  The connection with which this request handler instance
   *                     will be associated.
   *
   * @return  The request handler instance that will be used for the provided
   *          connection.
   *
   * @throws  LDAPException  If the connection should not be accepted.
   */
  @Override()
  public InMemoryRequestHandler newInstance(
              final LDAPListenerClientConnection connection)
         throws LDAPException
  {
    return new InMemoryRequestHandler(this, connection);
  }



  /**
   * Retrieves the schema that will be used by the server, if any.
   *
   * @return  The schema that will be used by the server, or {@code null} if
   *          none has been configured.
   */
  public Schema getSchema()
  {
    return schema;
  }



  /**
   * Retrieves a list of the base DNs configured for use by the server.
   *
   * @return  A list of the base DNs configured for use by the server.
   */
  public List<DN> getBaseDNs()
  {
    return Collections.unmodifiableList(new ArrayList<DN>(baseDNs));
  }



  /**
   * Retrieves the client connection associated with this request handler
   * instance.
   *
   * @return  The client connection associated with this request handler
   *          instance, or {@code null} if this instance is not associated with
   *          any client connection.
   */
  public synchronized LDAPListenerClientConnection getClientConnection()
  {
    return connection;
  }



  /**
   * Retrieves the DN of the user currently authenticated on the connection
   * associated with this request handler instance.
   *
   * @return  The DN of the user currently authenticated on the connection
   *          associated with this request handler instance, or
   *          {@code DN#NULL_DN} if the connection is unauthenticated or is
   *          authenticated as the anonymous user.
   */
  public synchronized DN getAuthenticatedDN()
  {
    return authenticatedDN;
  }



  /**
   * Sets the DN of the user currently authenticated on the connection
   * associated with this request handler instance.
   *
   * @param  authenticatedDN  The DN of the user currently authenticated on the
   *                          connection associated with this request handler.
   *                          It may be {@code null} or {@link DN#NULL_DN} to
   *                          indicate that the connection is unauthenticated.
   */
  public synchronized void setAuthenticatedDN(final DN authenticatedDN)
  {
    if (authenticatedDN == null)
    {
      this.authenticatedDN = DN.NULL_DN;
    }
    else
    {
      this.authenticatedDN = authenticatedDN;
    }
  }



  /**
   * Retrieves an unmodifiable map containing the defined set of additional bind
   * credentials, mapped from bind DN to password bytes.
   *
   * @return  An unmodifiable map containing the defined set of additional bind
   *          credentials, or an empty map if no additional credentials have
   *          been defined.
   */
  public Map<DN,byte[]> getAdditionalBindCredentials()
  {
    return additionalBindCredentials;
  }



  /**
   * Retrieves the password for the given DN from the set of additional bind
   * credentials.
   *
   * @param  dn  The DN for which to retrieve the corresponding password.
   *
   * @return  The password bytes for the given DN, or {@code null} if the
   *          additional bind credentials does not include information for the
   *          provided DN.
   */
  public byte[] getAdditionalBindCredentials(final DN dn)
  {
    return additionalBindCredentials.get(dn);
  }



  /**
   * Retrieves a map that may be used to hold state information specific to the
   * connection associated with this request handler instance.  It may be
   * queried and updated if necessary to store state information that may be
   * needed at multiple different times in the life of a connection (e.g., when
   * processing a multi-stage SASL bind).
   *
   * @return  An updatable map that may be used to hold state information
   *          specific to the connection associated with this request handler
   *          instance.
   */
  public synchronized Map<String,Object> getConnectionState()
  {
    return connectionState;
  }



  /**
   * Attempts to add an entry to the in-memory data set.  The attempt will fail
   * if any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The provided entry has a malformed DN.</LI>
   *   <LI>The provided entry has the null DN.</LI>
   *   <LI>The provided entry has a DN that is the same as or subordinate to the
   *       subschema subentry.</LI>
   *   <LI>The provided entry has a DN that is the same as or subordinate to the
   *       changelog base entry.</LI>
   *   <LI>An entry already exists with the same DN as the entry in the provided
   *       request.</LI>
   *   <LI>The entry is outside the set of base DNs for the server.</LI>
   *   <LI>The entry is below one of the defined base DNs but the immediate
   *       parent entry does not exist.</LI>
   *   <LI>If a schema was provided, and the entry is not valid according to the
   *       constraints of that schema.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the add
   *                    request.
   * @param  request    The add request that was included in the LDAP message
   *                    that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be an
   *          {@code AddResponseProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processAddRequest(final int messageID,
                                       final AddRequestProtocolOp request,
                                       final List<Control> controls)
  {
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_ADD_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // Get the entry to be added.  If a schema was provided, then make sure the
    // attributes are created with the appropriate matching rules.
    final Entry entry;
    if (schema == null)
    {
      entry = new Entry(request.getDN(), request.getAttributes());
    }
    else
    {
      final List<Attribute> providedAttrs = request.getAttributes();
      final List<Attribute> newAttrs =
           new ArrayList<Attribute>(providedAttrs.size());
      for (final Attribute a : providedAttrs)
      {
        final String baseName = a.getBaseName();
        final MatchingRule matchingRule =
             MatchingRule.selectEqualityMatchingRule(baseName, schema);
        newAttrs.add(new Attribute(a.getName(), matchingRule,
             a.getRawValues()));
      }

      entry = new Entry(request.getDN(), newAttrs);
    }

    // Make sure that the DN is valid.
    final DN dn;
    try
    {
      dn = entry.getParsedDN();
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_ADD_MALFORMED_DN.get(request.getDN(),
                le.getMessage()),
           null));
    }

    // See if the DN is the null DN, the schema entry DN, or a changelog entry.
    if (dn.isNullDN())
    {
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE, null,
           ERR_MEM_HANDLER_ADD_ROOT_DSE.get(), null));
    }
    else if (dn.isDescendantOf(subschemaSubentryDN, true))
    {
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE, null,
           ERR_MEM_HANDLER_ADD_SCHEMA.get(subschemaSubentryDN.toString()),
           null));
    }
    else if (dn.isDescendantOf(changeLogBaseDN, true))
    {
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_ADD_CHANGELOG.get(changeLogBaseDN.toString()),
           null));
    }

    // See if there is a referral at or above the target entry.
    if (! controlMap.containsKey(
               ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID))
    {
      final Entry referralEntry = findNearestReferral(dn);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new AddResponseProtocolOp(
             ResultCode.REFERRAL_INT_VALUE, referralEntry.getDN(),
             INFO_MEM_HANDLER_REFERRAL_ENCOUNTERED.get(),
             getReferralURLs(dn, referralEntry)));
      }
    }

    // See if another entry exists with the same DN.
    if (entryMap.containsKey(dn))
    {
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE, null,
           ERR_MEM_HANDLER_ADD_ALREADY_EXISTS.get(request.getDN()), null));
    }

    // Make sure that all RDN attribute values are present in the entry.
    final RDN      rdn           = dn.getRDN();
    final String[] rdnAttrNames  = rdn.getAttributeNames();
    final byte[][] rdnAttrValues = rdn.getByteArrayAttributeValues();
    for (int i=0; i < rdnAttrNames.length; i++)
    {
      final MatchingRule matchingRule =
           MatchingRule.selectEqualityMatchingRule(rdnAttrNames[i], schema);
      entry.addAttribute(new Attribute(rdnAttrNames[i], matchingRule,
           rdnAttrValues[i]));
    }

    // Make sure that all superior object classes are present in the entry.
    if (schema != null)
    {
      final String[] objectClasses = entry.getObjectClassValues();
      if (objectClasses != null)
      {
        final ArrayList<String> ocNames =
             new ArrayList<String>(objectClasses.length);
        final LinkedHashSet<ObjectClassDefinition> ocSet =
             new LinkedHashSet<ObjectClassDefinition>(objectClasses.length);
        for (final String ocName : objectClasses)
        {
          ocNames.add(ocName);
          final ObjectClassDefinition oc = schema.getObjectClass(ocName);
          if (oc != null)
          {
            ocSet.add(oc);
            for (final ObjectClassDefinition supClass :
                 oc.getSuperiorClasses(schema, true))
            {
              if (! ocSet.contains(supClass))
              {
                ocSet.add(supClass);
                ocNames.add(supClass.getNameOrOID());
              }
            }
          }
        }

        final String[] newObjectClasses = new String[ocNames.size()];
        ocNames.toArray(newObjectClasses);
        entry.setAttribute("objectClass", newObjectClasses);
      }
    }

    // If a schema was provided, then make sure the entry complies with it.
    // Also make sure that there are no attributes marked with
    // NO-USER-MODIFICATION.
    if (entryValidator != null)
    {
      final ArrayList<String> invalidReasons =
           new ArrayList<String>(1);
      if (! entryValidator.entryIsValid(entry, invalidReasons))
      {
        return new LDAPMessage(messageID, new AddResponseProtocolOp(
             ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE, null,
             ERR_MEM_HANDLER_ADD_VIOLATES_SCHEMA.get(request.getDN(),
                  StaticUtils.concatenateStrings(invalidReasons)), null));
      }

      if (! controlMap.containsKey(OID_ADD_IGNORE_NO_USER_MODIFICATION))
      {
        for (final Attribute a : entry.getAttributes())
        {
          final AttributeTypeDefinition at =
               schema.getAttributeType(a.getBaseName());
          if ((at != null) && at.isNoUserModification())
          {
            return new LDAPMessage(messageID, new AddResponseProtocolOp(
                 ResultCode.CONSTRAINT_VIOLATION_INT_VALUE, null,
                 ERR_MEM_HANDLER_ADD_CONTAINS_NO_USER_MOD.get(request.getDN(),
                      a.getName()), null));
          }
        }
      }
    }

    // If the entry contains a proxied authorization control, then process it.
    final DN authzDN;
    try
    {
      authzDN = handleProxiedAuthControl(controlMap);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // Add a number of operational attributes to the entry.
    if (generateOperationalAttributes)
    {
      final Date d = new Date();
      if (! entry.hasAttribute("entryDN"))
      {
        entry.addAttribute(new Attribute("entryDN",
             DistinguishedNameMatchingRule.getInstance(),
             dn.toNormalizedString()));
      }
      if (! entry.hasAttribute("entryUUID"))
      {
        entry.addAttribute(new Attribute("entryUUID",
             UUID.randomUUID().toString()));
      }
      if (! entry.hasAttribute("subschemaSubentry"))
      {
        entry.addAttribute(new Attribute("subschemaSubentry",
             DistinguishedNameMatchingRule.getInstance(),
             subschemaSubentryDN.toString()));
      }
      if (! entry.hasAttribute("creatorsName"))
      {
        entry.addAttribute(new Attribute("creatorsName",
             DistinguishedNameMatchingRule.getInstance(),
             authzDN.toString()));
      }
      if (! entry.hasAttribute("createTimestamp"))
      {
        entry.addAttribute(new Attribute("createTimestamp",
             GeneralizedTimeMatchingRule.getInstance(),
             StaticUtils.encodeGeneralizedTime(d)));
      }
      if (! entry.hasAttribute("modifiersName"))
      {
        entry.addAttribute(new Attribute("modifiersName",
             DistinguishedNameMatchingRule.getInstance(),
             authzDN.toString()));
      }
      if (! entry.hasAttribute("modifyTimestamp"))
      {
        entry.addAttribute(new Attribute("modifyTimestamp",
             GeneralizedTimeMatchingRule.getInstance(),
             StaticUtils.encodeGeneralizedTime(d)));
      }
    }

    // If the request includes the assertion request control, then check it now.
    try
    {
      handleAssertionRequestControl(controlMap, entry);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new AddResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // If the request includes the post-read request control, then create the
    // appropriate response control.
    final PostReadResponseControl postReadResponse =
         handlePostReadControl(controlMap, entry);
    if (postReadResponse != null)
    {
      responseControls.add(postReadResponse);
    }

    // See if the entry DN is one of the defined base DNs.  If so, then we can
    // add the entry.
    if (baseDNs.contains(dn))
    {
      entryMap.put(dn, entry);
      addChangeLogEntry(request, authzDN);
      return new LDAPMessage(messageID,
           new AddResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null,
                null),
           responseControls);
    }

    // See if the parent entry exists.  If so, then we can add the entry.
    final DN parentDN = dn.getParent();
    if ((parentDN != null) && entryMap.containsKey(parentDN))
    {
      entryMap.put(dn, entry);
      addChangeLogEntry(request, authzDN);
      return new LDAPMessage(messageID,
           new AddResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null,
                null),
           responseControls);
    }

    // The add attempt must fail.
    return new LDAPMessage(messageID, new AddResponseProtocolOp(
         ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(dn),
         ERR_MEM_HANDLER_ADD_MISSING_PARENT.get(request.getDN(),
              dn.getParentString()),
         null));
  }



  /**
   * Attempts to process the provided bind request.  The attempt will fail if
   * any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The bind request is not a simple bind request.</LI>
   *   <LI>The bind request contains a malformed bind DN.</LI>
   *   <LI>The bind DN is not the null DN and is not the DN of any entry in the
   *       data set.</LI>
   *   <LI>The bind password is empty and the bind DN is not the null DN.</LI>
   *   <LI>The target user does not have a userPassword value that matches the
   *       provided bind password.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the bind
   *                    request.
   * @param  request    The bind request that was included in the LDAP message
   *                    that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be a
   *          {@code BindResponseProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processBindRequest(final int messageID,
                                       final BindRequestProtocolOp request,
                                       final List<Control> controls)
  {
    authenticatedDN = DN.NULL_DN;

    // Get the parsed bind DN.
    final DN bindDN;
    try
    {
      bindDN = new DN(request.getBindDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new BindResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_BIND_MALFORMED_DN.get(request.getBindDN(),
                le.getMessage()),
           null, null));
    }

    // If the bind request is for a SASL bind, then see if there is a SASL
    // mechanism handler that can be used to process it.
    if (request.getCredentialsType() == BindRequestProtocolOp.CRED_TYPE_SASL)
    {
      final String mechanism = request.getSASLMechanism();
      final InMemorySASLBindHandler handler = saslBindHandlers.get(mechanism);
      if (handler == null)
      {
        return new LDAPMessage(messageID, new BindResponseProtocolOp(
             ResultCode.AUTH_METHOD_NOT_SUPPORTED_INT_VALUE, null,
             ERR_MEM_HANDLER_SASL_MECH_NOT_SUPPORTED.get(mechanism), null,
             null));
      }

      try
      {
        final BindResult bindResult = handler.processSASLBind(this, messageID,
             bindDN, request.getSASLCredentials(), controls);
        return new LDAPMessage(messageID, new BindResponseProtocolOp(
             bindResult.getResultCode().intValue(),
             bindResult.getMatchedDN(), bindResult.getDiagnosticMessage(),
             Arrays.asList(bindResult.getReferralURLs()),
             bindResult.getServerSASLCredentials()),
             Arrays.asList(bindResult.getResponseControls()));
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
        return new LDAPMessage(messageID, new BindResponseProtocolOp(
             ResultCode.OTHER_INT_VALUE, null,
             ERR_MEM_HANDLER_SASL_BIND_FAILURE.get(
                  StaticUtils.getExceptionMessage(e)),
             null, null));
      }
    }

    // If we've gotten here, then the bind must use simple authentication.
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_BIND_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new BindResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null, null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // If the bind DN is the null DN, then the bind will be considered
    // successful as long as the password is also empty.
    final ASN1OctetString bindPassword = request.getSimplePassword();
    if (bindDN.isNullDN())
    {
      if (bindPassword.getValueLength() == 0)
      {
        if (controlMap.containsKey(AuthorizationIdentityRequestControl.
             AUTHORIZATION_IDENTITY_REQUEST_OID))
        {
          responseControls.add(new AuthorizationIdentityResponseControl("dn:"));
        }
        return new LDAPMessage(messageID,
             new BindResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null,
                  null, null, null),
             responseControls);
      }
      else
      {
        return new LDAPMessage(messageID, new BindResponseProtocolOp(
             ResultCode.INVALID_CREDENTIALS_INT_VALUE,
             getMatchedDNString(bindDN),
             ERR_MEM_HANDLER_BIND_WRONG_PASSWORD.get(request.getBindDN()), null,
             null));
      }
    }

    // If the bind DN is not null and the password is empty, then reject the
    // request.
    if ((! bindDN.isNullDN()) && (bindPassword.getValueLength() == 0))
    {
      return new LDAPMessage(messageID, new BindResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_BIND_SIMPLE_DN_WITHOUT_PASSWORD.get(), null, null));
    }

    // See if the bind DN is in the set of additional bind credentials.  If so,
    // then use the password there.
    final byte[] additionalCreds = additionalBindCredentials.get(bindDN);
    if (additionalCreds != null)
    {
      if (Arrays.equals(additionalCreds, bindPassword.getValue()))
      {
        authenticatedDN = bindDN;
        if (controlMap.containsKey(AuthorizationIdentityRequestControl.
             AUTHORIZATION_IDENTITY_REQUEST_OID))
        {
          responseControls.add(new AuthorizationIdentityResponseControl(
               "dn:" + bindDN.toString()));
        }
        return new LDAPMessage(messageID,
             new BindResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null,
                  null, null, null),
             responseControls);
      }
      else
      {
        return new LDAPMessage(messageID, new BindResponseProtocolOp(
             ResultCode.INVALID_CREDENTIALS_INT_VALUE,
             getMatchedDNString(bindDN),
             ERR_MEM_HANDLER_BIND_WRONG_PASSWORD.get(request.getBindDN()), null,
             null));
      }
    }

    // If the target user doesn't exist, then reject the request.
    final Entry userEntry = entryMap.get(bindDN);
    if (userEntry == null)
    {
      return new LDAPMessage(messageID, new BindResponseProtocolOp(
           ResultCode.INVALID_CREDENTIALS_INT_VALUE, getMatchedDNString(bindDN),
           ERR_MEM_HANDLER_BIND_NO_SUCH_USER.get(request.getBindDN()), null,
           null));
    }

    // If the user entry has a userPassword value that matches the provided
    // password, then the bind will be successful.  Otherwise, it will fail.
    if (userEntry.hasAttributeValue("userPassword", bindPassword.getValue(),
             OctetStringMatchingRule.getInstance()))
    {
      authenticatedDN = bindDN;
      if (controlMap.containsKey(AuthorizationIdentityRequestControl.
           AUTHORIZATION_IDENTITY_REQUEST_OID))
      {
        responseControls.add(new AuthorizationIdentityResponseControl(
             "dn:" + bindDN.toString()));
      }
      return new LDAPMessage(messageID,
           new BindResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null,
                null, null),
           responseControls);
    }
    else
    {
      return new LDAPMessage(messageID, new BindResponseProtocolOp(
           ResultCode.INVALID_CREDENTIALS_INT_VALUE, getMatchedDNString(bindDN),
           ERR_MEM_HANDLER_BIND_WRONG_PASSWORD.get(request.getBindDN()), null,
           null));
    }
  }



  /**
   * Attempts to process the provided compare request.  The attempt will fail if
   * any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The compare request contains a malformed target DN.</LI>
   *   <LI>The target entry does not exist.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the
   *                    compare request.
   * @param  request    The compare request that was included in the LDAP
   *                    message that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be a
   *          {@code CompareResponseProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processCompareRequest(final int messageID,
                                       final CompareRequestProtocolOp request,
                                       final List<Control> controls)
  {
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_COMPARE_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new CompareResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // Get the parsed target DN.
    final DN dn;
    try
    {
      dn = new DN(request.getDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new CompareResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_COMPARE_MALFORMED_DN.get(request.getDN(),
                le.getMessage()),
           null));
    }

    // See if the target entry or one of its superiors is a smart referral.
    if (! controlMap.containsKey(
               ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID))
    {
      final Entry referralEntry = findNearestReferral(dn);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new CompareResponseProtocolOp(
             ResultCode.REFERRAL_INT_VALUE, referralEntry.getDN(),
             INFO_MEM_HANDLER_REFERRAL_ENCOUNTERED.get(),
             getReferralURLs(dn, referralEntry)));
      }
    }

    // Get the target entry (optionally checking for the root DSE or subschema
    // subentry).  If it does not exist, then fail.
    final Entry entry;
    if (dn.isNullDN())
    {
      entry = generateRootDSE();
    }
    else if (dn.equals(subschemaSubentryDN))
    {
      entry = subschemaSubentry;
    }
    else
    {
      entry = entryMap.get(dn);
    }
    if (entry == null)
    {
      return new LDAPMessage(messageID, new CompareResponseProtocolOp(
           ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(dn),
           ERR_MEM_HANDLER_COMPARE_NO_SUCH_ENTRY.get(request.getDN()), null));
    }

    // If the request includes an assertion or proxied authorization control,
    // then perform the appropriate processing.
    try
    {
      handleAssertionRequestControl(controlMap, entry);
      handleProxiedAuthControl(controlMap);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new CompareResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // See if the entry contains the assertion value.
    final int resultCode;
    if (entry.hasAttributeValue(request.getAttributeName(),
             request.getAssertionValue().getValue()))
    {
      resultCode = ResultCode.COMPARE_TRUE_INT_VALUE;
    }
    else
    {
      resultCode = ResultCode.COMPARE_FALSE_INT_VALUE;
    }
    return new LDAPMessage(messageID,
         new CompareResponseProtocolOp(resultCode, null, null, null),
         responseControls);
  }



  /**
   * Attempts to process the provided delete request.  The attempt will fail if
   * any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The delete request contains a malformed target DN.</LI>
   *   <LI>The target entry is the root DSE.</LI>
   *   <LI>The target entry is the subschema subentry.</LI>
   *   <LI>The target entry is at or below the changelog base entry.</LI>
   *   <LI>The target entry does not exist.</LI>
   *   <LI>The target entry has one or more subordinate entries.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the delete
   *                    request.
   * @param  request    The delete request that was included in the LDAP message
   *                    that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be a
   *          {@code DeleteResponseProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processDeleteRequest(final int messageID,
                                       final DeleteRequestProtocolOp request,
                                       final List<Control> controls)
  {
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_DELETE_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // Get the parsed target DN.
    final DN dn;
    try
    {
      dn = new DN(request.getDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_DELETE_MALFORMED_DN.get(request.getDN(),
                le.getMessage()),
           null));
    }

    // See if the target entry or one of its superiors is a smart referral.
    if (! controlMap.containsKey(
               ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID))
    {
      final Entry referralEntry = findNearestReferral(dn);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
             ResultCode.REFERRAL_INT_VALUE, referralEntry.getDN(),
             INFO_MEM_HANDLER_REFERRAL_ENCOUNTERED.get(),
             getReferralURLs(dn, referralEntry)));
      }
    }

    // Make sure the target entry isn't the root DSE or schema, or a changelog
    // entry.
    if (dn.isNullDN())
    {
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_DELETE_ROOT_DSE.get(), null));
    }
    else if (dn.equals(subschemaSubentryDN))
    {
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_DELETE_SCHEMA.get(subschemaSubentryDN.toString()),
           null));
    }
    else if (dn.isDescendantOf(changeLogBaseDN, true))
    {
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_DELETE_CHANGELOG.get(request.getDN()), null));
    }

    // Get the target entry.  If it does not exist, then fail.
    final Entry entry = entryMap.get(dn);
    if (entry == null)
    {
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(dn),
           ERR_MEM_HANDLER_DELETE_NO_SUCH_ENTRY.get(request.getDN()), null));
    }

    // Create a list with the DN of the target entry, and all the DNs of its
    // subordinates.  If the entry has subordinates and the subtree delete
    // control was not provided, then fail.
    final ArrayList<DN> subordinateDNs = new ArrayList<DN>(entryMap.size());
    for (final DN mapEntryDN : entryMap.keySet())
    {
      if (mapEntryDN.isDescendantOf(dn, false))
      {
        subordinateDNs.add(mapEntryDN);
      }
    }

    if ((! subordinateDNs.isEmpty()) &&
        (! controlMap.containsKey(
               SubtreeDeleteRequestControl.SUBTREE_DELETE_REQUEST_OID)))
    {
      for (final DN mapEntryDN : entryMap.keySet())
      {
        return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
             ResultCode.NOT_ALLOWED_ON_NONLEAF_INT_VALUE, null,
             ERR_MEM_HANDLER_DELETE_HAS_SUBORDINATES.get(request.getDN()),
             null));
      }
    }

    // Handle the necessary processing for the assertion, pre-read, and proxied
    // auth controls.
    final DN authzDN;
    try
    {
      handleAssertionRequestControl(controlMap, entry);

      final PreReadResponseControl preReadResponse =
           handlePreReadControl(controlMap, entry);
      if (preReadResponse != null)
      {
        responseControls.add(preReadResponse);
      }

      authzDN = handleProxiedAuthControl(controlMap);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new DeleteResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // At this point, the entry will be removed.  However, if this will be a
    // subtree delete, then we want to delete all of its subordinates first so
    // that the changelog will show the deletes in the appropriate order.
    for (int i=(subordinateDNs.size() - 1); i >= 0; i--)
    {
      final Entry subEntry = entryMap.remove(subordinateDNs.get(i));
      addDeleteChangeLogEntry(subEntry, authzDN);
    }

    // Finally, remove the target entry and create a changelog entry for it.
    entryMap.remove(dn);
    addDeleteChangeLogEntry(entry, authzDN);

    return new LDAPMessage(messageID,
         new DeleteResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null,
              null),
         responseControls);
  }



  /**
   * Attempts to process the provided extended request, if an extended operation
   * handler is defined for the given request OID.
   *
   * @param  messageID  The message ID of the LDAP message containing the
   *                    extended request.
   * @param  request    The extended request that was included in the LDAP
   *                    message that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be an
   *          {@code ExtendedResponseProtocolOp}.
   */
  @Override()
  public LDAPMessage processExtendedRequest(final int messageID,
                          final ExtendedRequestProtocolOp request,
                          final List<Control> controls)
  {
    final String oid = request.getOID();
    final InMemoryExtendedOperationHandler handler =
         extendedRequestHandlers.get(oid);
    if (handler == null)
    {
      return new LDAPMessage(messageID, new ExtendedResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_EXTENDED_OP_NOT_SUPPORTED.get(oid), null, null,
           null));
    }

    try
    {
      final Control[] controlArray = new Control[controls.size()];
      controls.toArray(controlArray);

      final ExtendedRequest extendedRequest = new ExtendedRequest(oid,
           request.getValue(), controlArray);

      final ExtendedResult extendedResult =
           handler.processExtendedOperation(this, messageID, extendedRequest);

      return new LDAPMessage(messageID,
           new ExtendedResponseProtocolOp(
                extendedResult.getResultCode().intValue(),
                extendedResult.getMatchedDN(),
                extendedResult.getDiagnosticMessage(),
                Arrays.asList(extendedResult.getReferralURLs()),
                extendedResult.getOID(), extendedResult.getValue()),
           extendedResult.getResponseControls());
    }
    catch (final Exception e)
    {
      Debug.debugException(e);

      return new LDAPMessage(messageID, new ExtendedResponseProtocolOp(
           ResultCode.OTHER_INT_VALUE, null,
           ERR_MEM_HANDLER_EXTENDED_OP_FAILURE.get(
                StaticUtils.getExceptionMessage(e)),
           null, null, null));
    }
  }



  /**
   * Attempts to process the provided modify request.  The attempt will fail if
   * any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The modify request contains a malformed target DN.</LI>
   *   <LI>The target entry is the root DSE.</LI>
   *   <LI>The target entry is the subschema subentry.</LI>
   *   <LI>The target entry does not exist.</LI>
   *   <LI>Any of the modifications cannot be applied to the entry.</LI>
   *   <LI>If a schema was provided, and the entry violates any of the
   *       constraints of that schema.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the modify
   *                    request.
   * @param  request    The modify request that was included in the LDAP message
   *                    that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be an
   *          {@code ModifyResponseProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processModifyRequest(final int messageID,
                                       final ModifyRequestProtocolOp request,
                                       final List<Control> controls)
  {
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // Get the parsed target DN.
    final DN dn;
    try
    {
      dn = new DN(request.getDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_MALFORMED_DN.get(request.getDN(),
                le.getMessage()),
           null));
    }

    // See if the target entry or one of its superiors is a smart referral.
    if (! controlMap.containsKey(
               ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID))
    {
      final Entry referralEntry = findNearestReferral(dn);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
             ResultCode.REFERRAL_INT_VALUE, referralEntry.getDN(),
             INFO_MEM_HANDLER_REFERRAL_ENCOUNTERED.get(),
             getReferralURLs(dn, referralEntry)));
      }
    }

    // See if the target entry is the root DSE, the subschema subentry, or a
    // changelog entry.
    if (dn.isNullDN())
    {
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_ROOT_DSE.get(), null));
    }
    else if (dn.equals(subschemaSubentryDN))
    {
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_SCHEMA.get(subschemaSubentryDN.toString()),
           null));
    }
    else if (dn.isDescendantOf(changeLogBaseDN, true))
    {
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_CHANGELOG.get(request.getDN()), null));
    }

    // Get the target entry.  If it does not exist, then fail.
    final Entry entry = entryMap.get(dn);
    if (entry == null)
    {
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(dn),
           ERR_MEM_HANDLER_MOD_NO_SUCH_ENTRY.get(request.getDN()), null));
    }


    // Attempt to apply the modifications to the entry.  If successful, then a
    // copy of the entry will be returned with the modifications applied.
    final Entry modifiedEntry;
    try
    {
      modifiedEntry = Entry.applyModifications(entry,
           controlMap.containsKey(PermissiveModifyRequestControl.
                PERMISSIVE_MODIFY_REQUEST_OID),
           request.getModifications());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           le.getResultCode().intValue(), null,
           ERR_MEM_HANDLER_MOD_FAILED.get(request.getDN(), le.getMessage()),
           null));
    }

    // If a schema was provided, use it to validate the resulting entry.  Also,
    // ensure that no NO-USER-MODIFICATION attributes were targeted.
    if (entryValidator != null)
    {
      final ArrayList<String> invalidReasons = new ArrayList<String>(1);
      if (! entryValidator.entryIsValid(modifiedEntry, invalidReasons))
      {
        return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
             ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE, null,
             ERR_MEM_HANDLER_MOD_VIOLATES_SCHEMA.get(request.getDN(),
                  StaticUtils.concatenateStrings(invalidReasons)),
             null));
      }

      for (final Modification m : request.getModifications())
      {
        final Attribute a = m.getAttribute();
        final String baseName = a.getBaseName();
        final AttributeTypeDefinition at = schema.getAttributeType(baseName);
        if ((at != null) && at.isNoUserModification())
        {
          return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
               ResultCode.CONSTRAINT_VIOLATION_INT_VALUE, null,
               ERR_MEM_HANDLER_MOD_NO_USER_MOD.get(request.getDN(),
                    a.getName()), null));
        }
      }
    }


    // Perform the appropriate processing for the assertion, pre-read,
    // post-read, and proxied authorization controls.
    final DN authzDN;
    try
    {
      handleAssertionRequestControl(controlMap, entry);

      final PreReadResponseControl preReadResponse =
           handlePreReadControl(controlMap, entry);
      if (preReadResponse != null)
      {
        responseControls.add(preReadResponse);
      }

      final PostReadResponseControl postReadResponse =
           handlePostReadControl(controlMap, modifiedEntry);
      if (postReadResponse != null)
      {
        responseControls.add(postReadResponse);
      }

      authzDN = handleProxiedAuthControl(controlMap);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // Update modifiersName and modifyTimestamp.
    if (generateOperationalAttributes)
    {
      modifiedEntry.setAttribute(new Attribute("modifiersName",
           DistinguishedNameMatchingRule.getInstance(),
           authzDN.toString()));
      modifiedEntry.setAttribute(new Attribute("modifyTimestamp",
           GeneralizedTimeMatchingRule.getInstance(),
           StaticUtils.encodeGeneralizedTime(new Date())));
    }


    // Replace the entry in the map and return a success result.
    entryMap.put(dn, modifiedEntry);
    addChangeLogEntry(request, authzDN);
    return new LDAPMessage(messageID,
         new ModifyResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null, null,
              null),
         responseControls);
  }



  /**
   * Attempts to process the provided modify DN request.  The attempt will fail
   * if any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The modify DN request contains a malformed target DN, new RDN, or
   *       new superior DN.</LI>
   *   <LI>The original or new DN is that of the root DSE.</LI>
   *   <LI>The original or new DN is that of the subschema subentry.</LI>
   *   <LI>The new DN of the entry would conflict with the DN of an existing
   *       entry.</LI>
   *   <LI>The new DN of the entry would exist outside the set of defined
   *       base DNs.</LI>
   *   <LI>The new DN of the entry is not a defined base DN and does not exist
   *       immediately below an existing entry.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the modify
   *                    DN request.
   * @param  request    The modify DN request that was included in the LDAP
   *                    message that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be an
   *          {@code ModifyDNResponseProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processModifyDNRequest(final int messageID,
                                       final ModifyDNRequestProtocolOp request,
                                       final List<Control> controls)
  {
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_MODIFY_DN_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // Get the parsed target DN, new RDN, and new superior DN values.
    final DN dn;
    try
    {
      dn = new DN(request.getDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_MALFORMED_DN.get(request.getDN(),
                le.getMessage()),
           null));
    }

    final RDN newRDN;
    try
    {
      newRDN = new RDN(request.getNewRDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_MALFORMED_NEW_RDN.get(request.getDN(),
                request.getNewRDN(), le.getMessage()),
           null));
    }

    final DN newSuperiorDN;
    final String newSuperiorString = request.getNewSuperiorDN();
    if (newSuperiorString == null)
    {
      newSuperiorDN = null;
    }
    else
    {
      try
      {
        newSuperiorDN = new DN(newSuperiorString);
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);
        return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
             ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
             ERR_MEM_HANDLER_MOD_DN_MALFORMED_NEW_SUPERIOR.get(request.getDN(),
                  request.getNewSuperiorDN(), le.getMessage()),
             null));
      }
    }

    // See if the target entry or one of its superiors is a smart referral.
    if (! controlMap.containsKey(
               ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID))
    {
      final Entry referralEntry = findNearestReferral(dn);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
             ResultCode.REFERRAL_INT_VALUE, referralEntry.getDN(),
             INFO_MEM_HANDLER_REFERRAL_ENCOUNTERED.get(),
             getReferralURLs(dn, referralEntry)));
      }
    }

    // See if the target is the root DSE, the subschema subentry, or a changelog
    // entry.
    if (dn.isNullDN())
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_ROOT_DSE.get(), null));
    }
    else if (dn.equals(subschemaSubentryDN))
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_SOURCE_IS_SCHEMA.get(), null));
    }
    else if (dn.isDescendantOf(changeLogBaseDN, true))
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_SOURCE_IS_CHANGELOG.get(), null));
    }

    // Construct the new DN.
    final DN newDN;
    if (newSuperiorDN == null)
    {
      final DN originalParent = dn.getParent();
      if (originalParent == null)
      {
        newDN = new DN(newRDN);
      }
      else
      {
        newDN = new DN(newRDN, originalParent);
      }
    }
    else
    {
      newDN = new DN(newRDN, newSuperiorDN);
    }

    // If the new DN matches the old DN, then fail.
    if (newDN.equals(dn))
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_NEW_DN_SAME_AS_OLD.get(request.getDN()),
           null));
    }

    // If the new DN is below a smart referral, then fail.
    if (! controlMap.containsKey(
               ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID))
    {
      final Entry referralEntry = findNearestReferral(newDN);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
             ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, referralEntry.getDN(),
             ERR_MEM_HANDLER_MOD_DN_NEW_DN_BELOW_REFERRAL.get(request.getDN(),
                  referralEntry.getDN().toString(), newDN.toString()),
             null));
      }
    }

    // If the target entry doesn't exist, then fail.
    final Entry originalEntry = entryMap.get(dn);
    if (originalEntry == null)
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(dn),
           ERR_MEM_HANDLER_MOD_DN_NO_SUCH_ENTRY.get(request.getDN()), null));
    }

    // If the new DN matches the subschema subentry DN, then fail.
    if (newDN.equals(subschemaSubentryDN))
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_TARGET_IS_SCHEMA.get(request.getDN(),
                newDN.toString()),
           null));
    }

    // If the new DN is at or below the changelog base DN, then fail.
    if (newDN.isDescendantOf(changeLogBaseDN, true))
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.UNWILLING_TO_PERFORM_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_TARGET_IS_CHANGELOG.get(request.getDN(),
                newDN.toString()),
           null));
    }

    // If the new DN already exists, then fail.
    if (entryMap.containsKey(newDN))
    {
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE, null,
           ERR_MEM_HANDLER_MOD_DN_TARGET_ALREADY_EXISTS.get(request.getDN(),
                newDN.toString()),
           null));
    }

    // If the new DN is not a base DN and its parent does not exist, then fail.
    if (baseDNs.contains(newDN))
    {
      // The modify DN can be processed.
    }
    else
    {
      final DN newParent = newDN.getParent();
      if ((newParent != null) && entryMap.containsKey(newParent))
      {
        // The modify DN can be processed.
      }
      else
      {
        return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
             ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(newDN),
             ERR_MEM_HANDLER_MOD_DN_PARENT_DOESNT_EXIST.get(request.getDN(),
                  newDN.toString()),
             null));
      }
    }

    // Create a copy of the entry and update it to reflect the new DN (with
    // attribute value changes).
    final RDN originalRDN = dn.getRDN();
    final Entry updatedEntry = originalEntry.duplicate();
    updatedEntry.setDN(newDN);
    if (request.deleteOldRDN() && (! newRDN.equals(originalRDN)))
    {
      final String[] oldRDNNames  = originalRDN.getAttributeNames();
      final byte[][] oldRDNValues = originalRDN.getByteArrayAttributeValues();
      for (int i=0; i < oldRDNNames.length; i++)
      {
        updatedEntry.removeAttributeValue(oldRDNNames[i], oldRDNValues[i]);
      }

      final String[] newRDNNames  = newRDN.getAttributeNames();
      final byte[][] newRDNValues = newRDN.getByteArrayAttributeValues();
      for (int i=0; i < newRDNNames.length; i++)
      {
        final MatchingRule matchingRule =
             MatchingRule.selectEqualityMatchingRule(newRDNNames[i], schema);
        updatedEntry.addAttribute(new Attribute(newRDNNames[i], matchingRule,
             newRDNValues[i]));
      }
    }

    // If a schema was provided, then make sure the updated entry conforms to
    // the schema.  Also, reject the attempt if any of the new RDN attributes
    // is marked with NO-USER-MODIFICATION.
    if (entryValidator != null)
    {
      final ArrayList<String> invalidReasons = new ArrayList<String>(1);
      if (! entryValidator.entryIsValid(updatedEntry, invalidReasons))
      {
        return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
             ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE, null,
             ERR_MEM_HANDLER_MOD_DN_VIOLATES_SCHEMA.get(request.getDN(),
                  StaticUtils.concatenateStrings(invalidReasons)),
             null));
      }

      final String[] oldRDNNames = originalRDN.getAttributeNames();
      for (int i=0; i < oldRDNNames.length; i++)
      {
        final String name = oldRDNNames[i];
        final AttributeTypeDefinition at = schema.getAttributeType(name);
        if ((at != null) && at.isNoUserModification())
        {
          final byte[] value = originalRDN.getByteArrayAttributeValues()[i];
          if (! updatedEntry.hasAttributeValue(name, value))
          {
            return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
                 ResultCode.CONSTRAINT_VIOLATION_INT_VALUE, null,
                 ERR_MEM_HANDLER_MOD_DN_NO_USER_MOD.get(request.getDN(),
                      name), null));
          }
        }
      }

      final String[] newRDNNames = newRDN.getAttributeNames();
      for (int i=0; i < newRDNNames.length; i++)
      {
        final String name = newRDNNames[i];
        final AttributeTypeDefinition at = schema.getAttributeType(name);
        if ((at != null) && at.isNoUserModification())
        {
          final byte[] value = newRDN.getByteArrayAttributeValues()[i];
          if (! originalEntry.hasAttributeValue(name, value))
          {
            return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
                 ResultCode.CONSTRAINT_VIOLATION_INT_VALUE, null,
                 ERR_MEM_HANDLER_MOD_DN_NO_USER_MOD.get(request.getDN(),
                      name), null));
          }
        }
      }
    }

    // Perform the appropriate processing for the assertion, pre-read,
    // post-read, and proxied authorization controls
    // Perform the appropriate processing for the assertion, pre-read,
    // post-read, and proxied authorization controls.
    final DN authzDN;
    try
    {
      handleAssertionRequestControl(controlMap, originalEntry);

      final PreReadResponseControl preReadResponse =
           handlePreReadControl(controlMap, originalEntry);
      if (preReadResponse != null)
      {
        responseControls.add(preReadResponse);
      }

      final PostReadResponseControl postReadResponse =
           handlePostReadControl(controlMap, updatedEntry);
      if (postReadResponse != null)
      {
        responseControls.add(postReadResponse);
      }

      authzDN = handleProxiedAuthControl(controlMap);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new ModifyDNResponseProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // Update the modifiersName, modifyTimestamp, and entryDN operational
    // attributes.
    if (generateOperationalAttributes)
    {
      updatedEntry.setAttribute(new Attribute("modifiersName",
           DistinguishedNameMatchingRule.getInstance(),
           authzDN.toString()));
      updatedEntry.setAttribute(new Attribute("modifyTimestamp",
           GeneralizedTimeMatchingRule.getInstance(),
           StaticUtils.encodeGeneralizedTime(new Date())));
      updatedEntry.setAttribute(new Attribute("entryDN",
           DistinguishedNameMatchingRule.getInstance(),
           newDN.toNormalizedString()));
    }

    // Remove the old entry and add the new one.
    entryMap.remove(dn);
    entryMap.put(newDN, updatedEntry);

    // If the target entry had any subordinates, then rename them as well.
    final RDN[] oldDNComps = dn.getRDNs();
    final RDN[] newDNComps = newDN.getRDNs();
    final Set<DN> dnSet = new LinkedHashSet<DN>(entryMap.keySet());
    for (final DN mapEntryDN : dnSet)
    {
      if (mapEntryDN.isDescendantOf(dn, false))
      {
        final Entry e = entryMap.remove(mapEntryDN);

        final RDN[] oldMapEntryComps = mapEntryDN.getRDNs();
        final int compsToSave = oldMapEntryComps.length - oldDNComps.length ;

        final RDN[] newMapEntryComps = new RDN[compsToSave + newDNComps.length];
        System.arraycopy(oldMapEntryComps, 0, newMapEntryComps, 0,
             compsToSave);
        System.arraycopy(newDNComps, 0, newMapEntryComps, compsToSave,
             newDNComps.length);

        final DN newMapEntryDN = new DN(newMapEntryComps);
        e.setDN(newMapEntryDN);
        if (generateOperationalAttributes)
        {
          e.setAttribute(new Attribute("entryDN",
               DistinguishedNameMatchingRule.getInstance(),
               newMapEntryDN.toNormalizedString()));
        }
        entryMap.put(newMapEntryDN, e);
      }
    }

    addChangeLogEntry(request, authzDN);
    return new LDAPMessage(messageID,
         new ModifyDNResponseProtocolOp(ResultCode.SUCCESS_INT_VALUE, null,
              null, null),
         responseControls);
  }



  /**
   * Attempts to process the provided search request.  The attempt will fail
   * if any of the following conditions is true:
   * <UL>
   *   <LI>There is a problem with any of the request controls.</LI>
   *   <LI>The modify DN request contains a malformed target DN, new RDN, or
   *       new superior DN.</LI>
   *   <LI>The new DN of the entry would conflict with the DN of an existing
   *       entry.</LI>
   *   <LI>The new DN of the entry would exist outside the set of defined
   *       base DNs.</LI>
   *   <LI>The new DN of the entry is not a defined base DN and does not exist
   *       immediately below an existing entry.</LI>
   * </UL>
   *
   * @param  messageID  The message ID of the LDAP message containing the search
   *                    request.
   * @param  request    The search request that was included in the LDAP message
   *                    that was received.
   * @param  controls   The set of controls included in the LDAP message.  It
   *                    may be empty if there were no controls, but will not be
   *                    {@code null}.
   *
   * @return  The {@link LDAPMessage} containing the response to send to the
   *          client.  The protocol op in the {@code LDAPMessage} must be an
   *          {@code SearchResultDoneProtocolOp}.
   */
  @Override()
  public synchronized LDAPMessage processSearchRequest(final int messageID,
                                       final SearchRequestProtocolOp request,
                                       final List<Control> controls)
  {
    // Process the provided request controls.
    final Map<String,Control> controlMap;
    try
    {
      controlMap = RequestControlPreProcessor.processControls(
           LDAPMessage.PROTOCOL_OP_TYPE_SEARCH_REQUEST, controls);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }
    final ArrayList<Control> responseControls = new ArrayList<Control>(1);

    // Get the parsed base DN.
    final DN baseDN;
    try
    {
      baseDN = new DN(request.getBaseDN());
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
           ResultCode.INVALID_DN_SYNTAX_INT_VALUE, null,
           ERR_MEM_HANDLER_SEARCH_MALFORMED_BASE.get(request.getBaseDN(),
                le.getMessage()),
                null));
    }

    // See if the search base or one of its superiors is a smart referral.
    final boolean hasManageDsaIT = controlMap.containsKey(
         ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID);
    if (! hasManageDsaIT)
    {
      final Entry referralEntry = findNearestReferral(baseDN);
      if (referralEntry != null)
      {
        return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
             ResultCode.REFERRAL_INT_VALUE, referralEntry.getDN(),
             INFO_MEM_HANDLER_REFERRAL_ENCOUNTERED.get(),
             getReferralURLs(baseDN, referralEntry)));
      }
    }

    // Make sure that the base entry exists.  It may be the root DSE or
    // subschema subentry.
    final Entry baseEntry;
    boolean includeChangeLog = true;
    if (baseDN.isNullDN())
    {
      baseEntry = generateRootDSE();
      includeChangeLog = false;
    }
    else if (baseDN.equals(subschemaSubentryDN))
    {
      baseEntry = subschemaSubentry;
    }
    else
    {
      baseEntry = entryMap.get(baseDN);
    }

    if (baseEntry == null)
    {
      return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
           ResultCode.NO_SUCH_OBJECT_INT_VALUE, getMatchedDNString(baseDN),
           ERR_MEM_HANDLER_SEARCH_BASE_DOES_NOT_EXIST.get(request.getBaseDN()),
           null));
    }

    // Perform any necessary processing for the assertion and proxied auth
    // controls.
    try
    {
      handleAssertionRequestControl(controlMap, baseEntry);
      handleProxiedAuthControl(controlMap);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
           le.getResultCode().intValue(), null, le.getMessage(), null));
    }

    // Process the set of requested attributes.
    final AtomicBoolean allUserAttrs = new AtomicBoolean(false);
    final AtomicBoolean allOpAttrs = new AtomicBoolean(false);
    final Map<String,List<List<String>>> returnAttrs =
         processRequestedAttributes(request.getAttributes(), allUserAttrs,
              allOpAttrs);

    // Check the scope.  If it is a base-level search, then we only need to
    // examine the base entry.  Otherwise, we'll have to scan the entire entry
    // map.
    final Filter filter = request.getFilter();
    final SearchScope scope = request.getScope();
    final AtomicInteger entriesSent = new AtomicInteger(0);
    final boolean includeSubEntries = ((scope == SearchScope.BASE) ||
         controlMap.containsKey(
              SubentriesRequestControl.SUBENTRIES_REQUEST_OID));
    if (scope == SearchScope.BASE)
    {
      try
      {
        if (filter.matchesEntry(baseEntry, schema))
        {
          try
          {
            returnEntry(messageID, baseEntry, allUserAttrs.get(),
                 allOpAttrs.get(), returnAttrs, includeSubEntries,
                 includeChangeLog, hasManageDsaIT, entriesSent,
                 request.getSizeLimit());
          }
          catch (final LDAPException le)
          {
            Debug.debugException(le);

            return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
                 le.getResultCode().intValue(), le.getMatchedDN(),
                 le.getMessage(), null));
          }
        }
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      return new LDAPMessage(messageID,
           new SearchResultDoneProtocolOp(ResultCode.SUCCESS_INT_VALUE, null,
                null, null),
           responseControls);
    }

    // If the search uses a single-level scope and the base DN is the root DSE,
    // then we will only examine the defined base entries for the data set.
    if ((scope == SearchScope.ONE) && baseDN.isNullDN())
    {
      for (final DN dn : baseDNs)
      {
        final Entry e = entryMap.get(dn);
        if (e != null)
        {
          try
          {
            if (filter.matchesEntry(e, schema))
            {
              try
              {
                returnEntry(messageID, e, allUserAttrs.get(), allOpAttrs.get(),
                     returnAttrs, includeSubEntries, includeChangeLog,
                     hasManageDsaIT, entriesSent, request.getSizeLimit());
              }
              catch (final LDAPException le)
              {
                Debug.debugException(le);

                return new LDAPMessage(messageID,
                     new SearchResultDoneProtocolOp(
                          le.getResultCode().intValue(), le.getMatchedDN(),
                          le.getMessage(), null));
              }
            }
          }
          catch (final Exception ex)
          {
            Debug.debugException(ex);
          }
        }
      }

      return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
           ResultCode.SUCCESS_INT_VALUE, null, null, null));
    }

    // Iterate through the map to find and return entries matching the criteria.
    // It is not necessary to consider the root DSE for non-base scopes.
    for (final Map.Entry<DN,Entry> me : entryMap.entrySet())
    {
      final DN dn = me.getKey();
      final Entry entry = me.getValue();
      try
      {
        if (dn.matchesBaseAndScope(baseDN, scope) &&
            filter.matchesEntry(entry, schema))
        {
          try
          {
            returnEntry(messageID, entry, allUserAttrs.get(), allOpAttrs.get(),
                 returnAttrs, includeSubEntries, includeChangeLog,
                 hasManageDsaIT, entriesSent, request.getSizeLimit());
          }
          catch (final LDAPException le)
          {
            Debug.debugException(le);

            return new LDAPMessage(messageID, new SearchResultDoneProtocolOp(
                 le.getResultCode().intValue(), le.getMatchedDN(),
                 le.getMessage(), null));
          }
        }
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }
    }

    return new LDAPMessage(messageID,
         new SearchResultDoneProtocolOp(ResultCode.SUCCESS_INT_VALUE, null,
              null, null),
         responseControls);
  }



  /**
   * Retrieves the number of entries currently held in the server.
   *
   * @return  The number of entries currently held in the server.
   */
  public synchronized int countEntries()
  {
    return entryMap.size();
  }



  /**
   * Removes all entries currently held in the server.
   */
  public synchronized void clear()
  {
    entryMap.clear();
  }



  /**
   * Adds all entries obtained from the provided LDIF reader to the server.   If
   * an error is encountered during processing, then the contents of the server
   * will be the same as they were before this method was called.
   *
   * @param  clear       Indicates whether to clear the contents of the server
   *                     prior to adding all entries read from LDIF.
   * @param  ldifReader  The LDIF reader from which to read the entries to use
   *                     to populate the server.  It must not be {@code null}.
   *                     It will be closed by this method.
   *
   * @return  The number of entries read from LDIF.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         populate the server with entries from the specified
   *                         LDIF file.
   */
  public synchronized int initializeFromLDIF(final boolean clear,
                                             final LDIFReader ldifReader)
         throws LDAPException
  {
    final HashMap<DN,Entry> originalEntryMap = new HashMap<DN,Entry>(entryMap);
    boolean restoreOriginalEntryMap = true;

    try
    {
      if (clear)
      {
        entryMap.clear();
      }

      int entriesAdded = 0;
      while (true)
      {
        final Entry entry;
        try
        {
          entry = ldifReader.readEntry();
          if (entry == null)
          {
            restoreOriginalEntryMap = false;
            return entriesAdded;
          }
        }
        catch (final LDIFException le)
        {
          Debug.debugException(le);
          throw new LDAPException(ResultCode.LOCAL_ERROR,
               ERR_MEM_HANDLER_INIT_FROM_LDIF_READ_ERROR.get(le.getMessage()),
               le);
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          throw new LDAPException(ResultCode.LOCAL_ERROR,
               ERR_MEM_HANDLER_INIT_FROM_LDIF_READ_ERROR.get(
                    StaticUtils.getExceptionMessage(e)),
               e);
        }

        addEntry(entry, true);
        entriesAdded++;
      }
    }
    finally
    {
      try
      {
        ldifReader.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }

      if (restoreOriginalEntryMap)
      {
        entryMap.clear();
        entryMap.putAll(originalEntryMap);
      }
    }
  }



  /**
   * Writes all entries contained in the server to LDIF using the provided
   * writer.
   *
   * @param  ldifWriter             The LDIF writer to use when writing the
   *                                entries.  It must not be {@code null}.
   * @param  excludeGeneratedAttrs  Indicates whether to exclude automatically
   *                                generated operational attributes like
   *                                entryUUID, entryDN, creatorsName, etc.
   * @param  excludeChangeLog       Indicates whether to exclude entries
   *                                contained in the changelog.
   * @param  closeWriter            Indicates whether the LDIF writer should be
   *                                closed after all entries have been written.
   *
   * @return  The number of entries written to LDIF.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         write an entry to LDIF.
   */
  public synchronized int writeToLDIF(final LDIFWriter ldifWriter,
                                      final boolean excludeGeneratedAttrs,
                                      final boolean excludeChangeLog,
                                      final boolean closeWriter)
         throws LDAPException
  {
    boolean exceptionThrown = false;

    try
    {
      int entriesWritten = 0;

      for (final Map.Entry<DN,Entry> me : entryMap.entrySet())
      {
        final DN dn = me.getKey();
        if (excludeChangeLog && dn.isDescendantOf(changeLogBaseDN, true))
        {
          continue;
        }

        final Entry entry;
        if (excludeGeneratedAttrs)
        {
          entry = me.getValue().duplicate();
          entry.removeAttribute("entryDN");
          entry.removeAttribute("entryUUID");
          entry.removeAttribute("subschemaSubentry");
          entry.removeAttribute("creatorsName");
          entry.removeAttribute("createTimestamp");
          entry.removeAttribute("modifiersName");
          entry.removeAttribute("modifyTimestamp");
        }
        else
        {
          entry = me.getValue();
        }

        try
        {
          ldifWriter.writeEntry(entry);
          entriesWritten++;
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          exceptionThrown = true;
          throw new LDAPException(ResultCode.LOCAL_ERROR,
               ERR_MEM_HANDLER_LDIF_WRITE_ERROR.get(entry.getDN(),
                    StaticUtils.getExceptionMessage(e)),
               e);
        }
      }

      return entriesWritten;
    }
    finally
    {
      if (closeWriter)
      {
        try
        {
          ldifWriter.close();
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          if (! exceptionThrown)
          {
            throw new LDAPException(ResultCode.LOCAL_ERROR,
                 ERR_MEM_HANDLER_LDIF_WRITE_CLOSE_ERROR.get(
                      StaticUtils.getExceptionMessage(e)),
                 e);
          }
        }
      }
    }
  }



  /**
   * Attempts to add the provided entry to the in-memory data set.  The attempt
   * will fail if any of the following conditions is true:
   * <UL>
   *   <LI>The provided entry has a malformed DN.</LI>
   *   <LI>The provided entry has the null DN.</LI>
   *   <LI>The provided entry has a DN that is the same as or subordinate to the
   *       subschema subentry.</LI>
   *   <LI>An entry already exists with the same DN as the entry in the provided
   *       request.</LI>
   *   <LI>The entry is outside the set of base DNs for the server.</LI>
   *   <LI>The entry is below one of the defined base DNs but the immediate
   *       parent entry does not exist.</LI>
   *   <LI>If a schema was provided, and the entry is not valid according to the
   *       constraints of that schema.</LI>
   * </UL>
   *
   * @param  entry                     The entry to be added.  It must not be
   *                                   {@code null}.
   * @param  ignoreNoUserModification  Indicates whether to ignore constraints
   *                                   normally imposed by the
   *                                   NO-USER-MODIFICATION element in attribute
   *                                   type definitions.
   *
   * @throws  LDAPException  If a problem occurs while attempting to add the
   *                         provided entry.
   */
  public void addEntry(final Entry entry,
                       final boolean ignoreNoUserModification)
         throws LDAPException
  {
    final List<Control> controls;
    if (ignoreNoUserModification)
    {
      controls = new ArrayList<Control>(1);
      controls.add(new Control("1.3.6.1.4.1.30221.2.5.5", false));
    }
    else
    {
      controls = Collections.emptyList();
    }

    final AddRequestProtocolOp addRequest = new AddRequestProtocolOp(
         entry.getDN(), new ArrayList<Attribute>(entry.getAttributes()));

    final LDAPMessage resultMessage =
         processAddRequest(-1, addRequest, controls);

    final AddResponseProtocolOp addResponse =
         resultMessage.getAddResponseProtocolOp();
    if (addResponse.getResultCode() != ResultCode.SUCCESS_INT_VALUE)
    {
      throw new LDAPException(ResultCode.valueOf(addResponse.getResultCode()),
           addResponse.getDiagnosticMessage(), addResponse.getMatchedDN(),
           stringListToArray(addResponse.getReferralURLs()));
    }
  }



  /**
   * Attempts to add all of the provided entries to the server.  If an error is
   * encountered during processing, then the contents of the server will be the
   * same as they were before this method was called.
   *
   * @param  entries  The collection of entries to be added.
   *
   * @throws  LDAPException  If a problem was encountered while attempting to
   *                         add any of the entries to the server.
   */
  public synchronized void addEntries(final List<? extends Entry> entries)
         throws LDAPException
  {
    final HashMap<DN,Entry> originalEntryMap = new HashMap<DN,Entry>(entryMap);
    boolean restoreOriginalEntryMap = true;

    try
    {
      for (final Entry e : entries)
      {
        addEntry(e, false);
      }
      restoreOriginalEntryMap = false;
    }
    finally
    {
      if (restoreOriginalEntryMap)
      {
        entryMap.clear();
        entryMap.putAll(originalEntryMap);
      }
    }
  }



  /**
   * Attempts to delete the specified entry.  The attempt will fail if
   * any of the following conditions is true:
   * <UL>
   *   <LI>The provided entry DN is malformed.</LI>
   *   <LI>The target entry is the root DSE.</LI>
   *   <LI>The target entry is the subschema subentry.</LI>
   *   <LI>The target entry does not exist.</LI>
   *   <LI>The target entry has one or more subordinate entries.</LI>
   * </UL>
   *
   * @param  dn  The DN of the entry to remove.  It must not be {@code null}.
   *
   * @throws  LDAPException  If a problem occurs while attempting to delete the
   *                         specified entry.
   */
  public void deleteEntry(final String dn)
         throws LDAPException
  {
    final DeleteRequestProtocolOp deleteRequest =
         new DeleteRequestProtocolOp(dn);

    final LDAPMessage resultMessage = processDeleteRequest(-1, deleteRequest,
         Collections.<Control>emptyList());

    final DeleteResponseProtocolOp deleteResponse =
         resultMessage.getDeleteResponseProtocolOp();
    if (deleteResponse.getResultCode() != ResultCode.SUCCESS_INT_VALUE)
    {
      throw new LDAPException(
           ResultCode.valueOf(deleteResponse.getResultCode()),
           deleteResponse.getDiagnosticMessage(), deleteResponse.getMatchedDN(),
           stringListToArray(deleteResponse.getReferralURLs()));
    }
  }



  /**
   * Removes the entry with the specified DN and any subordinate entries it may
   * have.
   *
   * @param  baseDN  The DN of the entry to be deleted.  It must not be
   *                 {@code null} or represent the null DN.
   *
   * @return  The number of entries actually removed, or zero if the specified
   *          base DN does not represent an entry in the server.
   *
   * @throws  LDAPException  If the provided base DN is not a valid DN, or is
   *                         the DN of an entry that cannot be deleted (e.g.,
   *                         the null DN).
   */
  public synchronized int deleteSubtree(final String baseDN)
         throws LDAPException
  {
    final DN dn = new DN(baseDN);
    if (dn.isNullDN())
    {
      throw new LDAPException(ResultCode.UNWILLING_TO_PERFORM,
           ERR_MEM_HANDLER_DELETE_ROOT_DSE.get());
    }

    int numDeleted = 0;

    final Iterator<Map.Entry<DN,Entry>> iterator =
         entryMap.entrySet().iterator();
    while (iterator.hasNext())
    {
      final Map.Entry<DN,Entry> e = iterator.next();
      if (e.getKey().isDescendantOf(dn, true))
      {
        iterator.remove();
        numDeleted++;
      }
    }

    return numDeleted;
  }



  /**
   * Attempts to apply the provided set of modifications to the specified entry.
   * The attempt will fail if any of the following conditions is true:
   * <UL>
   *   <LI>The target DN is malformed.</LI>
   *   <LI>The target entry is the root DSE.</LI>
   *   <LI>The target entry is the subschema subentry.</LI>
   *   <LI>The target entry does not exist.</LI>
   *   <LI>Any of the modifications cannot be applied to the entry.</LI>
   *   <LI>If a schema was provided, and the entry violates any of the
   *       constraints of that schema.</LI>
   * </UL>
   *
   * @param  dn    The DN of the entry to be modified.
   * @param  mods  The set of modifications to be applied to the entry.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         update the specified entry.
   */
  public void modifyEntry(final String dn, final List<Modification> mods)
         throws LDAPException
  {
    final ModifyRequestProtocolOp modifyRequest =
         new ModifyRequestProtocolOp(dn, mods);

    final LDAPMessage resultMessage = processModifyRequest(-1, modifyRequest,
         Collections.<Control>emptyList());

    final ModifyResponseProtocolOp modifyResponse =
         resultMessage.getModifyResponseProtocolOp();
    if (modifyResponse.getResultCode() != ResultCode.SUCCESS_INT_VALUE)
    {
      throw new LDAPException(
           ResultCode.valueOf(modifyResponse.getResultCode()),
           modifyResponse.getDiagnosticMessage(), modifyResponse.getMatchedDN(),
           stringListToArray(modifyResponse.getReferralURLs()));
    }
  }



  /**
   * Retrieves a read-only representation the entry with the specified DN, if
   * it exists.
   *
   * @param  dn  The DN of the entry to retrieve.
   *
   * @return  The requested entry, or {@code null} if no entry exists with the
   *          given DN.
   *
   * @throws  LDAPException  If the provided DN is malformed.
   */
  public synchronized ReadOnlyEntry getEntry(final String dn)
         throws LDAPException
  {
    return getEntry(new DN(dn));
  }



  /**
   * Retrieves a read-only representation the entry with the specified DN, if
   * it exists.
   *
   * @param  dn  The DN of the entry to retrieve.
   *
   * @return  The requested entry, or {@code null} if no entry exists with the
   *          given DN.
   */
  public synchronized ReadOnlyEntry getEntry(final DN dn)
  {
    if (dn.isNullDN())
    {
      return generateRootDSE();
    }
    else if (dn.equals(subschemaSubentryDN))
    {
      return subschemaSubentry;
    }
    else
    {
      final Entry e = entryMap.get(dn);
      if (e == null)
      {
        return null;
      }
      else
      {
        return new ReadOnlyEntry(e);
      }
    }
  }



  /**
   * Retrieves a list of all entries in the server which match the given
   * search criteria.
   *
   * @param  baseDN  The base DN to use for the search.  It must not be
   *                 {@code null}.
   * @param  scope   The scope to use for the search.  It must not be
   *                 {@code null}.
   * @param  filter  The filter to use for the search.  It must not be
   *                 {@code null}.
   *
   * @return  A list of the entries that matched the provided search criteria.
   *
   * @throws  LDAPException  If a problem is encountered while performing the
   *                         search.
   */
  public synchronized List<ReadOnlyEntry> search(final String baseDN,
                                                 final SearchScope scope,
                                                 final Filter filter)
         throws LDAPException
  {
    final DN parsedDN;
    try
    {
      parsedDN = new DN(baseDN);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
           ERR_MEM_HANDLER_SEARCH_MALFORMED_BASE.get(baseDN, le.getMessage()),
           le);
    }

    final ReadOnlyEntry baseEntry;
    if (parsedDN.isNullDN())
    {
      baseEntry = generateRootDSE();
    }
    else if (parsedDN.equals(subschemaSubentryDN))
    {
      baseEntry = subschemaSubentry;
    }
    else
    {
      final Entry e = entryMap.get(parsedDN);
      if (e == null)
      {
        throw new LDAPException(ResultCode.NO_SUCH_OBJECT,
             ERR_MEM_HANDLER_SEARCH_BASE_DOES_NOT_EXIST.get(baseDN),
             getMatchedDNString(parsedDN), null);
      }

      baseEntry = new ReadOnlyEntry(e);
    }

    if (scope == SearchScope.BASE)
    {
      final List<ReadOnlyEntry> entryList = new ArrayList<ReadOnlyEntry>(1);

      try
      {
        if (filter.matchesEntry(baseEntry, schema))
        {
          entryList.add(baseEntry);
        }
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);
      }

      return Collections.unmodifiableList(entryList);
    }

    if ((scope == SearchScope.ONE) && parsedDN.isNullDN())
    {
      final List<ReadOnlyEntry> entryList =
           new ArrayList<ReadOnlyEntry>(baseDNs.size());

      try
      {
        for (final DN dn : baseDNs)
        {
          final Entry e = entryMap.get(dn);
          if ((e != null) && filter.matchesEntry(e, schema))
          {
            entryList.add(new ReadOnlyEntry(e));
          }
        }
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);
      }

      return Collections.unmodifiableList(entryList);
    }

    final List<ReadOnlyEntry> entryList = new ArrayList<ReadOnlyEntry>(10);
    for (final Map.Entry<DN,Entry> me : entryMap.entrySet())
    {
      final DN dn = me.getKey();
      if (dn.matchesBaseAndScope(parsedDN, scope))
      {
        // We don't want to return changelog entries searches based at the
        // root DSE.
        if (parsedDN.isNullDN() && dn.isDescendantOf(changeLogBaseDN, true))
        {
          continue;
        }

        try
        {
          final Entry entry = me.getValue();
          if (filter.matchesEntry(entry, schema))
          {
            entryList.add(new ReadOnlyEntry(entry));
          }
        }
        catch (final LDAPException le)
        {
          Debug.debugException(le);
        }
      }
    }

    return Collections.unmodifiableList(entryList);
  }



  /**
   * Parses the provided set of strings as DNs.
   *
   * @param  dnStrings  The array of strings to be parsed as DNs.
   *
   * @return  The array of parsed DNs.
   *
   * @throws  LDAPException  If any of the provided strings cannot be parsed as
   *                         DNs.
   */
  static DN[] parseDNs(final String... dnStrings)
         throws LDAPException
  {
    if (dnStrings == null)
    {
      return null;
    }

    final DN[] dns = new DN[dnStrings.length];
    for (int i=0; i < dns.length; i++)
    {
      dns[i] = new DN(dnStrings[i]);
    }
    return dns;
  }



  /**
   * Generates an entry to use as the server root DSE.
   *
   * @return  The generated root DSE entry.
   */
  private ReadOnlyEntry generateRootDSE()
  {
    final Entry rootDSEEntry = new Entry(DN.NULL_DN);
    rootDSEEntry.addAttribute("objectClass", "top", "ds-root-dse");
    rootDSEEntry.addAttribute(new Attribute("supportedLDAPVersion",
         IntegerMatchingRule.getInstance(), "3"));
    rootDSEEntry.addAttribute("vendorName", "UnboundID Corp.");
    rootDSEEntry.addAttribute("vendorVersion", Version.FULL_VERSION_STRING);
    rootDSEEntry.addAttribute(new Attribute("subschemaSubentry",
         DistinguishedNameMatchingRule.getInstance(),
         subschemaSubentryDN.toString()));
    rootDSEEntry.addAttribute(new Attribute("entryDN",
         DistinguishedNameMatchingRule.getInstance(), ""));
    rootDSEEntry.addAttribute("entryUUID", UUID.randomUUID().toString());

    rootDSEEntry.addAttribute("supportedFeatures",
         "1.3.6.1.4.1.4203.1.5.1",  // All operational attributes
         "1.3.6.1.4.1.4203.1.5.2",  // Request attributes by object class
         "1.3.6.1.4.1.4203.1.5.3",  // LDAP absolute true and false filters
         "1.3.6.1.1.14");           // Increment modification type

    final TreeSet<String> ctlSet = new TreeSet<String>();

    ctlSet.add(AssertionRequestControl.ASSERTION_REQUEST_OID);
    ctlSet.add(AuthorizationIdentityRequestControl.
         AUTHORIZATION_IDENTITY_REQUEST_OID);
    ctlSet.add(ManageDsaITRequestControl.MANAGE_DSA_IT_REQUEST_OID);
    ctlSet.add(PermissiveModifyRequestControl.PERMISSIVE_MODIFY_REQUEST_OID);
    ctlSet.add(PostReadRequestControl.POST_READ_REQUEST_OID);
    ctlSet.add(PreReadRequestControl.PRE_READ_REQUEST_OID);
    ctlSet.add(ProxiedAuthorizationV1RequestControl.
         PROXIED_AUTHORIZATION_V1_REQUEST_OID);
    ctlSet.add(ProxiedAuthorizationV2RequestControl.
         PROXIED_AUTHORIZATION_V2_REQUEST_OID);
    ctlSet.add(SubentriesRequestControl.SUBENTRIES_REQUEST_OID);
    ctlSet.add(SubtreeDeleteRequestControl.SUBTREE_DELETE_REQUEST_OID);

    final String[] controlOIDs = new String[ctlSet.size()];
    rootDSEEntry.addAttribute("supportedControl", ctlSet.toArray(controlOIDs));


    if (! extendedRequestHandlers.isEmpty())
    {
      final String[] oidArray = new String[extendedRequestHandlers.size()];
      rootDSEEntry.addAttribute("supportedExtension",
           extendedRequestHandlers.keySet().toArray(oidArray));
    }

    if (! saslBindHandlers.isEmpty())
    {
      final String[] mechanismArray = new String[saslBindHandlers.size()];
      rootDSEEntry.addAttribute("supportedSASLMechanisms",
           saslBindHandlers.keySet().toArray(mechanismArray));
    }

    int pos = 0;
    final String[] baseDNStrings = new String[baseDNs.size()];
    for (final DN baseDN : baseDNs)
    {
      baseDNStrings[pos++] = baseDN.toString();
    }
    rootDSEEntry.addAttribute(new Attribute("namingContexts",
         DistinguishedNameMatchingRule.getInstance(), baseDNStrings));

    if (maxChangelogEntries > 0)
    {
      rootDSEEntry.addAttribute(new Attribute("changeLog",
           DistinguishedNameMatchingRule.getInstance(),
           changeLogBaseDN.toString()));
      rootDSEEntry.addAttribute(new Attribute("firstChangeNumber",
           IntegerMatchingRule.getInstance(), firstChangeNumber.toString()));
      rootDSEEntry.addAttribute(new Attribute("lastChangeNumber",
           IntegerMatchingRule.getInstance(), lastChangeNumber.toString()));
    }

    return new ReadOnlyEntry(rootDSEEntry);
  }



  /**
   * Generates a subschema subentry from the provided schema object.
   *
   * @param  schema  The schema to use to generate the subschema subentry.  It
   *                 may be {@code null} if a minimal default entry should be
   *                 generated.
   *
   * @return  The generated subschema subentry.
   */
  private static ReadOnlyEntry generateSubschemaSubentry(final Schema schema)
  {
    final Entry e;

    if (schema == null)
    {
      e = new Entry("cn=schema");

      e.addAttribute("objectClass", "namedObject", "ldapSubEntry",
           "subschema");
      e.addAttribute("cn", "schema");
    }
    else
    {
      e = schema.getSchemaEntry().duplicate();
    }

    try
    {
      e.addAttribute("entryDN", DN.normalize(e.getDN()));
    }
    catch (final LDAPException le)
    {
      // This should never happen.
      Debug.debugException(le);
      e.setAttribute("entryDN", StaticUtils.toLowerCase(e.getDN()));
    }


    e.addAttribute("entryUUID", UUID.randomUUID().toString());
    return new ReadOnlyEntry(e);
  }



  /**
   * Processes the set of requested attributes from the given search request.
   *
   * @param  attrList      The list of requested attributes to examine.
   * @param  allUserAttrs  Indicates whether to return all user attributes.  It
   *                       should have an initial value of {@code false}.
   * @param  allOpAttrs    Indicates whether to return all operational
   *                       attributes.  It should have an initial value of
   *                       {@code false}.
   *
   * @return  A map of specific attribute types to be returned.  The keys of the
   *          map will be the lowercase OID and names of the attribute types,
   *          and the values will be a list of option sets for the associated
   *          attribute type.
   */
  private Map<String,List<List<String>>> processRequestedAttributes(
               final List<String> attrList, final AtomicBoolean allUserAttrs,
               final AtomicBoolean allOpAttrs)
  {
    if (attrList.isEmpty())
    {
      allUserAttrs.set(true);
      return Collections.emptyMap();
    }

    final HashMap<String,List<List<String>>> m =
         new HashMap<String,List<List<String>>>(attrList.size() * 2);
    for (final String s : attrList)
    {
      if (s.equals("*"))
      {
        // All user attributes.
        allUserAttrs.set(true);
      }
      else if (s.equals("+"))
      {
        // All operational attributes.
        allOpAttrs.set(true);
      }
      else if (s.startsWith("@"))
      {
        // Return attributes by object class.  This can only be supported if a
        // schema has been defined.
        if (schema != null)
        {
          final String ocName = s.substring(1);
          final ObjectClassDefinition oc = schema.getObjectClass(ocName);
          if (oc != null)
          {
            for (final AttributeTypeDefinition at :
                 oc.getRequiredAttributes(schema, true))
            {
              addAttributeOIDAndNames(at, m, Collections.<String>emptyList());
            }
            for (final AttributeTypeDefinition at :
                 oc.getOptionalAttributes(schema, true))
            {
              addAttributeOIDAndNames(at, m, Collections.<String>emptyList());
            }
          }
        }
      }
      else
      {
        final ObjectPair<String,List<String>> nameWithOptions =
             getNameWithOptions(s);
        if (nameWithOptions == null)
        {
          continue;
        }

        final String name = nameWithOptions.getFirst();
        final List<String> options = nameWithOptions.getSecond();

        if (schema == null)
        {
          // Just use the name as provided.
          List<List<String>> optionLists = m.get(name);
          if (optionLists == null)
          {
            optionLists = new ArrayList<List<String>>(1);
            m.put(name, optionLists);
          }
          optionLists.add(options);
        }
        else
        {
          // If the attribute type is defined in the schema, then use it to get
          // all names and the OID.  Otherwise, just use the name as provided.
          final AttributeTypeDefinition at = schema.getAttributeType(name);
          if (at == null)
          {
            List<List<String>> optionLists = m.get(name);
            if (optionLists == null)
            {
              optionLists = new ArrayList<List<String>>(1);
              m.put(name, optionLists);
            }
            optionLists.add(options);
          }
          else
          {
            addAttributeOIDAndNames(at, m, options);
          }
        }
      }
    }

    return m;
  }



  /**
   * Parses the provided string into an attribute type and set of options.
   *
   * @param  s  The string to be parsed.
   *
   * @return  An {@code ObjectPair} in which the first element is the attribute
   *          type name and the second is the list of options (or an empty
   *          list if there are no options).  Alternately, a value of
   *          {@code null} may be returned if the provided string does not
   *          represent a valid attribute type description.
   */
  private static ObjectPair<String,List<String>> getNameWithOptions(
                                                      final String s)
  {
    if (! Attribute.nameIsValid(s, true))
    {
      return null;
    }

    final String l = StaticUtils.toLowerCase(s);

    int semicolonPos = l.indexOf(';');
    if (semicolonPos < 0)
    {
      return new ObjectPair<String,List<String>>(l,
           Collections.<String>emptyList());
    }

    final String name = l.substring(0, semicolonPos);
    final ArrayList<String> optionList = new ArrayList<String>(1);
    while (true)
    {
      final int nextSemicolonPos = l.indexOf(';', semicolonPos+1);
      if (nextSemicolonPos < 0)
      {
        optionList.add(l.substring(semicolonPos+1));
        break;
      }
      else
      {
        optionList.add(l.substring(semicolonPos+1, nextSemicolonPos));
        semicolonPos = nextSemicolonPos;
      }
    }

    return new ObjectPair<String,List<String>>(name, optionList);
  }



  /**
   * Adds all-lowercase versions of the OID and all names for the provided
   * attribute type definition to the given map with the given options.
   *
   * @param  d  The attribute type definition to process.
   * @param  m  The map to which the OID and names should be added.
   * @param  o  The array of attribute options to use in the map.  It should be
   *            empty if no options are needed, and must not be {@code null}.
   */
  private void addAttributeOIDAndNames(final AttributeTypeDefinition d,
                                       final Map<String,List<List<String>>> m,
                                       final List<String> o)
  {
    if (d == null)
    {
      return;
    }

    final String lowerOID = StaticUtils.toLowerCase(d.getOID());
    if (lowerOID != null)
    {
      List<List<String>> l = m.get(lowerOID);
      if (l == null)
      {
        l = new ArrayList<List<String>>(1);
        m.put(lowerOID, l);
      }

      l.add(o);
    }

    for (final String name : d.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<List<String>> l = m.get(lowerName);
      if (l == null)
      {
        l = new ArrayList<List<String>>(1);
        m.put(lowerName, l);
      }

      l.add(o);
    }

    // If a schema is available, then see if the attribute type has any
    // subordinate types.  If so, then add them.
    if (schema != null)
    {
      for (final AttributeTypeDefinition subordinateType :
           schema.getSubordinateAttributeTypes(d))
      {
        addAttributeOIDAndNames(subordinateType, m, o);
      }
    }
  }



  /**
   * Returns the provided entry to the client, paring it down as necessary
   * based on the requested attributes.
   *
   * @param  messageID          The message ID for the search operation.
   * @param  entry              The entry to be returned.
   * @param  allUserAttrs       Indicates whether to return all user attributes.
   * @param  allOpAttrs         Indicates whether to return all operational
   *                            attributes.
   * @param  returnAttrs        A map with information about the specific
   *                            attribute types to return.
   * @param  includeSubEntries  Indicates whether LDAP subentries should be
   *                            returned.
   * @param  includeChangeLog   Indicates whether entries within the changelog
   *                            should be returned.
   * @param  hasManageDsaIT     Indicates whether the request includes the
   *                            ManageDsaIT control, which can change how smart
   *                            referrals should be handled.
   * @param  entriesSent        The number of entries returned so far for the
   *                            associated search.
   * @param  sizeLimit          The size limit for the search.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         return the entry.
   */
  private void returnEntry(final int messageID, final Entry entry,
                           final boolean allUserAttrs, final boolean allOpAttrs,
                           final Map<String,List<List<String>>> returnAttrs,
                           final boolean includeSubEntries,
                           final boolean includeChangeLog,
                           final boolean hasManageDsaIT,
                           final AtomicInteger entriesSent,
                           final int sizeLimit)
          throws LDAPException
  {
    // Check to see if we have hit the size limit.
    if ((sizeLimit > 0) && (entriesSent.get() >= sizeLimit))
    {
      throw new LDAPException(ResultCode.SIZE_LIMIT_EXCEEDED,
           ERR_MEM_HANDLER_SEARCH_SIZE_LIMIT_EXCEEDED.get());
    }

    // See if the entry should be suppressed as an LDAP subentry.
    if ((! includeSubEntries) &&
        (entry.hasObjectClass("ldapSubEntry") ||
         entry.hasObjectClass("inheritableLDAPSubEntry")))
    {
      return;
    }

    // See if the entry should be suppressed as a changelog entry.
    if ((! includeChangeLog) &&
        (entry.getParsedDN().isDescendantOf(changeLogBaseDN, true)))
    {
      return;
    }

    // See if the entry is a referral and should result in a reference rather
    // than an entry.
    if ((! hasManageDsaIT) && entry.hasObjectClass("referral"))
    {
      connection.sendSearchResultReference(messageID,
           new SearchResultReferenceProtocolOp(
                Arrays.asList(entry.getAttributeValues("ref"))));
      return;
    }

    connection.sendSearchResultEntry(messageID,
         trimForRequestedAttributes(entry, allUserAttrs, allOpAttrs,
              returnAttrs));
    entriesSent.incrementAndGet();
  }



  /**
   * Retrieves a copy of the provided entry that includes only the appropriate
   * set of requested attributes.
   *
   * @param  entry         The entry to be returned.
   * @param  allUserAttrs  Indicates whether to return all user attributes.
   * @param  allOpAttrs    Indicates whether to return all operational
   *                       attributes.
   * @param  returnAttrs   A map with information about the specific attribute
   *                       types to return.
   *
   * @return  A copy of the provided entry that includes only the appropriate
   *          set of requested attributes.
   */
  private Entry trimForRequestedAttributes(final Entry entry,
                     final boolean allUserAttrs, final boolean allOpAttrs,
                     final Map<String,List<List<String>>> returnAttrs)
  {
    // See if we can return the entry without paring it down.
    if (allUserAttrs)
    {
      if (allOpAttrs || (schema == null))
      {
        return entry;
      }
    }


    // If we've gotten here, then we may only need to return a partial entry.
    final Entry copy = new Entry(entry.getDN());

    for (final Attribute a : entry.getAttributes())
    {
      final ObjectPair<String,List<String>> nameWithOptions =
           getNameWithOptions(a.getName());
      final String name = nameWithOptions.getFirst();
      final List<String> options = nameWithOptions.getSecond();

      // If there is a schema, then see if it is an operational attribute, since
      // that needs to be handled in a manner different from user attributes
      if (schema != null)
      {
        final AttributeTypeDefinition at = schema.getAttributeType(name);
        if ((at != null) && at.isOperational())
        {
          if (allOpAttrs)
          {
            copy.addAttribute(a);
            continue;
          }

          final List<List<String>> optionLists = returnAttrs.get(name);
          if (optionLists == null)
          {
            continue;
          }

          for (final List<String> optionList : optionLists)
          {
            boolean matchAll = true;
            for (final String option : optionList)
            {
              if (! options.contains(option))
              {
                matchAll = false;
                break;
              }
            }

            if (matchAll)
            {
              copy.addAttribute(a);
              break;
            }
          }
          continue;
        }
      }

      // We'll assume that it's a user attribute, and we'll look for an exact
      // match on the base name.
      if (allUserAttrs)
      {
        copy.addAttribute(a);
        continue;
      }

      final List<List<String>> optionLists = returnAttrs.get(name);
      if (optionLists == null)
      {
        continue;
      }

      for (final List<String> optionList : optionLists)
      {
        boolean matchAll = true;
        for (final String option : optionList)
        {
          if (! options.contains(option))
          {
            matchAll = false;
            break;
          }
        }

        if (matchAll)
        {
          copy.addAttribute(a);
          break;
        }
      }
    }

    return copy;
  }



  /**
   * Retrieves the DN of the existing entry which is the closest hierarchical
   * match to the provided DN.
   *
   * @param  dn  The DN for which to retrieve the appropriate matched DN.
   *
   * @return  The appropriate matched DN value, or {@code null} if there is
   *          none.
   */
  private String getMatchedDNString(final DN dn)
  {
    DN parentDN = dn.getParent();
    while (parentDN != null)
    {
      if (entryMap.containsKey(parentDN))
      {
        return parentDN.toString();
      }

      parentDN = parentDN.getParent();
    }

    return null;
  }



  /**
   * Converts the provided string list to an array.
   *
   * @param  l  The possibly null list to be converted.
   *
   * @return  The string array with the same elements as the given list in the
   *          same order, or {@code null} if the given list was null.
   */
  private static String[] stringListToArray(final List<String> l)
  {
    if (l == null)
    {
      return null;
    }
    else
    {
      final String[] a = new String[l.size()];
      return l.toArray(a);
    }
  }



  /**
   * Creates a changelog entry from the information in the provided add request
   * and adds it to the server changelog.
   *
   * @param  addRequest  The add request to use to construct the changelog
   *                     entry.
   * @param  authzDN     The authorization DN for the change.
   */
  private void addChangeLogEntry(final AddRequestProtocolOp addRequest,
                                 final DN authzDN)
  {
    // If the changelog is disabled, then don't do anything.
    if (maxChangelogEntries <= 0)
    {
      return;
    }

    final long changeNumber = lastChangeNumber.incrementAndGet();
    final LDIFAddChangeRecord changeRecord = new LDIFAddChangeRecord(
         addRequest.getDN(), addRequest.getAttributes());
    try
    {
      addChangeLogEntry(
           ChangeLogEntry.constructChangeLogEntry(changeNumber, changeRecord),
           authzDN);
    }
    catch (final LDAPException le)
    {
      // This should not happen.
      Debug.debugException(le);
    }
  }



  /**
   * Creates a changelog entry from the information in the provided delete
   * request and adds it to the server changelog.
   *
   * @param  e        The entry to be deleted.
   * @param  authzDN  The authorization DN for the change.
   */
  private void addDeleteChangeLogEntry(final Entry e, final DN authzDN)
  {
    // If the changelog is disabled, then don't do anything.
    if (maxChangelogEntries <= 0)
    {
      return;
    }

    final long changeNumber = lastChangeNumber.incrementAndGet();
    final LDIFDeleteChangeRecord changeRecord =
         new LDIFDeleteChangeRecord(e.getDN());

    // Create the changelog entry.
    try
    {
      final ChangeLogEntry cle = ChangeLogEntry.constructChangeLogEntry(
           changeNumber, changeRecord);

      // Add a set of deleted entry attributes, which is simply an LDIF-encoded
      // representation of the entry, excluding the first line since it contains
      // the DN.
      final StringBuilder deletedEntryAttrsBuffer = new StringBuilder();
      final String[] ldifLines = e.toLDIF(0);
      for (int i=1; i < ldifLines.length; i++)
      {
        deletedEntryAttrsBuffer.append(ldifLines[i]);
        deletedEntryAttrsBuffer.append(StaticUtils.EOL);
      }

      final Entry copy = cle.duplicate();
      copy.addAttribute(ChangeLogEntry.ATTR_DELETED_ENTRY_ATTRS,
           deletedEntryAttrsBuffer.toString());
      addChangeLogEntry(new ChangeLogEntry(copy), authzDN);
    }
    catch (final LDAPException le)
    {
      // This should never happen.
      Debug.debugException(le);
    }
  }



  /**
   * Creates a changelog entry from the information in the provided modify
   * request and adds it to the server changelog.
   *
   * @param  modifyRequest  The modify request to use to construct the changelog
   *                        entry.
   * @param  authzDN        The authorization DN for the change.
   */
  private void addChangeLogEntry(final ModifyRequestProtocolOp modifyRequest,
                                 final DN authzDN)
  {
    // If the changelog is disabled, then don't do anything.
    if (maxChangelogEntries <= 0)
    {
      return;
    }

    final long changeNumber = lastChangeNumber.incrementAndGet();
    final LDIFModifyChangeRecord changeRecord =
         new LDIFModifyChangeRecord(modifyRequest.getDN(),
              modifyRequest.getModifications());
    try
    {
      addChangeLogEntry(
           ChangeLogEntry.constructChangeLogEntry(changeNumber, changeRecord),
           authzDN);
    }
    catch (final LDAPException le)
    {
      // This should not happen.
      Debug.debugException(le);
    }
  }



  /**
   * Creates a changelog entry from the information in the provided modify DN
   * request and adds it to the server changelog.
   *
   * @param  modifyDNRequest  The modify DN request to use to construct the
   *                          changelog entry.
   * @param  authzDN          The authorization DN for the change.
   */
  private void addChangeLogEntry(
                    final ModifyDNRequestProtocolOp modifyDNRequest,
                    final DN authzDN)
  {
    // If the changelog is disabled, then don't do anything.
    if (maxChangelogEntries <= 0)
    {
      return;
    }

    final long changeNumber = lastChangeNumber.incrementAndGet();
    final LDIFModifyDNChangeRecord changeRecord =
         new LDIFModifyDNChangeRecord(modifyDNRequest.getDN(),
              modifyDNRequest.getNewRDN(), modifyDNRequest.deleteOldRDN(),
              modifyDNRequest.getNewSuperiorDN());
    try
    {
      addChangeLogEntry(
           ChangeLogEntry.constructChangeLogEntry(changeNumber, changeRecord),
           authzDN);
    }
    catch (final LDAPException le)
    {
      // This should not happen.
      Debug.debugException(le);
    }
  }



  /**
   * Adds the provided changelog entry to the data set, removing an old entry if
   * necessary to remain within the maximum allowed number of changes.  This
   * must only be called from a synchronized method, and the change number for
   * the changelog entry must have been obtained by calling
   * {@code lastChangeNumber.incrementAndGet()}.
   *
   * @param  e        The changelog entry to add to the data set.
   * @param  authzDN  The authorization DN for the change.
   */
  private void addChangeLogEntry(final ChangeLogEntry e, final DN authzDN)
  {
    // Construct the DN object to use for the entry and put it in the map.
    final long changeNumber = e.getChangeNumber();
    final DN dn = new DN(new RDN("changeNumber", String.valueOf(changeNumber)),
         changeLogBaseDN);

    final Entry entry = e.duplicate();
    if (generateOperationalAttributes)
    {
      final Date d = new Date();
      entry.addAttribute(new Attribute("entryDN",
           DistinguishedNameMatchingRule.getInstance(),
           dn.toNormalizedString()));
      entry.addAttribute(new Attribute("entryUUID",
           UUID.randomUUID().toString()));
      entry.addAttribute(new Attribute("subschemaSubentry",
           DistinguishedNameMatchingRule.getInstance(),
           subschemaSubentryDN.toString()));
      entry.addAttribute(new Attribute("creatorsName",
           DistinguishedNameMatchingRule.getInstance(),
           authzDN.toString()));
      entry.addAttribute(new Attribute("createTimestamp",
           GeneralizedTimeMatchingRule.getInstance(),
           StaticUtils.encodeGeneralizedTime(d)));
      entry.addAttribute(new Attribute("modifiersName",
           DistinguishedNameMatchingRule.getInstance(),
           authzDN.toString()));
      entry.addAttribute(new Attribute("modifyTimestamp",
           GeneralizedTimeMatchingRule.getInstance(),
           StaticUtils.encodeGeneralizedTime(d)));
    }

    entryMap.put(dn, entry);

    // Update the first change number and/or trim the changelog if necessary.
    final long firstNumber = firstChangeNumber.get();
    if (changeNumber == 1L)
    {
      // It's the first change, so we need to set the first change number.
      firstChangeNumber.set(1);
    }
    else
    {
      // See if we need to trim an entry.
      final long numChangeLogEntries = changeNumber - firstNumber + 1;
      if (numChangeLogEntries > maxChangelogEntries)
      {
        // We need to delete the first changelog entry and increment the
        // first change number.
        firstChangeNumber.incrementAndGet();
        entryMap.remove(new DN(
             new RDN("changeNumber", String.valueOf(firstNumber)),
             changeLogBaseDN));
      }
    }
  }



  /**
   * Checks to see if the provided control map includes a proxied authorization
   * control (v1 or v2) and if so then attempts to determine the appropriate
   * authorization identity to use for the operation.
   *
   * @param  m  The map of request controls, indexed by OID.
   *
   * @return  The DN of the authorized user, or the current authentication DN
   *          if the control map does not include a proxied authorization
   *          request control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         determine the authorization DN.
   */
  private DN handleProxiedAuthControl(final Map<String,Control> m)
          throws LDAPException
  {
    final ProxiedAuthorizationV1RequestControl p1 =
         (ProxiedAuthorizationV1RequestControl) m.get(
              ProxiedAuthorizationV1RequestControl.
                   PROXIED_AUTHORIZATION_V1_REQUEST_OID);
    if (p1 != null)
    {
      final DN authzDN = new DN(p1.getProxyDN());
      if (authzDN.isNullDN() ||
          entryMap.containsKey(authzDN) ||
          additionalBindCredentials.containsKey(authzDN))
      {
        return authzDN;
      }
      else
      {
        throw new LDAPException(ResultCode.AUTHORIZATION_DENIED,
             ERR_MEM_HANDLER_NO_SUCH_IDENTITY.get("dn:" + authzDN.toString()));
      }
    }

    final ProxiedAuthorizationV2RequestControl p2 =
         (ProxiedAuthorizationV2RequestControl) m.get(
              ProxiedAuthorizationV2RequestControl.
                   PROXIED_AUTHORIZATION_V2_REQUEST_OID);
    if (p2 != null)
    {
      return getDNForAuthzID(p2.getAuthorizationID());
    }

    return authenticatedDN;
  }



  /**
   * Attempts to identify the DN of the user referenced by the provided
   * authorization ID string.  It may be "dn:" followed by the target DN, or
   * "u:" followed by the value of the uid attribute in the entry.  If it uses
   * the "dn:" form, then it may reference the DN of a regular entry or a DN
   * in the configured set of additional bind credentials.
   *
   * @param  authzID  The authorization ID to resolve to a user DN.
   *
   * @return  The DN identified for the provided authorization ID.
   *
   * @throws  LDAPException  If a problem prevents resolving the authorization
   *                         ID to a user DN.
   */
  public synchronized DN getDNForAuthzID(final String authzID)
         throws LDAPException
  {
    final String lowerAuthzID = StaticUtils.toLowerCase(authzID);
    if (lowerAuthzID.startsWith("dn:"))
    {
      if (lowerAuthzID.equals("dn:"))
      {
        return DN.NULL_DN;
      }
      else
      {
        final DN dn = new DN(authzID.substring(3));
        if (entryMap.containsKey(dn) ||
            additionalBindCredentials.containsKey(dn))
        {
          return dn;
        }
        else
        {
          throw new LDAPException(ResultCode.AUTHORIZATION_DENIED,
               ERR_MEM_HANDLER_NO_SUCH_IDENTITY.get(authzID));
        }
      }
    }
    else if (lowerAuthzID.startsWith("u:"))
    {
      final Filter f =
           Filter.createEqualityFilter("uid", authzID.substring(2));
      final List<ReadOnlyEntry> entryList = search("", SearchScope.SUB, f);
      if (entryList.size() == 1)
      {
        return entryList.get(0).getParsedDN();
      }
      else
      {
        throw new LDAPException(ResultCode.AUTHORIZATION_DENIED,
             ERR_MEM_HANDLER_NO_SUCH_IDENTITY.get(authzID));
      }
    }
    else
    {
      throw new LDAPException(ResultCode.AUTHORIZATION_DENIED,
           ERR_MEM_HANDLER_NO_SUCH_IDENTITY.get(authzID));
    }
  }



  /**
   * Checks to see if the provided control map includes an assertion request
   * control, and if so then checks to see whether the provided entry satisfies
   * the filter in that control.
   *
   * @param  m  The map of request controls, indexed by OID.
   * @param  e  The entry to examine against the assertion filter.
   *
   * @throws  LDAPException  If the control map includes an assertion request
   *                         control and the provided entry does not match the
   *                         filter contained in that control.
   */
  private static void handleAssertionRequestControl(final Map<String,Control> m,
                                                    final Entry e)
          throws LDAPException
  {
    final AssertionRequestControl c = (AssertionRequestControl)
         m.get(AssertionRequestControl.ASSERTION_REQUEST_OID);
    if (c == null)
    {
      return;
    }

    try
    {
      if (c.getFilter().matchesEntry(e))
      {
        return;
      }
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
    }

    // If we've gotten here, then the filter doesn't match.
    throw new LDAPException(ResultCode.ASSERTION_FAILED,
         ERR_MEM_HANDLER_ASSERTION_CONTROL_NOT_SATISFIED.get());
  }



  /**
   * Checks to see if the provided control map includes a pre-read request
   * control, and if so then generates the appropriate response control that
   * should be returned to the client.
   *
   * @param  m  The map of request controls, indexed by OID.
   * @param  e  The entry as it appeared before the operation.
   *
   * @return  The pre-read response control that should be returned to the
   *          client, or {@code null} if there is none.
   */
  private PreReadResponseControl handlePreReadControl(
               final Map<String,Control> m, final Entry e)
  {
    final PreReadRequestControl c = (PreReadRequestControl)
         m.get(PreReadRequestControl.PRE_READ_REQUEST_OID);
    if (c == null)
    {
      return null;
    }

    final AtomicBoolean allUserAttrs = new AtomicBoolean(false);
    final AtomicBoolean allOpAttrs = new AtomicBoolean(false);
    final Map<String,List<List<String>>> returnAttrs =
         processRequestedAttributes(Arrays.asList(c.getAttributes()),
              allUserAttrs, allOpAttrs);

    final Entry trimmedEntry = trimForRequestedAttributes(e, allUserAttrs.get(),
         allOpAttrs.get(), returnAttrs);
    return new PreReadResponseControl(new ReadOnlyEntry(trimmedEntry));
  }



  /**
   * Checks to see if the provided control map includes a post-read request
   * control, and if so then generates the appropriate response control that
   * should be returned to the client.
   *
   * @param  m  The map of request controls, indexed by OID.
   * @param  e  The entry as it appeared before the operation.
   *
   * @return  The post-read response control that should be returned to the
   *          client, or {@code null} if there is none.
   */
  private PostReadResponseControl handlePostReadControl(
               final Map<String,Control> m, final Entry e)
  {
    final PostReadRequestControl c = (PostReadRequestControl)
         m.get(PostReadRequestControl.POST_READ_REQUEST_OID);
    if (c == null)
    {
      return null;
    }

    final AtomicBoolean allUserAttrs = new AtomicBoolean(false);
    final AtomicBoolean allOpAttrs = new AtomicBoolean(false);
    final Map<String,List<List<String>>> returnAttrs =
         processRequestedAttributes(Arrays.asList(c.getAttributes()),
              allUserAttrs, allOpAttrs);

    final Entry trimmedEntry = trimForRequestedAttributes(e, allUserAttrs.get(),
         allOpAttrs.get(), returnAttrs);
    return new PostReadResponseControl(new ReadOnlyEntry(trimmedEntry));
  }



  /**
   * Finds the smart referral entry which is hierarchically nearest the entry
   * with the given DN.
   *
   * @param  dn  The DN for which to find the hierarchically nearest smart
   *             referral entry.
   *
   * @return  The hierarchically nearest smart referral entry for the provided
   *          DN, or {@code null} if there are no smart referral entries with
   *          the provided DN or any of its ancestors.
   */
  private Entry findNearestReferral(final DN dn)
  {
    DN d = dn;
    while (true)
    {
      final Entry e = entryMap.get(d);
      if (e == null)
      {
        d = d.getParent();
        if (d == null)
        {
          return null;
        }
      }
      else if (e.hasObjectClass("referral"))
      {
        return e;
      }
      else
      {
        return null;
      }
    }
  }



  /**
   * Retrieves the referral URLs that should be used for the provided target DN
   * based on the given referral entry.
   *
   * @param  targetDN       The target DN from the associated operation.
   * @param  referralEntry  The entry containing the smart referral.
   *
   * @return  The referral URLs that should be returned.
   */
  static List<String> getReferralURLs(final DN targetDN,
                                      final Entry referralEntry)
  {
    final String[] refs = referralEntry.getAttributeValues("ref");
    if (refs == null)
    {
      return null;
    }

    final RDN[] retainRDNs;
    try
    {
      // If the target DN equals the referral entry DN, or if it's not
      // subordinate to the referral entry, then the URLs should be returned
      // as-is.
      final DN parsedEntryDN = referralEntry.getParsedDN();
      if (targetDN.equals(parsedEntryDN) ||
          (! targetDN.isDescendantOf(parsedEntryDN, true)))
      {
        return Arrays.asList(refs);
      }

      final RDN[] targetRDNs   = targetDN.getRDNs();
      final RDN[] refEntryRDNs = referralEntry.getParsedDN().getRDNs();
      retainRDNs = new RDN[targetRDNs.length - refEntryRDNs.length];
      System.arraycopy(targetRDNs, 0, retainRDNs, 0, retainRDNs.length);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return Arrays.asList(refs);
    }

    final List<String> refList = new ArrayList<String>(refs.length);
    for (final String ref : refs)
    {
      try
      {
        final LDAPURL url = new LDAPURL(ref);
        final RDN[] refRDNs = url.getBaseDN().getRDNs();
        final RDN[] newRefRDNs = new RDN[retainRDNs.length + refRDNs.length];
        System.arraycopy(retainRDNs, 0, newRefRDNs, 0, retainRDNs.length);
        System.arraycopy(refRDNs, 0, newRefRDNs, retainRDNs.length,
             refRDNs.length);
        final DN newBaseDN = new DN(newRefRDNs);

        final LDAPURL newURL = new LDAPURL(url.getScheme(), url.getHost(),
             url.getPort(), newBaseDN, null, null, null);
        refList.add(newURL.toString());
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);
        refList.add(ref);
      }
    }

    return refList;
  }



  /**
   * Indicates whether the specified entry exists in the server.
   *
   * @param  dn  The DN of the entry for which to make the determination.
   *
   * @return  {@code true} if the entry exists, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  public boolean entryExists(final String dn)
         throws LDAPException
  {
    return (getEntry(dn) != null);
  }



  /**
   * Indicates whether the specified entry exists in the server and matches the
   * given filter.
   *
   * @param  dn      The DN of the entry for which to make the determination.
   * @param  filter  The filter the entry is expected to match.
   *
   * @return  {@code true} if the entry exists and matches the specified filter,
   *          or {@code false} if not.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  public boolean entryExists(final String dn, final String filter)
         throws LDAPException
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      return false;
    }

    final Filter f = Filter.create(filter);
    try
    {
      return f.matchesEntry(e, schema);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      return false;
    }
  }



  /**
   * Indicates whether the specified entry exists in the server.  This will
   * return {@code true} only if the target entry exists and contains all values
   * for all attributes of the provided entry.  The entry will be allowed to
   * have attribute values not included in the provided entry.
   *
   * @param  entry  The entry to compare against the directory server.
   *
   * @return  {@code true} if the entry exists in the server and is a superset
   *          of the provided entry, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  public boolean entryExists(final Entry entry)
         throws LDAPException
  {
    final Entry e = getEntry(entry.getDN());
    if (e == null)
    {
      return false;
    }

    for (final Attribute a : entry.getAttributes())
    {
      for (final byte[] value : a.getValueByteArrays())
      {
        if (! e.hasAttributeValue(a.getName(), value))
        {
          return false;
        }
      }
    }

    return true;
  }



  /**
   * Ensures that an entry with the provided DN exists in the directory.
   *
   * @param  dn  The DN of the entry for which to make the determination.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist.
   */
  public void assertEntryExists(final String dn)
         throws LDAPException, AssertionError
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }
  }



  /**
   * Ensures that an entry with the provided DN exists in the directory.
   *
   * @param  dn      The DN of the entry for which to make the determination.
   * @param  filter  A filter that the target entry must match.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          match the provided filter.
   */
  public void assertEntryExists(final String dn, final String filter)
         throws LDAPException, AssertionError
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }

    final Filter f = Filter.create(filter);
    try
    {
      if (! f.matchesEntry(e, schema))
      {
        throw new AssertionError(
             ERR_MEM_HANDLER_TEST_ENTRY_DOES_NOT_MATCH_FILTER.get(dn, filter));
      }
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      throw new AssertionError(
           ERR_MEM_HANDLER_TEST_ENTRY_DOES_NOT_MATCH_FILTER.get(dn, filter));
    }
  }



  /**
   * Ensures that an entry exists in the directory with the same DN and all
   * attribute values contained in the provided entry.  The server entry may
   * contain additional attributes and/or attribute values not included in the
   * provided entry.
   *
   * @param  entry  The entry expected to be present in the directory server.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          match the provided filter.
   */
  public void assertEntryExists(final Entry entry)
         throws LDAPException, AssertionError
  {
    final Entry e = getEntry(entry.getDN());
    if (e == null)
    {
      throw new AssertionError(
           ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(entry.getDN()));
    }


    final Collection<Attribute> attrs = entry.getAttributes();
    final List<String> messages = new ArrayList<String>(attrs.size());

    for (final Attribute a : entry.getAttributes())
    {
      final Filter presFilter = Filter.createPresenceFilter(a.getName());
      if (! presFilter.matchesEntry(e, schema))
      {
        messages.add(ERR_MEM_HANDLER_TEST_ATTR_MISSING.get(entry.getDN(),
             a.getName()));
        continue;
      }

      for (final byte[] value : a.getValueByteArrays())
      {
        final Filter eqFilter = Filter.createEqualityFilter(a.getName(), value);
        if (! eqFilter.matchesEntry(e, schema))
        {
          messages.add(ERR_MEM_HANDLER_TEST_VALUE_MISSING.get(entry.getDN(),
               a.getName(), StaticUtils.toUTF8String(value)));
        }
      }
    }

    if (! messages.isEmpty())
    {
      throw new AssertionError(StaticUtils.concatenateStrings(messages));
    }
  }



  /**
   * Retrieves a list containing the DNs of the entries which are missing from
   * the directory server.
   *
   * @param  dns  The DNs of the entries to try to find in the server.
   *
   * @return  A list containing all of the provided DNs that were not found in
   *          the server, or an empty list if all entries were found.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  public List<String> getMissingEntryDNs(final Collection<String> dns)
         throws LDAPException
  {
    final List<String> missingDNs = new ArrayList<String>(dns.size());
    for (final String dn : dns)
    {
      final Entry e = getEntry(dn);
      if (e == null)
      {
        missingDNs.add(dn);
      }
    }

    return missingDNs;
  }



  /**
   * Ensures that all of the entries with the provided DNs exist in the
   * directory.
   *
   * @param  dns  The DNs of the entries for which to make the determination.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If any of the target entries does not exist.
   */
  public void assertEntriesExist(final Collection<String> dns)
         throws LDAPException, AssertionError
  {
    final List<String> missingDNs = getMissingEntryDNs(dns);
    if (missingDNs.isEmpty())
    {
      return;
    }

    final List<String> messages = new ArrayList<String>(missingDNs.size());
    for (final String dn : missingDNs)
    {
      messages.add(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }

    throw new AssertionError(StaticUtils.concatenateStrings(messages));
  }



  /**
   * Retrieves a list containing all of the named attributes which do not exist
   * in the target entry.
   *
   * @param  dn              The DN of the entry to examine.
   * @param  attributeNames  The names of the attributes expected to be present
   *                         in the target entry.
   *
   * @return  A list containing the names of the attributes which were not
   *          present in the target entry, an empty list if all specified
   *          attributes were found in the entry, or {@code null} if the target
   *          entry does not exist.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  public List<String> getMissingAttributeNames(final String dn,
                           final Collection<String> attributeNames)
         throws LDAPException
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      return null;
    }

    final List<String> missingAttrs =
         new ArrayList<String>(attributeNames.size());
    for (final String attr : attributeNames)
    {
      final Filter f = Filter.createPresenceFilter(attr);
      if (! f.matchesEntry(e, schema))
      {
        missingAttrs.add(attr);
      }
    }

    return missingAttrs;
  }



  /**
   * Ensures that the specified entry exists in the directory with all of the
   * specified attributes.
   *
   * @param  dn              The DN of the entry to examine.
   * @param  attributeNames  The names of the attributes that are expected to be
   *                         present in the provided entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          contain all of the specified attributes.
   */
  public void assertAttributeExists(final String dn,
                                    final Collection<String> attributeNames)
        throws LDAPException, AssertionError
  {
    final List<String> missingAttrs =
         getMissingAttributeNames(dn, attributeNames);
    if (missingAttrs == null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }
    else if (missingAttrs.isEmpty())
    {
      return;
    }

    final List<String> messages = new ArrayList<String>(missingAttrs.size());
    for (final String attr : missingAttrs)
    {
      messages.add(ERR_MEM_HANDLER_TEST_ATTR_MISSING.get(dn, attr));
    }

    throw new AssertionError(StaticUtils.concatenateStrings(messages));
  }



  /**
   * Retrieves a list of all provided attribute values which are missing from
   * the specified entry.  The target attribute may or may not contain
   * additional values.
   *
   * @param  dn               The DN of the entry to examine.
   * @param  attributeName    The attribute expected to be present in the target
   *                          entry with the given values.
   * @param  attributeValues  The values expected to be present in the target
   *                          entry.
   *
   * @return  A list containing all of the provided values which were not found
   *          in the entry, an empty list if all provided attribute values were
   *          found, or {@code null} if the target entry does not exist.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  public List<String> getMissingAttributeValues(final String dn,
                           final String attributeName,
                           final Collection<String> attributeValues)
       throws LDAPException
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      return null;
    }

    final List<String> missingValues =
         new ArrayList<String>(attributeValues.size());
    for (final String value : attributeValues)
    {
      final Filter f = Filter.createEqualityFilter(attributeName, value);
      if (! f.matchesEntry(e, schema))
      {
        missingValues.add(value);
      }
    }

    return missingValues;
  }



  /**
   * Ensures that the specified entry exists in the directory with all of the
   * specified values for the given attribute.  The attribute may or may not
   * contain additional values.
   *
   * @param  dn               The DN of the entry to examine.
   * @param  attributeName    The name of the attribute to examine.
   * @param  attributeValues  The set of values which must exist for the given
   *                          attribute.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist, does not
   *                          contain the specified attribute, or that attribute
   *                          does not have all of the specified values.
   */
  public void assertValueExists(final String dn, final String attributeName,
                                final Collection<String> attributeValues)
        throws LDAPException, AssertionError
  {
    final List<String> missingValues =
         getMissingAttributeValues(dn, attributeName, attributeValues);
    if (missingValues == null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }
    else if (missingValues.isEmpty())
    {
      return;
    }

    // See if the attribute exists at all in the entry.
    final Entry e = getEntry(dn);
    final Filter f = Filter.createPresenceFilter(attributeName);
    if (! f.matchesEntry(e,  schema))
    {
      throw new AssertionError(
           ERR_MEM_HANDLER_TEST_ATTR_MISSING.get(dn, attributeName));
    }

    final List<String> messages = new ArrayList<String>(missingValues.size());
    for (final String value : missingValues)
    {
      messages.add(ERR_MEM_HANDLER_TEST_VALUE_MISSING.get(dn, attributeName,
           value));
    }

    throw new AssertionError(StaticUtils.concatenateStrings(messages));
  }



  /**
   * Ensures that the specified entry does not exist in the directory.
   *
   * @param  dn  The DN of the entry expected to be missing.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is found in the server.
   */
  public void assertEntryMissing(final String dn)
         throws LDAPException, AssertionError
  {
    final Entry e = getEntry(dn);
    if (e != null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_EXISTS.get(dn));
    }
  }



  /**
   * Ensures that the specified entry exists in the directory but does not
   * contain any of the specified attributes.
   *
   * @param  dn              The DN of the entry expected to be present.
   * @param  attributeNames  The names of the attributes expected to be missing
   *                         from the entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is missing from the server, or
   *                          if it contains any of the target attributes.
   */
  public void assertAttributeMissing(final String dn,
                                     final Collection<String> attributeNames)
         throws LDAPException, AssertionError
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }

    final List<String> messages = new ArrayList<String>(attributeNames.size());
    for (final String name : attributeNames)
    {
      final Filter f = Filter.createPresenceFilter(name);
      if (f.matchesEntry(e, schema))
      {
        messages.add(ERR_MEM_HANDLER_TEST_ATTR_EXISTS.get(dn, name));
      }
    }

    if (! messages.isEmpty())
    {
      throw new AssertionError(StaticUtils.concatenateStrings(messages));
    }
  }



  /**
   * Ensures that the specified entry exists in the directory but does not
   * contain any of the specified attribute values.
   *
   * @param  dn               The DN of the entry expected to be present.
   * @param  attributeName    The name of the attribute to examine.
   * @param  attributeValues  The values expected to be missing from the target
   *                          entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is missing from the server, or
   *                          if it contains any of the target attribute values.
   */
  public void assertValueMissing(final String dn, final String attributeName,
                                 final Collection<String> attributeValues)
         throws LDAPException, AssertionError
  {
    final Entry e = getEntry(dn);
    if (e == null)
    {
      throw new AssertionError(ERR_MEM_HANDLER_TEST_ENTRY_MISSING.get(dn));
    }

    final List<String> messages = new ArrayList<String>(attributeValues.size());
    for (final String value : attributeValues)
    {
      final Filter f = Filter.createEqualityFilter(attributeName, value);
      if (f.matchesEntry(e, schema))
      {
        messages.add(ERR_MEM_HANDLER_TEST_VALUE_EXISTS.get(dn, attributeName,
             value));
      }
    }

    if (! messages.isEmpty())
    {
      throw new AssertionError(StaticUtils.concatenateStrings(messages));
    }
  }
}
