/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
using System;
using System.Diagnostics;
using System.DirectoryServices;
using Org.IdentityConnectors.Framework.Common.Exceptions;

namespace Org.IdentityConnectors.ActiveDirectory
{
    /**
     * This class will handle setting all of the terminal services attributes
     */
    public class TerminalServicesUtils
    {
        // tracing (using ActiveDirectoryConnector's name!)
        internal static TraceSource LOGGER = new TraceSource(TraceNames.DEFAULT);
        private const int CAT_DEFAULT = 1;      // default tracing event category

        // used to be 'Terminal Services Initial Program'
        public static string TS_INITIAL_PROGRAM = "TerminalServicesInitialProgram";

        // used to be 'Terminal Services Initial Program Directory'
        public static string TS_INITIAL_PROGRAM_DIR = "TerminalServicesWorkDirectory";
        
        // used to be 'Terminal Services Inherit Initial Program'

        // used to be 'Terminal Services Allow Logon' - defaults to false, so testing true
        public static string TS_ALLOW_LOGON = "AllowLogon";

        // used to be 'Terminal Services Active Session Timeout'
        public static string TS_MAX_CONNECTION_TIME = "MaxConnectionTime";

        // used to be 'Terminal Services Disconnected Session Timeout'
        public static string TS_MAX_DISCONNECTION_TIME = "MaxDisconnectionTime";

        // used to be 'Terminal Services Idle Timeout'
        public static string TS_MAX_IDLE_TIME = "MaxIdleTime";

        // used to be 'Terminal Services Connect Client Drives At Logon'
        public static string TS_CONNECT_CLIENT_DRIVES_AT_LOGON = "ConnectClientDrivesAtLogon";

        // used to be 'Terminal Services Connect Client Printers At Logon'
        public static string TS_CONNECT_CLIENT_PRINTERS_AT_LOGON = "ConnectClientPrintersAtLogon";

        // used to be 'Terminal Services Default To Main Client Printer'
        public static string TS_DEFAULT_TO_MAIN_PRINTER = "DefaultToMainPrinter";

        // used to be 'Terminal Services End Session On Timeout Or Broken Connection'
        public static string TS_BROKEN_CONNECTION_ACTION = "BrokenConnectionAction";

        // used to be 'Terminal Services Allow Reconnect From Originating Client Only'
        public static string TS_RECONNECTION_ACTION = "ReconnectionAction";

        // used to be 'Terminal Services Callback Settings'

        // used to be 'Terminal Services Callback Phone Number'

        // used to be 'Terminal Services Remote Control Settings'
        public static string TS_ENABLE_REMOTE_CONTROL = "EnableRemoteControl";

        // used to be 'Terminal Services User Profile'
        public static string TS_PROFILE_PATH = "TerminalServicesProfilePath";

        // used to be 'Terminal Services Local Home Directory'
        public static string TS_HOME_DIRECTORY = "TerminalServicesHomeDirectory";

        // used to be 'Terminal Services Home Directory Drive'
        public static string TS_HOME_DRIVE = "TerminalServicesHomeDrive";

        private static T GetValue<T>(DirectoryEntry entry, string name, T defaultValue)
        {
            Stopwatch stopWatch = new Stopwatch();
    		stopWatch.Start();

            // get 'name' from the directory entry, and return it if it exists
            try
            {
                object result = entry.InvokeGet(name);
                if (result != null)
                {
                    T value = (T)result;
                    return value;
                }
            }
            catch (Exception e)
            {
                LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, "Unable to retrieve property called {0}", name);
                LOGGER.TraceEvent(TraceEventType.Warning, CAT_DEFAULT, e.Message);
            }
            finally
            {
                stopWatch.Stop();
    			TimeSpan ts = stopWatch.Elapsed;	// Get the elapsed time as a TimeSpan value.
				string elapsedTime = String.Format("{0:00}:{1:00}:{2:00}.{3:000}",
        			ts.Hours, ts.Minutes, ts.Seconds,
        			ts.Milliseconds);
                LOGGER.TraceEvent(TraceEventType.Verbose, CAT_DEFAULT, "Getting {0} took {1}, that is {2} ticks", name, elapsedTime, stopWatch.ElapsedTicks);
            }

            // if the name didn't exist, return 'defaultValue'
            return defaultValue;
        }

        internal static void SetValue<T>(UpdateType type, 
            DirectoryEntry directoryEntry, string name, T value)
        {
            if (!type.Equals(UpdateType.REPLACE))
            {
                // Only allow replace on single value attributes,
                // and for now, all terminal services are single value
                ThrowInvalidUpdateType(name);
            }

            if (value == null)
            {
                throw new ArgumentException();
            }

            // invoke set on 'name' with 'value'
            directoryEntry.InvokeSet(name, value);
        }

        private static void ThrowInvalidUpdateType(string attributeName)
        {
            // throws an exception that says invalid update type 
            string msg = string.Format("The update type specified is invalid for the terminal services attribute ''{0}''",
                attributeName);
            throw new ConnectorException(msg);
        }

        internal static string GetInitialProgram(DirectoryEntry entry)
        {
            return GetValue<string>(entry, TS_INITIAL_PROGRAM, null);
        }

        internal static void SetInitialProgram(UpdateType type, DirectoryEntry directoryEntry,
            string initialProgram)
        {
            SetValue(type, directoryEntry, TS_INITIAL_PROGRAM, initialProgram);
        }

