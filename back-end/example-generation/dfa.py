from typing import Set, Tuple, Optional, Dict
from character_classes import CharacterClass

# id number for a node
NodeId = int

class DFANode:
    '''Nodes in a DFA'''
    def __init__(self, out_edges=None):
        if out_edges is None:
            out_edges = set()

        self.out_edges: Set[Tuple[CharacterClass, NodeId]] = out_edges

    def __str__(self):
        return f"<Node with out edges: {list((str(edge), destination) for edge, destination in self.out_edges)}>"

    def add_out_edge(self, edge: CharacterClass, destination: NodeId) -> None:
        '''An an edge going out from this node to some other node (may be itself)'''
        assert isinstance(destination, NodeId), "Destinations used to be nodes: they should now be IDs"
        self.out_edges.add((edge, destination))

class DFA:
    '''DFA class that can be used to simulate a DFA'''
    def __init__(self):
        self.nodes: Dict[NodeId, DFANode] = {}
        self.start_node: Optional[NodeId] = None
        self.accept_nodes: Set[NodeId] = set()

    def __str__(self):
        nodes = ""
        for node_id in self.nodes:
            nodes += f"<Node {node_id}: {self.nodes[node_id]}\n"
        return f"<DFA with starting node {self.start_node} and accepting nodes {self.accept_nodes} and nodes as follows:\n{nodes}END DFA>"

    def add_node(self, node_id: NodeId, node: DFANode):
        '''Add a node to the DFA'''
        assert isinstance(node_id, NodeId)
        self.nodes[node_id] = node

    def set_node_properties(self, node_id: NodeId, start: Optional[bool] = None, accept: Optional[bool] = None):
        '''Set whether the node is the start node or an accept node'''
        if start:
            self.start_node = node_id
        elif start == False: # pylint: disable=singleton-comparison
            if self.start_node == node_id:
                self.start_node = None

        if accept:
            self.accept_nodes.add(node_id)
        elif accept == False: # pylint: disable=singleton-comparison
            try:
                self.accept_nodes.remove(node_id)
            except KeyError:
                # it wasn't in there in the first place
                pass

    def simulate(self, string: str) -> bool:
        '''Simulate the DFA on a string and return whether or not it accepts the string'''
        node_id = self.start_node
        for character in string:
            node = self.nodes[node_id]
            target_node_id = None
            for edge in node.out_edges:
                character_class, possible_target_node_id = edge
                if character in character_class:
                    target_node_id = possible_target_node_id
                    break
            if target_node_id is None:
                # there was no place to go
                return False
            else:
                node_id = target_node_id

        return node_id in self.accept_nodes
