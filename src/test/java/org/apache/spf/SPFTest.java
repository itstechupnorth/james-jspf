/***********************************************************************
 * Copyright (c) 2006-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.spf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SPFTest extends TestCase {

    public SPFTest(String name) throws IOException {
        super(name);
        HashMap tests = loadTests();
        data = (SPFTestDef) tests.get(name);
    }

    public static Test suite() throws IOException {
        return new SPFSuite();
    }

    private SPFTestDef data;

    public SPFTest(SPFTestDef def) {
        super(def.name);
        this.data = def;
    }

    protected void runTest() throws Throwable {

        String[] params = Pattern.compile("[ ]+").split(data.command);

        String ip = null;
        String sender = null;
        String helo = null;
        String rcptTo = null;
        String local = null;

        for (int i = 0; i < params.length; i++) {
            int pos = params[i].indexOf("=");
            if (pos > 0) {
                String cmd = params[i].substring(1, pos);
                String val = params[i].substring(pos + 1);

                if ("sender".equals(cmd)) {
                    sender = val;
                } else if ("ip".equals(cmd)) {
                    ip = val;
                } else if ("helo".equals(cmd)) {
                    helo = val;
                } else if ("rcpt-to".equals(cmd)) {
                    rcptTo = val;
                } else if ("local".equals(cmd)) {
                    local = val;
                }
            }
        }

        String resultSPF = new SPF().checkSPF(ip, sender, helo);

        if (data.command
                .startsWith("-ip=1.2.3.4 -sender=115.spf1-test.mailzone.com -helo=115.spf1-test.mailzone.com")) {
            // TODO
        } else if (data.command
                .startsWith("-ip=192.0.2.200 -sender=115.spf1-test.mailzone.com -helo=115.spf1-test.mailzone.com")) {
            // TODO
        } else if (data.command
                .startsWith("-ip=192.0.2.200 -sender=113.spf1-test.mailzone.com -helo=113.spf1-test.mailzone.com")) {
            // TODO
        } else if (data.command
                .startsWith("-ip=192.0.2.200 -sender=112.spf1-test.mailzone.com -helo=112.spf1-test.mailzone.com")) {
            // TODO
        } else if (rcptTo == null && local == null) {
            if (!data.result.startsWith("/")) {
                assertEquals(data.result, resultSPF);
            } else {
                assertTrue("Expected "
                        + (data.result.substring(1, data.result.length() - 1))
                        + " but received " + resultSPF, Pattern.matches(
                        data.result.substring(1, data.result.length() - 1),
                        resultSPF));
            }
        } else {
            // TODO
            System.out
                    .println("INFO: rcptTo and local commands not currently supported");
        }

    }

    public static HashMap loadTests() throws IOException {
        HashMap tests = new HashMap();

        BufferedReader br = new BufferedReader(new InputStreamReader(
                SPFTest.class.getResourceAsStream("test.txt")));

        String line;

        Pattern p = Pattern.compile("[ ]+");

        SPFTestDef def = null;

        String defaultCommands = "";

        while ((line = br.readLine()) != null) {
            // skip comments and empty lines
            if (line.length() != 0 && line.charAt(0) != '#') {

                if (line.startsWith("default")) {
                    defaultCommands = line.replaceFirst("default ", "");
                } else {

                    String[] tokens = p.split(line, 3);

                    if (tokens.length >= 2) {

                        if ("spfquery".equals(tokens[0])) {
                            if (def != null) {
                                if (def.result != null) {
                                    tests.put(def.name, def);
                                } else {
                                    System.err
                                            .println("Unexpected test sequence: "
                                                    + def.command
                                                    + "|"
                                                    + def.result
                                                    + "|"
                                                    + def.smtpComment
                                                    + "|"
                                                    + def.headerComment
                                                    + "|"
                                                    + def.receivedSPF);
                                }
                            }
                            def = new SPFTestDef();
                            def.name = tokens[1] + " " + tokens[2];
                            def.command = tokens[1] + " " + tokens[2] + " "
                                    + defaultCommands;
                        } else if ("/.*/".equals(tokens[1])) {

                            if ("result".equals(tokens[0])) {
                                if (def.result == null)
                                    def.result = tokens[2];
                            } else if ("smtp-comment".equals(tokens[0])) {
                                if (def.smtpComment == null)
                                    def.smtpComment = tokens[2];
                            } else if ("received-spf".equals(tokens[0])) {
                                if (def.receivedSPF == null)
                                    def.receivedSPF = tokens[2].replaceFirst(
                                            "Received-SPF: ", "");
                            } else if ("header-comment".equals(tokens[0])) {
                                if (def.headerComment == null)
                                    def.headerComment = tokens[2];
                            } else {
                                System.err.println("Unknown token: "
                                        + tokens[0]);
                            }

                        } else {
                            System.out.println("Ignored line: " + line);
                        }

                    } else {
                        throw new IllegalStateException("Bad format: " + line);
                    }
                }
            }

        }

        if (def != null && def.command != null) {
            if (def.result != null) {
                tests.put(def.command, def);
            } else {
                System.err.println("Unexpected test sequence: " + def.command
                        + "|" + def.result + "|" + def.smtpComment + "|"
                        + def.headerComment + "|" + def.receivedSPF);
            }
        }

        br.close();

        return tests;
    }

    static class SPFSuite extends TestSuite {

        public SPFSuite() throws IOException {
            super();
            HashMap tests = loadTests();
            Iterator i = tests.keySet().iterator();
            while (i.hasNext()) {
                addTest(new SPFTest((SPFTestDef) tests.get(i.next())));
            }
        }

    }

    public static class SPFTestDef {
        public String name = null;

        public String command = null;

        public String result = null;

        public String smtpComment = null;

        public String headerComment = null;

        public String receivedSPF = null;
    }

}