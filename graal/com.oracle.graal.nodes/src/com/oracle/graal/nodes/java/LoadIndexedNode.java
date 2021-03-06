/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.java;

import com.oracle.graal.compiler.common.type.Stamp;
import com.oracle.graal.compiler.common.type.StampFactory;
import com.oracle.graal.compiler.common.type.TypeReference;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.Canonicalizable;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.ConstantNode;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.spi.Virtualizable;
import com.oracle.graal.nodes.spi.VirtualizerTool;
import com.oracle.graal.nodes.type.StampTool;
import com.oracle.graal.nodes.virtual.VirtualArrayNode;
import com.oracle.graal.nodes.virtual.VirtualObjectNode;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * The {@code LoadIndexedNode} represents a read from an element of an array.
 */
@NodeInfo
public class LoadIndexedNode extends AccessIndexedNode implements Virtualizable, Canonicalizable {

    public static final NodeClass<LoadIndexedNode> TYPE = NodeClass.create(LoadIndexedNode.class);

    /**
     * Creates a new LoadIndexedNode.
     *
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param elementKind the element type
     */
    public LoadIndexedNode(Assumptions assumptions, ValueNode array, ValueNode index, JavaKind elementKind) {
        this(TYPE, createStamp(assumptions, array, elementKind), array, index, elementKind);
    }

    public static ValueNode create(Assumptions assumptions, ValueNode array, ValueNode index, JavaKind elementKind, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection) {
        ValueNode constant = tryConstantFold(array, index, metaAccess, constantReflection);
        if (constant != null) {
            return constant;
        }
        return new LoadIndexedNode(assumptions, array, index, elementKind);
    }

    protected LoadIndexedNode(NodeClass<? extends LoadIndexedNode> c, Stamp stamp, ValueNode array, ValueNode index, JavaKind elementKind) {
        super(c, stamp, array, index, elementKind);
    }

    private static Stamp createStamp(Assumptions assumptions, ValueNode array, JavaKind kind) {
        ResolvedJavaType type = StampTool.typeOrNull(array);
        if (kind == JavaKind.Object && type != null && type.isArray()) {
            return StampFactory.object(TypeReference.createTrusted(assumptions, type.getComponentType()));
        } else {
            return StampFactory.forKind(kind);
        }
    }

    @Override
    public boolean inferStamp() {
        return updateStamp(createStamp(graph().getAssumptions(), array(), elementKind()));
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode alias = tool.getAlias(array());
        if (alias instanceof VirtualObjectNode) {
            VirtualArrayNode virtual = (VirtualArrayNode) alias;
            ValueNode indexValue = tool.getAlias(index());
            int idx = indexValue.isConstant() ? indexValue.asJavaConstant().asInt() : -1;
            if (idx >= 0 && idx < virtual.entryCount()) {
                tool.replaceWith(tool.getEntry(virtual, idx));
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        ValueNode constant = tryConstantFold(array(), index(), tool.getMetaAccess(), tool.getConstantReflection());
        if (constant != null) {
            return constant;
        }
        return this;
    }

    private static ValueNode tryConstantFold(ValueNode array, ValueNode index, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection) {
        if (array.isConstant() && !array.isNullConstant() && index.isConstant()) {
            JavaConstant arrayConstant = array.asJavaConstant();
            if (arrayConstant != null) {
                int stableDimension = ((ConstantNode) array).getStableDimension();
                if (stableDimension > 0) {
                    JavaConstant constant = constantReflection.readArrayElement(arrayConstant, index.asJavaConstant().asInt());
                    boolean isDefaultStable = ((ConstantNode) array).isDefaultStable();
                    if (constant != null && (isDefaultStable || !constant.isDefaultForKind())) {
                        return ConstantNode.forConstant(constant, stableDimension - 1, isDefaultStable, metaAccess);
                    }
                }
            }
        }
        return null;
    }
}
