package com.mujah.minidb;

import java.util.ArrayList;
import java.util.List;

class BPlusTreeNode {

    boolean leaf;
    List<String> keys;
    List<Long> values;
    List<BPlusTreeNode> children;
    BPlusTreeNode next;

    BPlusTreeNode(boolean leaf) {
        this.leaf = leaf;
        this.keys = new ArrayList<>();

        if (leaf) {
            this.values = new ArrayList<>();
        } else {
            this.children = new ArrayList<>();
        }
    }
}