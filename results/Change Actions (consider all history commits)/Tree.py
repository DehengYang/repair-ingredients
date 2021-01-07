# -*- coding: utf-8 -*-
"""
@author: Administrator
"""

class Queue(object):
    def __init__(self, queue=None):
        if queue is None:
            self.queue = []
        else:
            self.queue = list(queue)
    def dequeue(self):
        return self.queue.pop(0)
    def enqueue(self, element):
        self.queue.append(element)
    def is_empty(self):
        return True if len(self.queue) == 0 else False

class Tree():
    class Position():
        def element(self):
            raise NotImplementedError('must be implemented by subclass')

        def __eq__(self, other):
            raise NotImplementedError('must be implemented by subclass')

    def root(self):
        raise NotImplementedError('must be implemented by subclass')

    def parent(self,p):
        raise NotImplementedError('must be implemented by subclass')

    def num_children(self,p):
        raise NotImplementedError('must be implemented by subclass')

    def children(self,p):
        raise NotImplementedError('must be implemented by subclass')

    def __len__(self):
        raise NotImplementedError('must be implemented by subclass')

    def is_root(self,p):
        return self.root() == p

    def is_leaf(self,p):
        return self.num_children(p) == 0

    def is_empty(self):
        return len(self) == 0
    
class BinaryTree(Tree):

    class Node():

        def __init__(self, element, parent=None, left=None, right=None):
            self.element = element
            self.parent = parent
            self.left = left
            self.right = right

    class Position(Tree.Position):

        def __init__(self, container, node):
            self.container = container
            self.node = node

        def element(self):
            return self.node.element

        def __eq__(self, other):
            return isinstance(other, type(self)) and other.node is self.node

    def validate(self, p):
        if not isinstance(p, self.Position):
            raise TypeError('p must be proper Position type')
        if p.container is not self:
            raise ValueError('p does not belong to this container')
        if p.node.parent is p.node:
            raise ValueError('p is no longer valid')
        return p.node

    def make_position(self, node):
        return self.Position(self, node) if node is not None else None

    def __init__(self):
        self._root = None
        self.size = 0

    def __len__(self):
        return self.size

    def root(self):
        return self.make_position(self._root)

    def parent(self, p):
        node = self.validate(p)
        return self.make_position(node.parent)

    def left(self, p):
        node = self.validate(p)
        return self.make_position(node.left)

    def right(self, p):
        node = self.validate(p)
        return self.make_position(node.right)

    def sibling(self, p):
        parent = self.parent(p)
        if parent is None:
            return None
        else:
            if p == self.left(parent):
                return self.right(parent)
            else:
                return self.left(parent)

    def num_children(self, p):
        node = self.validate(p)
        count = 0
        if node.left is not None:
            count += 1
        if node.right is not None:
            count += 1
        return count

    def children(self,p):
        if self.left(p) is not None:
            yield self.left(p)
        if self.right(p) is not None:
            yield self.right(p)
            
    def add_root(self, e):
        if self._root is not None:
            raise ValueError('Root exists')
        self.size += 1
        self._root = self.Node(e)
        return self.make_position(self._root)

    def add_left(self, e, p):
        node = self.validate(p)
        if node.left is not None:
            raise ValueError('Left child exists')
        self.size += 1
        node.left = self.Node(e, node)
        return self.make_position(node.left)
    
    def add_right(self, e, p):
        node = self.validate(p)
        if node.right is not None:
            raise ValueError('Left child exists')
        self.size += 1
        node.right = self.Node(e, node)
        return self.make_position(node.right)
    
    def replace(self, p, e):
        node = self.validate(p)
        old = node.element
        node.element = e
        return old
    
    def delete(self, p):
        node = self.validate(p)
        if self.num_children(p) == 2:
            raise ValueError('p has two children')
        child = node.left if node.left else node.right
        if child is not None:
            child.parent = node.parent
        if node is self._root:
            self._root = child
        else:
            parent = node.parent
            if node is parent.left:
                parent.left = child
            else:
                parent.right = child
        self.size -= 1
        node.parent = node
        return node.element
    
    def breadthfirst(self):
        if not self.is_empty():
            queue = Queue()
            queue.enqueue(self.root())
            while not queue.is_empty():
                p = queue.dequeue()
                yield p
                for i in self.children(p):
                    queue.enqueue(i)
            
