from typing import Dict
from dfa import DFA, DFANode
from character_classes import CharacterClass

# id of a node in a DFA
NodeId = int

# an id such that int(StringNodeIdInstance) returns a NodeId
StringNodeId = str

# the character class representation used by the DFA generation program
DFACharacterClass = str

def convert_dfa_character_class(dfa_character_class: str) -> CharacterClass:
    '''
    Convert the character class from the DFA program into
    ...the sorts of character classes this program uses
    '''
    if "-" in dfa_character_class:
        if "---" in dfa_character_class:
            starting_str = "-"
            ending_str = "-"
        elif "--" in dfa_character_class:
            if dfa_character_class.startswith("--"):
                starting_str = "-"
                ending_str = dfa_character_class[2:]
                # print(starting_str + "***" + ending_str)
            else: 
                starting_str = dfa_character_class[0:-2]
                ending_str = "-"
                # print(starting_str + "***" + ending_str)
        else: 
            starting_str, ending_str = dfa_character_class.split('-')

        # convert both of these to ascii just to be consistent
        if starting_str.startswith("\\u"):
            starting_ascii = int("0x" + starting_str[2:], 16)
        else:
            starting_ascii = ord(starting_str)

        if ending_str.startswith("\\u"):
            ending_ascii = int("0x" + ending_str[2:], 16)
        else:
            ending_ascii = ord(ending_str)

        character_class = CharacterClass('')
        for ascii_char in range(starting_ascii, ending_ascii + 1):
            if int(0x20) <= ascii_char <= int(0x7e):
                # if the character is within the range of characters we're using for this program
                character_class += CharacterClass(chr(ascii_char))
        return character_class
    else:
        return CharacterClass(dfa_character_class)


def file_to_dfa(filename) -> DFA:
    '''
    Given a file, create a DFA.
    The file should be in the following format:
        The first line should contain the id of the starting node in the DFA.
        The last line should contain the ids of the accepting nodes in the DFA.
        The subsequent lines should be in the format:
            NODE_ID of source,NODE_ID of target,CHARACTER_CLASS

        NODE_IDs should be integers.
        At this time, we're not sure what form the CHARACTER_CLASS should be in. (TODO)
        Also, note the lack of space between the commas
    '''
    dfa: DFA = DFA()
    def add_node(node_id) -> DFANode:
        '''Adds a node if it does not exist'''
        try:
            return dfa.nodes[node_id]
        except KeyError:
            dfa.add_node(node_id, DFANode())
            return dfa.nodes[node_id]

    with open(filename, 'r') as f:
        # makes it easier to figure out when we're at the last line
        lines = f.readlines()
        for line_index, line in enumerate(lines):
            if line.endswith('\n'):
                line = line[:-1]
            if line_index == 0:
                start_node_id = int(line)
                dfa.set_node_properties(start_node_id, start=True)
            elif line_index == len(lines) - 1:
                # the last line with the accepting nodes is unneeded
                accepters = line.split(',')
                for accepter in accepters:
                    accepter_node_id = int(accepter)
                    dfa.set_node_properties(accepter_node_id, accept=True)
            elif line:
                source_node_id: StringNodeId
                target_node_id: StringNodeId
                dfa_character_class: DFACharacterClass
                source_node_id, target_node_id, dfa_character_class = line.split(',')
                source_node: DFANode = add_node(int(source_node_id))
                target_node: DFANode = add_node(int(target_node_id))
                character_class = convert_dfa_character_class(dfa_character_class)
                source_node.add_out_edge(character_class, int(target_node_id))

        return dfa
