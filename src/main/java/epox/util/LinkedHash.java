// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 24.10.05
 *
 * @version 	01 (1.14)
 * @author 		epoximator
 */

package epox.util;

/**
 * Combined linked list and hash map. Provides both fast O(1) lookup via hash map
 * and maintains insertion order via a doubly-linked list.
 *
 * @author JV
 * @version 1
 */
@SuppressWarnings("rawtypes")
public class LinkedHash extends java.util.HashMap /* !<Object,Object> */ {
    private static final long serialVersionUID = 1L;

    private Node head = null;
    private Node tail = null;

    public LinkedHash() {
        super();
        head = tail = new Node(null, null);
    }

    /**
     * Adds an element to the end of the list if not already present.
     *
     * @param element
     *            the element to add
     * @return true if the element was added, false if it already existed
     */
    @SuppressWarnings("unchecked")
    public boolean addLast(Object element) {
        Node newNode = new Node(element, tail);
        if (super.put(element, newNode) == null) {
            tail = tail.next = newNode;
            return true;
        }
        return false;
    }

    /**
     * Removes an element from both the hash map and the linked list.
     *
     * @param element
     *            the element to remove
     * @return null (the element is removed but not returned)
     */
    @Override
    public Object remove(Object element) {
        Node removedNode = (Node) super.remove(element);
        if (removedNode == null) {
            System.out.println("! LinkedHash: Tried to remove non existing entry: " + element);
        }
        removedNode.prev.next = removedNode.next;
        if (removedNode.next != null) {
            removedNode.next.prev = removedNode.prev;
        } else {
            tail = removedNode.prev;
        }
        return null;
    }

    /**
     * Clears both the hash map and the linked list.
     */
    @Override
    public void clear() {
        super.clear();
        head.next = head.prev = null;
        tail = head;
    }

    /**
     * Returns the first element in the list without removing it.
     *
     * @return the first element, or null if the list is empty
     */
    public Object getFirst() {
        if (head.next == null) {
            return null;
        }
        return head.next.data;
    }

    /**
     * Internal doubly-linked list node.
     */
    private class Node {
        Node prev;
        Node next;
        Object data;

        Node(Object data, Node prev) {
            this.data = data;
            this.prev = prev;
        }
    }
}
