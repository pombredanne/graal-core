/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.graph.test;

import static com.oracle.graal.graph.test.matchers.NodeIterableContains.contains;
import static com.oracle.graal.graph.test.matchers.NodeIterableIsEmpty.isEmpty;
import static com.oracle.graal.graph.test.matchers.NodeIterableIsEmpty.isNotEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.oracle.graal.graph.Graph;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.nodeinfo.NodeInfo;

public class NodeUsagesTests {

    @NodeInfo
    static final class Def extends Node {
        public static final NodeClass<Def> TYPE = NodeClass.create(Def.class);

        protected Def() {
            super(TYPE);
        }
    }

    @NodeInfo
    static final class Use extends Node {
        public static final NodeClass<Use> TYPE = NodeClass.create(Use.class);
        @Input Def in0;
        @Input Def in1;
        @Input Def in2;

        protected Use(Def in0, Def in1, Def in2) {
            super(TYPE);
            this.in0 = in0;
            this.in1 = in1;
            this.in2 = in2;
        }

    }

    @Test
    public void testReplaceAtUsages() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtUsages(def1);

        assertThat(def0.usages(), isEmpty());

        assertEquals(3, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));
        assertThat(def1.usages(), contains(use1));
        assertThat(def1.usages(), contains(use2));

        assertThat(def1.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicateAll() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> true);

        assertThat(def0.usages(), isEmpty());

        assertEquals(3, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));
        assertThat(def1.usages(), contains(use1));
        assertThat(def1.usages(), contains(use2));

        assertThat(def1.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicateNone() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> false);

        assertThat(def1.usages(), isEmpty());

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate1() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u == use1);

        assertEquals(1, def1.getUsageCount());
        assertThat(def1.usages(), contains(use1));

        assertThat(def1.usages(), isNotEmpty());

        assertEquals(2, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate2() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u == use2);

        assertEquals(1, def1.getUsageCount());
        assertThat(def1.usages(), contains(use2));

        assertThat(def1.usages(), isNotEmpty());

        assertEquals(2, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));

        assertThat(def0.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate0() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u == use0);

        assertEquals(1, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));

        assertThat(def1.usages(), isNotEmpty());

        assertEquals(2, def0.getUsageCount());
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate02() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u != use1);

        assertEquals(1, def0.getUsageCount());
        assertThat(def0.usages(), contains(use1));

        assertThat(def0.usages(), isNotEmpty());

        assertEquals(2, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));
        assertThat(def1.usages(), contains(use2));

        assertThat(def1.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate023() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));
        Use use3 = graph.add(new Use(null, null, def0));

        assertEquals(4, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));
        assertThat(def0.usages(), contains(use3));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u != use1);

        assertEquals(1, def0.getUsageCount());
        assertThat(def0.usages(), contains(use1));

        assertThat(def0.usages(), isNotEmpty());

        assertEquals(3, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));
        assertThat(def1.usages(), contains(use2));
        assertThat(def1.usages(), contains(use3));

        assertThat(def1.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate013() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));
        Use use3 = graph.add(new Use(null, null, def0));

        assertEquals(4, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));
        assertThat(def0.usages(), contains(use3));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u != use2);

        assertEquals(1, def0.getUsageCount());
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());

        assertEquals(3, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));
        assertThat(def1.usages(), contains(use1));
        assertThat(def1.usages(), contains(use3));

        assertThat(def1.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate203() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));
        Use use3 = graph.add(new Use(null, null, def0));

        assertEquals(4, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));
        assertThat(def0.usages(), contains(use3));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u == use2);

        assertEquals(1, def1.getUsageCount());
        assertThat(def1.usages(), contains(use2));

        assertThat(def1.usages(), isNotEmpty());

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use3));

        assertThat(def0.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate01() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u != use2);

        assertEquals(1, def0.getUsageCount());
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());

        assertEquals(2, def1.getUsageCount());
        assertThat(def1.usages(), contains(use0));
        assertThat(def1.usages(), contains(use1));

        assertThat(def1.usages(), isNotEmpty());
    }

    @Test
    public void testReplaceAtUsagesWithPredicate12() {
        Graph graph = new Graph();
        Def def0 = graph.add(new Def());
        Def def1 = graph.add(new Def());
        Use use0 = graph.add(new Use(def0, null, null));
        Use use1 = graph.add(new Use(null, def0, null));
        Use use2 = graph.add(new Use(null, null, def0));

        assertEquals(3, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));
        assertThat(def0.usages(), contains(use1));
        assertThat(def0.usages(), contains(use2));

        assertThat(def0.usages(), isNotEmpty());
        assertThat(def1.usages(), isEmpty());

        def0.replaceAtMatchingUsages(def1, u -> u != use0);

        assertEquals(1, def0.getUsageCount());
        assertThat(def0.usages(), contains(use0));

        assertThat(def0.usages(), isNotEmpty());

        assertEquals(2, def1.getUsageCount());
        assertThat(def1.usages(), contains(use1));
        assertThat(def1.usages(), contains(use2));

        assertThat(def1.usages(), isNotEmpty());
    }
}
