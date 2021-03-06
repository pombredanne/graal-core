/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.hotspot.replacements;

import static com.oracle.graal.compiler.common.LocationIdentity.any;
import static com.oracle.graal.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.JAVA_THREAD_OSTHREAD_LOCATION;
import static com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.JAVA_THREAD_THREAD_OBJECT_LOCATION;
import static com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.osThreadInterruptedOffset;
import static com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.osThreadOffset;
import static com.oracle.graal.hotspot.replacements.HotSpotReplacementsUtil.threadObjectOffset;

import com.oracle.graal.api.replacements.ClassSubstitution;
import com.oracle.graal.api.replacements.MethodSubstitution;
import com.oracle.graal.compiler.common.spi.ForeignCallDescriptor;
import com.oracle.graal.graph.Node.ConstantNodeParameter;
import com.oracle.graal.graph.Node.NodeIntrinsic;
import com.oracle.graal.hotspot.nodes.CurrentJavaThreadNode;
import com.oracle.graal.nodes.extended.ForeignCallNode;
import com.oracle.graal.word.Word;

/**
 * Substitutions for {@link java.lang.Thread} methods.
 */
@ClassSubstitution(Thread.class)
public class ThreadSubstitutions {

    @MethodSubstitution(isStatic = false)
    public static boolean isInterrupted(final Thread thisObject, boolean clearInterrupted) {
        Word javaThread = CurrentJavaThreadNode.get();
        Object thread = javaThread.readObject(threadObjectOffset(INJECTED_VMCONFIG), JAVA_THREAD_THREAD_OBJECT_LOCATION);
        if (thisObject == thread) {
            Word osThread = javaThread.readWord(osThreadOffset(INJECTED_VMCONFIG), JAVA_THREAD_OSTHREAD_LOCATION);
            boolean interrupted = osThread.readInt(osThreadInterruptedOffset(INJECTED_VMCONFIG), any()) != 0;
            if (!interrupted || !clearInterrupted) {
                return interrupted;
            }
        }

        return threadIsInterruptedStub(THREAD_IS_INTERRUPTED, thisObject, clearInterrupted);
    }

    public static final ForeignCallDescriptor THREAD_IS_INTERRUPTED = new ForeignCallDescriptor("thread_is_interrupted", boolean.class, Object.class, boolean.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native boolean threadIsInterruptedStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Thread thread, boolean clearIsInterrupted);
}
