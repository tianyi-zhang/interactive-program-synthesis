#!/usr/bin/env python
# pylint: disable=invalid-name

import argparse
from file_to_dfa import file_to_dfa
from generate_examples import generate_examples

parser = argparse.ArgumentParser()
parser.add_argument('string', help="the string to be modified such that it breaks the dfa", type=str)
parser.add_argument('input_file', help="file containing the dfa", type=str)
parser.add_argument('-a', '--amount', help="Amount of examples to generate per character in the string",
                    type=int, default=20)
args = parser.parse_args()

dfa = file_to_dfa(args.input_file)
string = args.string
amount = args.amount

print(generate_examples(string, dfa, amount))
