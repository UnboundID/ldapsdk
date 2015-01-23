/*
 * Copyright 2009-2014 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2014 UnboundID Corp.
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



/**
 * This package contains a set of classes that may be used to ease the process
 * of migrating an application originally written to use the Netscape Directory
 * SDK for Java so that it uses the UnboundID LDAP SDK for Java instead.  The
 * API exposed in this package may be similar to that provided in other SDKs,
 * like Novell JLDAP.
 * <BR><BR>
 * The classes in this package do not constitute a complete set of those
 * provided by the Netscape SDK, nor do all of the classes which are provided
 * contain the complete set of methods from the corresponding classes from the
 * Netscape API.  However, it should contain those classes and methods which are
 * most likely to be used by applications written to use that SDK, so that a
 * minimal set of code changes may be required when initially performing the
 * migration.  In many cases, it may simply be sufficient to replace all
 * references to the {@code netscape.ldap} package with
 * {@code com.unboundid.ldap.sdk.migrate.ldapjdk}.  Note, however, that the
 * logic implemented in this package is a wrapper that translates processing to
 * use the core functionality provided by the UnboundID LDAP SDK for Java, so
 * code changes may be required to achieve optimal performance.
 * <BR><BR>
 * This implementation was constructed solely from the public API documentation
 * available online at http://www.mozilla.org/directory/javasdk.html.  The
 * source code used to generate that API was not used in any way when creating
 * this implementation, but was developed solely by UnboundID Corp.
 * <BR><BR>
 * UnboundID is a registered trademark of UnboundID Corp.  Netscape and Novell
 * are registered trademarks of their respective holders.
 */
package com.unboundid.ldap.sdk.migrate.ldapjdk;