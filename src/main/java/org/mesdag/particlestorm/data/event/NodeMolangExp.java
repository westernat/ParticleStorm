package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.MolangExp;

public class NodeMolangExp extends MolangExp implements IEventNode {
    public static final Codec<NodeMolangExp> CODEC = Codec.STRING.xmap(NodeMolangExp::new, e -> e.expStr);

    public NodeMolangExp(String expStr) {
        super(expStr);
    }
}