        internal static string GetInitialProgramDir(DirectoryEntry entry)
        {
            return GetValue<string>(entry, TS_INITIAL_PROGRAM_DIR, null);
        }

        internal static void SetInitialProgramDir(UpdateType type,
            DirectoryEntry directoryEntry, string initialProgramDir)
        {
            SetValue(type, directoryEntry, TS_INITIAL_PROGRAM_DIR, initialProgramDir);
        }

        internal static int? GetAllowLogon(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_ALLOW_LOGON, null);
        }

        internal static void SetAllowLogon(UpdateType type, DirectoryEntry directoryEntry,
            int? isAllowed)
        {
            SetValue(type, directoryEntry, TS_ALLOW_LOGON, isAllowed);
        }

        internal static int? GetMaxConnectionTime(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_MAX_CONNECTION_TIME, null);
        }

        internal static void SetMaxConnectionTime(UpdateType type, DirectoryEntry directoryEntry,
            int? maxConnectionTime)
        {
            SetValue(type, directoryEntry, TS_MAX_CONNECTION_TIME, maxConnectionTime);
        }

        internal static int? GetMaxDisconnectionTime(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_MAX_DISCONNECTION_TIME, null);
        }

        internal static void SetMaxDisconnectionTime(UpdateType type,
            DirectoryEntry directoryEntry, int? maxDisconnectionTime)
        {
            SetValue(type, directoryEntry, TS_MAX_DISCONNECTION_TIME, maxDisconnectionTime);
        }

        internal static int? GetMaxIdleTime(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_MAX_IDLE_TIME, null);
        }

        internal static void SetMaxIdleTime(UpdateType type, DirectoryEntry directoryEntry,
            int? maxIdleTime)
        {
            SetValue(type, directoryEntry, TS_MAX_IDLE_TIME, maxIdleTime);
        }

        internal static int? GetConnectClientDrivesAtLogon(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_CONNECT_CLIENT_DRIVES_AT_LOGON, null);
        }

        internal static void SetConnectClientDrivesAtLogon(UpdateType type,
            DirectoryEntry directoryEntry, int? connectClientDrivesAtLogon)
        {
            SetValue(type, directoryEntry, TS_CONNECT_CLIENT_DRIVES_AT_LOGON, 
                connectClientDrivesAtLogon);
        }

        internal static int? GetConnectClientPrintersAtLogon(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_CONNECT_CLIENT_PRINTERS_AT_LOGON, null);
        }

        internal static void SetConnectClientPrintersAtLogon(UpdateType type,
            DirectoryEntry directoryEntry, int? connectClientPrintersAtLogon)
        {
            SetValue(type, directoryEntry, TS_CONNECT_CLIENT_PRINTERS_AT_LOGON, 
                connectClientPrintersAtLogon);
        }

        internal static int? GetDefaultToMainPrinter(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_DEFAULT_TO_MAIN_PRINTER, null);
        }

        internal static void SetDefaultToMainPrinter(UpdateType type,
            DirectoryEntry directoryEntry, int? defaultToMainPrinter)
        {
            SetValue(type, directoryEntry, TS_DEFAULT_TO_MAIN_PRINTER, 
                defaultToMainPrinter);
        }

        internal static int? GetBrokenConnectionAction(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_BROKEN_CONNECTION_ACTION, null);
        }

        internal static void SetBrokenConnectionAction(UpdateType type,
            DirectoryEntry directoryEntry, int? brokenConnectionAction)
        {
            SetValue(type, directoryEntry, TS_BROKEN_CONNECTION_ACTION, 
                brokenConnectionAction);
        }

        internal static int? GetReconnectionAction(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_RECONNECTION_ACTION, null);
        }

        internal static void SetReconnectionAction(UpdateType type,
            DirectoryEntry directoryEntry, int? reconnectionAction)
        {
            SetValue(type, directoryEntry, TS_RECONNECTION_ACTION, reconnectionAction);
        }

        internal static int? GetEnableRemoteControl(DirectoryEntry entry)
        {
            return GetValue<int?>(entry, TS_ENABLE_REMOTE_CONTROL, null);
        }

        internal static void SetEnableRemoteControl(UpdateType type,
            DirectoryEntry directoryEntry, int? enableRemoteControl)
        {
            SetValue(type, directoryEntry, TS_ENABLE_REMOTE_CONTROL, enableRemoteControl);
        }

        internal static string GetProfilePath(DirectoryEntry entry)
        {
            return GetValue<string>(entry, TS_PROFILE_PATH, null);
        }

        internal static void SetProfilePath(UpdateType type,
            DirectoryEntry directoryEntry, string profilePath)
        {
            SetValue(type, directoryEntry, TS_PROFILE_PATH, profilePath);
        }

        internal static string GetHomeDirectory(DirectoryEntry entry)
        {
            return GetValue<string>(entry, TS_HOME_DIRECTORY, null);
        }

        internal static void SetHomeDirectory(UpdateType type,
            DirectoryEntry directoryEntry, string homeDirectory)
        {
            SetValue(type, directoryEntry, TS_HOME_DIRECTORY, homeDirectory);
        }

        internal static string GetHomeDrive(DirectoryEntry entry)
        {
            return GetValue<string>(entry, TS_HOME_DRIVE, null);
        }

        internal static void SetHomeDrive(UpdateType type,
            DirectoryEntry directoryEntry, string homeDrive)
        {
            SetValue(type, directoryEntry, TS_HOME_DRIVE, homeDrive);
        }

    }
}
