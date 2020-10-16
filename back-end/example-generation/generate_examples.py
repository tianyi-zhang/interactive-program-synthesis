import random
import json
from typing import Set, List, Dict
from dfa import DFA
from character_classes import ALL as ALL_CHARACTERS, CharacterClass

def generate_examples(user_string: str, dfa: DFA, how_many: int):
    '''
    Takes a string, a dfa, and how many examples it should generate for each character in the string.
    Returns a json object in the form {characterIndex: [listOfModifiedStrings]}
    '''
    answer = {}
    for character_index in range(len(user_string)):
        modified_strings = get_modified_strings(user_string, character_index, dfa, how_many)
        answer[character_index] = modified_strings
    json_answer = json.dumps(answer)
    return json_answer

def get_modified_strings(user_string: str, character_index: int, dfa: DFA, how_many: int) -> List[str]:
    '''
    Gets `how_many` modified versions of the `user_string` that pass  break the `dfa`
    ...at the given `character_index` in the `user_string`'''
    POSITIVE = "positive"
    NEGATIVE = "negative"
    # return value
    answer: Dict[str, List[str]] = {POSITIVE: [], NEGATIVE: []}
    before_user_string = user_string[:character_index]
    user_string_character = user_string[character_index]
    after_user_string = user_string[character_index + 1:]

    for example_type in [POSITIVE, NEGATIVE]:
        # possibilities that will be added to the return value
        options: List[str] = []
        example_type_answer: Set[str] = answer[example_type]
        for test_character in (ALL_CHARACTERS - CharacterClass(user_string_character)):
            test_string = before_user_string + test_character + after_user_string
            accepted: bool = dfa.simulate(test_string)
            if (example_type == POSITIVE and accepted) or (example_type == NEGATIVE and not accepted):
                options.append(test_string)

        for example_index in range(how_many): # pylint: disable=unused-variable
            if options:
                chosen_index = random.randint(0, len(options) - 1)
                example_type_answer.append(options.pop(chosen_index))
    return answer