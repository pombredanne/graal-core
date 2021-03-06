/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.printer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.graal.debug.Debug;
import com.oracle.graal.debug.DebugConfig;
import com.oracle.graal.debug.DebugConfigCustomizer;
import com.oracle.graal.debug.DebugDumpHandler;
import com.oracle.graal.debug.TTY;
import com.oracle.graal.debug.GraalDebugConfig.Options;
import com.oracle.graal.graph.Node;
import com.oracle.graal.nodeinfo.Verbosity;
import com.oracle.graal.nodes.util.GraphUtil;
import com.oracle.graal.serviceprovider.ServiceProvider;

@ServiceProvider(DebugConfigCustomizer.class)
public class GraalDebugConfigCustomizer implements DebugConfigCustomizer {
    private static long dumpIgvTimestamp;
    private static final AtomicInteger dumpIgvId = new AtomicInteger();

    @Override
    public void customize(DebugConfig config) {
        if (Options.PrintIdealGraphFile.getValue()) {
            config.dumpHandlers().add(new GraphPrinterDumpHandler(GraalDebugConfigCustomizer::createFilePrinter));
        } else {
            config.dumpHandlers().add(new GraphPrinterDumpHandler(GraalDebugConfigCustomizer::createNetworkPrinter));
        }
        if (Options.PrintCanonicalGraphStrings.getValue()) {
            config.dumpHandlers().add(new GraphPrinterDumpHandler(GraalDebugConfigCustomizer::createStringPrinter));
        }
        config.dumpHandlers().add(new NodeDumper());
        if (Options.PrintCFG.getValue() || Options.PrintBackendCFG.getValue()) {
            if (Options.PrintBinaryGraphs.getValue() && Options.PrintCFG.getValue()) {
                TTY.out.println("Complete C1Visualizer dumping slows down PrintBinaryGraphs: use -Dgraal.PrintCFG=false to disable it");
            }
            config.dumpHandlers().add(new CFGPrinterObserver(Options.PrintCFG.getValue()));
        }
        config.verifyHandlers().add(new NoDeadCodeVerifyHandler());
    }

    private static class NodeDumper implements DebugDumpHandler {
        @Override
        public void dump(Object object, String message) {
            if (object instanceof Node) {
                String location = GraphUtil.approxSourceLocation((Node) object);
                String node = ((Node) object).toString(Verbosity.Debugger);
                if (location != null) {
                    Debug.log("Context obj %s (approx. location: %s)", node, location);
                } else {
                    Debug.log("Context obj %s", node);
                }
            }
        }

        @Override
        public void close() {
        }
    }

    private static CanonicalStringGraphPrinter createStringPrinter() {
        // If this is the first time I have constructed a FilePrinterPath,
        // get a time stamp in a (weak) attempt to make unique file names.
        if (dumpIgvTimestamp == 0) {
            dumpIgvTimestamp = System.currentTimeMillis();
        }
        // Construct the path to the directory.
        Path path = Paths.get(Options.DumpPath.getValue(), "graph-strings-" + dumpIgvTimestamp + "_" + dumpIgvId.incrementAndGet());
        return new CanonicalStringGraphPrinter(path);
    }

    private static GraphPrinter createNetworkPrinter() throws IOException {
        String host = Options.PrintIdealGraphAddress.getValue();
        int port = Options.PrintBinaryGraphs.getValue() ? Options.PrintBinaryGraphPort.getValue() : Options.PrintIdealGraphPort.getValue();
        try {
            GraphPrinter printer;
            if (Options.PrintBinaryGraphs.getValue()) {
                printer = new BinaryGraphPrinter(SocketChannel.open(new InetSocketAddress(host, port)));
            } else {
                printer = new IdealGraphPrinter(new Socket(host, port).getOutputStream(), true);
            }
            TTY.println("Connected to the IGV on %s:%d", host, port);
            return printer;
        } catch (ClosedByInterruptException | InterruptedIOException e) {
            /*
             * Interrupts should not count as errors because they may be caused by a cancelled Graal
             * compilation. ClosedByInterruptException occurs if the SocketChannel could not be
             * opened. InterruptedIOException occurs if new Socket(..) was interrupted.
             */
            return null;
        } catch (IOException e) {
            throw new IOException(String.format("Could not connect to the IGV on %s:%d", host, port), e);
        }
    }

    private static Path getFilePrinterPath() {
        // If this is the first time I have constructed a FilePrinterPath,
        // get a time stamp in a (weak) attempt to make unique file names.
        if (dumpIgvTimestamp == 0) {
            dumpIgvTimestamp = System.currentTimeMillis();
        }
        // Encode the kind of the file in the extension.
        final String ext = (Options.PrintBinaryGraphs.getValue() ? ".bgv" : ".gv.xml");
        // Construct the path to the file.
        return Paths.get(Options.DumpPath.getValue(), "runtime-graphs-" + dumpIgvTimestamp + "_" + dumpIgvId.incrementAndGet() + ext);
    }

    private static GraphPrinter createFilePrinter() throws IOException {
        Path path = getFilePrinterPath();
        try {
            GraphPrinter printer;
            if (Options.PrintBinaryGraphs.getValue()) {
                printer = new BinaryGraphPrinter(FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
            } else {
                printer = new IdealGraphPrinter(Files.newOutputStream(path), true);
            }
            TTY.println("Dumping IGV graphs to %s", path.toString());
            return printer;
        } catch (IOException e) {
            throw new IOException(String.format("Failed to open %s to dump IGV graphs", path), e);
        }
    }
}