class CCITree(Tree):

    class Node():

        def __init__(self, element, parent=None, children = [], space_cnt = -1, 
                     op_index = -1, parent_index = -1, children_index_list = []):
            self.element = element
            self.parent = parent
            self.children = children
            
            self.space_cnt = space_cnt
            self.op_index = op_index
            self.parent_index = parent_index
            self.children_index_list = children_index_list
            
        def get_children_str(self):
            children_str = ""
            for child in self.children:
                children_str += str(child.op_index + 1) + " "
            return children_str.strip()
        
        def print(self):
            print("Now in node.print()")
            if self.parent is not None:
                print("Node element: {}, node parent: {}, children size: {}, node parent children size:".format
                      (self.element, self.parent.element, len(self.children)), len(self.parent.children))
            for child in self.children:
                print("child: {}".format(child.element))
        def __eq__(self, other):
            return self.element == other.element
            
    class Position(Tree.Position):

        def __init__(self, container, node):
            self.container = container
            self.node = node

        def element(self):
            return self.node.element

        def __eq__(self, other):
            return isinstance(other, type(self)) and other.node is self.node
        
        def print(self):
            print("Position element: {}".format(self.element()))
            self.node.print()

    def validate(self, p):
        if not isinstance(p, self.Position):
            raise TypeError('p must be proper Position type')
        if p.container is not self:
            raise ValueError('p does not belong to this container')
        if p.node.parent is p.node:
            print(p.node.element)
            raise ValueError('p is no longer valid')
        return p.node

    def make_position(self, node):
        return self.Position(self, node) if node is not None else None

    def __init__(self):
        self._root = None
        self.size = 0

    def __len__(self):
        return self.size

    def root(self):
        return self.make_position(self._root)

    def parent(self, p):
        node = self.validate(p)
        return self.make_position(node.parent)

    def children(self, p):
        node = self.validate(p)
        if len(node.children) > 0:
#            print("current node: {}, current node children len: {}".format(node.element, len(node.children)))
            for child in node.children:
                yield self.make_position(child)

    def num_children(self, p):
        node = self.validate(p)
        return len(node.children)
            
    def add_root(self, e):
        if self._root is not None:
            raise ValueError('Root exists')
        self.size += 1
        self._root = self.Node(e)
        return self.make_position(self._root)
        
    def add_child(self, e, p):
        node = self.validate(p)
        self.size += 1
        node.children.append(self.Node(e, node, []))  
        return self.make_position(node.children[-1])
    
    def add_child_node(self, child_node, parent_node):
        self.size += 1
        parent_node.children.append(child_node)  
        return self.make_position(parent_node.children[-1])
    
    def replace(self, p, e):
        node = self.validate(p)
        old = node.element
        node.element = e
        return old
        
    def breadthfirst(self):
        if not self.is_empty():
            queue = Queue()
            queue.enqueue(self.root())
            while not queue.is_empty():
                p = queue.dequeue()
                yield p
                for i in self.children(p):
                    queue.enqueue(i)
                    
    def print_all(self):
        for position in self.breadthfirst():
            node = position.node
            print("node name: {}, node depth: {}, node children: {}".format(
                    node.element, self.depth(position), node.get_children_str()))
            
    def depth(self,p):
        if self.is_root(p):
            return 0
        else:
            return 1 + self.depth(self.parent(p))
    
    def height(self,p):
        if self.is_leaf(p):
            return 0
        else:
            return 1 + max(self.height(c) for c in self.children(p))
        
def test_binary_tree():
    tree = BinaryTree()
    element = "OP0"
    el1 = "OP1"
    el2 = "OP2"
    node = tree.Node(element)
    print("node element: {}".format(node.element))
    
    tree.add_root(element)
    op1 = tree.add_left(el1, tree.root())
    op2 = tree.add_right(el2, tree.root())
    
    tree.add_left("OP1-1", op1)
    tree.add_right("OP1-2", op1)
    tree.add_left("OP2-1", op2)
    
    for node in tree.breadthfirst():
        print(node.element())

def test_CCI_tree():
    tree = CCITree()
    element = "OP0"
    el1 = "OP1"
    el2 = "OP2"
    node = tree.Node(element)
    
    op_list = []
    
    op0 = tree.add_root(element)
    op_list.append(op0)
    
    op1 = tree.add_child(el1, tree.root())
    op_list.append(op1)
    
    op2 = tree.add_child(el2, tree.root())
    op_list.append(op2)
    
    op3 = tree.add_child("OP1-1", op1)
    op_list.append(op3)
#    print_list(op_list)
    
    op4 = tree.add_child("OP1-2", op1)
    op_list.append(op4)
#    print_list(op_list)
    
    op5 = tree.add_child("OP2-1", op2)
    op_list.append(op5)
#    print_list(op_list)
    
    tree.print_all()
    
def print_list(op_list):
    for op in op_list:
        op.print()
         
if __name__ == "__main__":
#    test_binary_tree()
    test_CCI_tree()
    

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    